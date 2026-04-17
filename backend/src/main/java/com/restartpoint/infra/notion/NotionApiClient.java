package com.restartpoint.infra.notion;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.restartpoint.domain.project.entity.Checkpoint;
import com.restartpoint.domain.project.entity.Project;
import com.restartpoint.global.exception.BusinessException;
import com.restartpoint.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * Notion API 클라이언트
 * - OAuth 토큰 교환
 * - 페이지/데이터베이스 생성
 * - 프로젝트 정보 동기화
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotionApiClient {

    private static final String NOTION_API_BASE = "https://api.notion.com/v1";
    private static final String NOTION_VERSION = "2022-06-28";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${notion.client-id:}")
    private String clientId;

    @Value("${notion.client-secret:}")
    private String clientSecret;

    @Value("${notion.redirect-uri:}")
    private String redirectUri;

    /**
     * OAuth 코드로 access token 교환
     */
    public NotionOAuthResponse exchangeCodeForToken(String code, String redirectUriOverride) {
        String url = NOTION_API_BASE + "/oauth/token";

        // Basic Auth 헤더 생성
        String credentials = clientId + ":" + clientSecret;
        String encodedCredentials = Base64.getEncoder().encodeToString(credentials.getBytes());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Basic " + encodedCredentials);

        // Request body
        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("grant_type", "authorization_code");
        requestBody.put("code", code);
        requestBody.put("redirect_uri", redirectUriOverride != null ? redirectUriOverride : redirectUri);

        try {
            HttpEntity<String> request = new HttpEntity<>(requestBody.toString(), headers);
            ResponseEntity<JsonNode> response = restTemplate.exchange(
                    url, HttpMethod.POST, request, JsonNode.class);

            JsonNode body = response.getBody();
            if (body == null) {
                throw new BusinessException(ErrorCode.NOTION_API_ERROR);
            }

            return NotionOAuthResponse.builder()
                    .accessToken(body.get("access_token").asText())
                    .workspaceId(body.get("workspace_id").asText())
                    .workspaceName(body.has("workspace_name") ? body.get("workspace_name").asText() : "Workspace")
                    .botId(body.get("bot_id").asText())
                    .build();

        } catch (RestClientException e) {
            log.error("Notion OAuth token exchange failed: {}", e.getMessage());
            throw new BusinessException(ErrorCode.NOTION_API_ERROR);
        }
    }

    /**
     * 프로젝트용 Notion 페이지 생성
     */
    public NotionPageResponse createProjectPage(String accessToken, Project project) {
        String url = NOTION_API_BASE + "/pages";

        HttpHeaders headers = createAuthHeaders(accessToken);

        // 페이지 생성 요청 (standalone page - no parent specified means it goes to workspace root)
        ObjectNode requestBody = objectMapper.createObjectNode();

        // Parent - workspace를 parent로 지정 (사용자의 workspace에 페이지 생성)
        ObjectNode parent = objectMapper.createObjectNode();
        parent.put("type", "workspace");
        parent.put("workspace", true);
        requestBody.set("parent", parent);

        // Properties (title)
        ObjectNode properties = objectMapper.createObjectNode();
        ObjectNode title = objectMapper.createObjectNode();
        ArrayNode titleArray = objectMapper.createArrayNode();
        ObjectNode titleText = objectMapper.createObjectNode();
        ObjectNode text = objectMapper.createObjectNode();
        text.put("content", "[Re:Start Point] " + project.getName());
        titleText.set("text", text);
        titleText.put("type", "text");
        titleArray.add(titleText);
        title.set("title", titleArray);
        properties.set("title", title);
        requestBody.set("properties", properties);

        // Children (page content)
        ArrayNode children = objectMapper.createArrayNode();

        // 프로젝트 설명 블록
        children.add(createHeading2Block("프로젝트 정보"));
        children.add(createParagraphBlock("팀: " + project.getTeam().getName()));
        if (project.getProblemDefinition() != null) {
            children.add(createParagraphBlock("문제 정의: " + project.getProblemDefinition()));
        }
        if (project.getSolution() != null) {
            children.add(createParagraphBlock("솔루션: " + project.getSolution()));
        }

        // 체크포인트 섹션
        children.add(createDividerBlock());
        children.add(createHeading2Block("체크포인트"));
        children.add(createParagraphBlock("프로젝트 진행 상황은 아래 데이터베이스에서 관리됩니다."));

        requestBody.set("children", children);

        try {
            HttpEntity<String> request = new HttpEntity<>(requestBody.toString(), headers);
            ResponseEntity<JsonNode> response = restTemplate.exchange(
                    url, HttpMethod.POST, request, JsonNode.class);

            JsonNode body = response.getBody();
            if (body == null) {
                throw new BusinessException(ErrorCode.NOTION_API_ERROR);
            }

            return NotionPageResponse.builder()
                    .pageId(body.get("id").asText())
                    .pageUrl(body.get("url").asText())
                    .build();

        } catch (RestClientException e) {
            log.error("Notion page creation failed: {}", e.getMessage());
            throw new BusinessException(ErrorCode.NOTION_API_ERROR);
        }
    }

    /**
     * 체크포인트 데이터베이스 생성
     */
    public String createCheckpointDatabase(String accessToken, String parentPageId, Project project) {
        String url = NOTION_API_BASE + "/databases";

        HttpHeaders headers = createAuthHeaders(accessToken);

        ObjectNode requestBody = objectMapper.createObjectNode();

        // Parent
        ObjectNode parent = objectMapper.createObjectNode();
        parent.put("type", "page_id");
        parent.put("page_id", parentPageId);
        requestBody.set("parent", parent);

        // Title
        ArrayNode titleArray = objectMapper.createArrayNode();
        ObjectNode titleText = objectMapper.createObjectNode();
        ObjectNode text = objectMapper.createObjectNode();
        text.put("content", "체크포인트");
        titleText.set("text", text);
        titleText.put("type", "text");
        titleArray.add(titleText);
        requestBody.set("title", titleArray);

        // Properties (database columns)
        ObjectNode properties = objectMapper.createObjectNode();

        // 주차 (Number)
        ObjectNode weekProperty = objectMapper.createObjectNode();
        weekProperty.set("number", objectMapper.createObjectNode());
        properties.set("주차", weekProperty);

        // 제목 (Title - required)
        ObjectNode titleProperty = objectMapper.createObjectNode();
        titleProperty.set("title", objectMapper.createObjectNode());
        properties.set("제목", titleProperty);

        // 상태 (Select)
        ObjectNode statusProperty = objectMapper.createObjectNode();
        ObjectNode selectOptions = objectMapper.createObjectNode();
        ArrayNode options = objectMapper.createArrayNode();
        options.add(createSelectOption("진행 전", "gray"));
        options.add(createSelectOption("진행 중", "blue"));
        options.add(createSelectOption("완료", "green"));
        selectOptions.set("options", options);
        statusProperty.set("select", selectOptions);
        properties.set("상태", statusProperty);

        // 이번 주 목표 (Rich Text)
        ObjectNode goalProperty = objectMapper.createObjectNode();
        goalProperty.set("rich_text", objectMapper.createObjectNode());
        properties.set("이번 주 목표", goalProperty);

        // 진행 상황 (Rich Text)
        ObjectNode progressProperty = objectMapper.createObjectNode();
        progressProperty.set("rich_text", objectMapper.createObjectNode());
        properties.set("진행 상황", progressProperty);

        // 막힘 요소 (Rich Text)
        ObjectNode blockersProperty = objectMapper.createObjectNode();
        blockersProperty.set("rich_text", objectMapper.createObjectNode());
        properties.set("막힘 요소", blockersProperty);

        requestBody.set("properties", properties);

        try {
            HttpEntity<String> request = new HttpEntity<>(requestBody.toString(), headers);
            ResponseEntity<JsonNode> response = restTemplate.exchange(
                    url, HttpMethod.POST, request, JsonNode.class);

            JsonNode body = response.getBody();
            if (body == null) {
                throw new BusinessException(ErrorCode.NOTION_API_ERROR);
            }

            return body.get("id").asText();

        } catch (RestClientException e) {
            log.error("Notion database creation failed: {}", e.getMessage());
            throw new BusinessException(ErrorCode.NOTION_API_ERROR);
        }
    }

    /**
     * 체크포인트 데이터 동기화 (데이터베이스에 항목 추가/업데이트)
     */
    public void syncCheckpoints(String accessToken, String databaseId, List<Checkpoint> checkpoints) {
        for (Checkpoint checkpoint : checkpoints) {
            createOrUpdateCheckpointPage(accessToken, databaseId, checkpoint);
        }
    }

    private void createOrUpdateCheckpointPage(String accessToken, String databaseId, Checkpoint checkpoint) {
        String url = NOTION_API_BASE + "/pages";

        HttpHeaders headers = createAuthHeaders(accessToken);

        ObjectNode requestBody = objectMapper.createObjectNode();

        // Parent (database)
        ObjectNode parent = objectMapper.createObjectNode();
        parent.put("type", "database_id");
        parent.put("database_id", databaseId);
        requestBody.set("parent", parent);

        // Properties
        ObjectNode properties = objectMapper.createObjectNode();

        // 제목
        ObjectNode title = objectMapper.createObjectNode();
        ArrayNode titleArray = objectMapper.createArrayNode();
        ObjectNode titleText = objectMapper.createObjectNode();
        ObjectNode text = objectMapper.createObjectNode();
        text.put("content", "Week " + checkpoint.getWeekNumber());
        titleText.set("text", text);
        titleText.put("type", "text");
        titleArray.add(titleText);
        title.set("title", titleArray);
        properties.set("제목", title);

        // 주차
        ObjectNode week = objectMapper.createObjectNode();
        week.put("number", checkpoint.getWeekNumber());
        properties.set("주차", week);

        // 상태
        ObjectNode status = objectMapper.createObjectNode();
        ObjectNode select = objectMapper.createObjectNode();
        select.put("name", mapCheckpointStatus(checkpoint));
        status.set("select", select);
        properties.set("상태", status);

        // 이번 주 목표
        if (checkpoint.getWeeklyGoal() != null) {
            ObjectNode goals = objectMapper.createObjectNode();
            ArrayNode goalsArray = objectMapper.createArrayNode();
            ObjectNode goalsText = objectMapper.createObjectNode();
            ObjectNode goalsContent = objectMapper.createObjectNode();
            goalsContent.put("content", checkpoint.getWeeklyGoal());
            goalsText.set("text", goalsContent);
            goalsText.put("type", "text");
            goalsArray.add(goalsText);
            goals.set("rich_text", goalsArray);
            properties.set("이번 주 목표", goals);
        }

        // 진행 상황
        if (checkpoint.getProgressSummary() != null) {
            ObjectNode progress = objectMapper.createObjectNode();
            ArrayNode progressArray = objectMapper.createArrayNode();
            ObjectNode progressText = objectMapper.createObjectNode();
            ObjectNode progressContent = objectMapper.createObjectNode();
            progressContent.put("content", checkpoint.getProgressSummary());
            progressText.set("text", progressContent);
            progressText.put("type", "text");
            progressArray.add(progressText);
            progress.set("rich_text", progressArray);
            properties.set("진행 상황", progress);
        }

        // 막힘 요소
        if (checkpoint.getBlockers() != null) {
            ObjectNode blockers = objectMapper.createObjectNode();
            ArrayNode blockersArray = objectMapper.createArrayNode();
            ObjectNode blockersText = objectMapper.createObjectNode();
            ObjectNode blockersContent = objectMapper.createObjectNode();
            blockersContent.put("content", checkpoint.getBlockers());
            blockersText.set("text", blockersContent);
            blockersText.put("type", "text");
            blockersArray.add(blockersText);
            blockers.set("rich_text", blockersArray);
            properties.set("막힘 요소", blockers);
        }

        requestBody.set("properties", properties);

        try {
            HttpEntity<String> request = new HttpEntity<>(requestBody.toString(), headers);
            restTemplate.exchange(url, HttpMethod.POST, request, JsonNode.class);
        } catch (RestClientException e) {
            log.error("Notion checkpoint sync failed for week {}: {}", checkpoint.getWeekNumber(), e.getMessage());
            // 개별 체크포인트 실패는 전체를 중단하지 않음
        }
    }

    private String mapCheckpointStatus(Checkpoint checkpoint) {
        // 진행 상황 요약이 있으면 완료로 간주
        if (checkpoint.getProgressSummary() != null && !checkpoint.getProgressSummary().isBlank()) {
            return "완료";
        } else if (checkpoint.getWeeklyGoal() != null && !checkpoint.getWeeklyGoal().isBlank()) {
            return "진행 중";
        }
        return "진행 전";
    }

    private HttpHeaders createAuthHeaders(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + accessToken);
        headers.set("Notion-Version", NOTION_VERSION);
        return headers;
    }

    private ObjectNode createHeading2Block(String content) {
        ObjectNode block = objectMapper.createObjectNode();
        block.put("object", "block");
        block.put("type", "heading_2");

        ObjectNode heading = objectMapper.createObjectNode();
        ArrayNode richText = objectMapper.createArrayNode();
        ObjectNode textObj = objectMapper.createObjectNode();
        ObjectNode text = objectMapper.createObjectNode();
        text.put("content", content);
        textObj.set("text", text);
        textObj.put("type", "text");
        richText.add(textObj);
        heading.set("rich_text", richText);

        block.set("heading_2", heading);
        return block;
    }

    private ObjectNode createParagraphBlock(String content) {
        ObjectNode block = objectMapper.createObjectNode();
        block.put("object", "block");
        block.put("type", "paragraph");

        ObjectNode paragraph = objectMapper.createObjectNode();
        ArrayNode richText = objectMapper.createArrayNode();
        ObjectNode textObj = objectMapper.createObjectNode();
        ObjectNode text = objectMapper.createObjectNode();
        text.put("content", content);
        textObj.set("text", text);
        textObj.put("type", "text");
        richText.add(textObj);
        paragraph.set("rich_text", richText);

        block.set("paragraph", paragraph);
        return block;
    }

    private ObjectNode createDividerBlock() {
        ObjectNode block = objectMapper.createObjectNode();
        block.put("object", "block");
        block.put("type", "divider");
        block.set("divider", objectMapper.createObjectNode());
        return block;
    }

    private ObjectNode createSelectOption(String name, String color) {
        ObjectNode option = objectMapper.createObjectNode();
        option.put("name", name);
        option.put("color", color);
        return option;
    }

    // Response DTOs
    @lombok.Builder
    @lombok.Getter
    public static class NotionOAuthResponse {
        private String accessToken;
        private String workspaceId;
        private String workspaceName;
        private String botId;
    }

    @lombok.Builder
    @lombok.Getter
    public static class NotionPageResponse {
        private String pageId;
        private String pageUrl;
    }
}
