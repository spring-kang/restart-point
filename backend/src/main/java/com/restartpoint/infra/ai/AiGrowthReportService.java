package com.restartpoint.infra.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.restartpoint.domain.profile.entity.JobRole;
import com.restartpoint.domain.project.entity.Project;
import com.restartpoint.domain.review.dto.ReviewSummaryResponse;
import com.restartpoint.domain.review.entity.RubricItem;
import com.restartpoint.domain.team.entity.TeamMember;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * AI 기반 성장 리포트 생성 서비스
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AiGrowthReportService {

    private final GroqService groqService;
    private final ObjectMapper objectMapper;

    private static final String SYSTEM_PROMPT = """
        당신은 IT 취업 준비생을 위한 프로젝트 코칭 전문가입니다.
        프로젝트 심사 결과를 바탕으로 팀과 개인의 성장을 위한 건설적인 피드백을 제공합니다.

        피드백은 다음 원칙을 따릅니다:
        1. 구체적이고 실행 가능한 조언을 제공합니다
        2. 강점을 먼저 언급하고, 개선점은 성장 기회로 표현합니다
        3. 다음 프로젝트에서 적용할 수 있는 실질적인 액션 아이템을 제시합니다
        4. 포트폴리오 개선을 위한 구체적인 가이드를 제공합니다

        응답은 반드시 유효한 JSON 형식이어야 합니다.
        """;

    /**
     * 팀 성장 리포트 생성
     */
    public Map<String, String> generateTeamReport(Project project, ReviewSummaryResponse reviewSummary) {
        String userMessage = buildTeamReportPrompt(project, reviewSummary);

        try {
            String response = groqService.chat(SYSTEM_PROMPT, userMessage);
            if (response != null) {
                return parseReportResponse(response);
            }
        } catch (Exception e) {
            log.error("팀 성장 리포트 생성 실패 - 프로젝트: {}, 오류: {}", project.getName(), e.getMessage());
        }

        return null;
    }

    /**
     * 개인 성장 리포트 생성
     */
    public Map<String, String> generateIndividualReport(Project project, ReviewSummaryResponse reviewSummary,
                                                         Long userId, String userName, JobRole jobRole) {
        String userMessage = buildIndividualReportPrompt(project, reviewSummary, userName, jobRole);

        try {
            String response = groqService.chat(SYSTEM_PROMPT, userMessage);
            if (response != null) {
                return parseReportResponse(response);
            }
        } catch (Exception e) {
            log.error("개인 성장 리포트 생성 실패 - 사용자: {}, 오류: {}", userName, e.getMessage());
        }

        return null;
    }

    /**
     * 비동기 팀 리포트 생성
     */
    @Async
    public CompletableFuture<Map<String, String>> generateTeamReportAsync(Project project, ReviewSummaryResponse reviewSummary) {
        return CompletableFuture.completedFuture(generateTeamReport(project, reviewSummary));
    }

    /**
     * 비동기 개인 리포트 생성
     */
    @Async
    public CompletableFuture<Map<String, String>> generateIndividualReportAsync(Project project, ReviewSummaryResponse reviewSummary,
                                                                                  Long userId, String userName, JobRole jobRole) {
        return CompletableFuture.completedFuture(generateIndividualReport(project, reviewSummary, userId, userName, jobRole));
    }

    private String buildTeamReportPrompt(Project project, ReviewSummaryResponse reviewSummary) {
        StringBuilder sb = new StringBuilder();

        sb.append("다음 프로젝트의 심사 결과를 바탕으로 팀 성장 리포트를 작성해주세요.\n\n");

        sb.append("## 프로젝트 정보\n");
        sb.append("- 프로젝트명: ").append(project.getName()).append("\n");
        sb.append("- 팀명: ").append(project.getTeam().getName()).append("\n");
        sb.append("- 문제 정의: ").append(project.getProblemDefinition()).append("\n");
        sb.append("- 타깃 사용자: ").append(project.getTargetUsers()).append("\n");
        sb.append("- 핵심 솔루션: ").append(project.getSolution()).append("\n");
        sb.append("- AI 활용: ").append(project.getAiUsage()).append("\n\n");

        sb.append("## 심사 결과\n");
        sb.append("- 전체 평균: ").append(String.format("%.2f", reviewSummary.getWeightedAverageScore())).append("/5.0\n");
        sb.append("- 전문가 평균: ").append(String.format("%.2f", reviewSummary.getExpertAverageScore())).append("/5.0\n");
        sb.append("- 수료생 평균: ").append(String.format("%.2f", reviewSummary.getCandidateAverageScore())).append("/5.0\n\n");

        sb.append("## 루브릭별 점수\n");
        for (RubricItem item : RubricItem.values()) {
            Double score = reviewSummary.getRubricAverages().get(item);
            sb.append("- ").append(item.getLabel()).append(": ")
              .append(score != null ? String.format("%.2f", score) : "N/A").append("/5.0\n");
        }

        sb.append("\n## 요청 사항\n");
        sb.append("위 정보를 바탕으로 다음 JSON 형식으로 응답해주세요:\n");
        sb.append("""
            {
                "teamStrengths": "팀의 강점 3-5가지 (마크다운 리스트 형식)",
                "teamImprovements": "팀이 보완해야 할 점 3-5가지 (마크다운 리스트 형식)",
                "nextProjectActions": "다음 프로젝트에서 적용할 액션 아이템 3-5가지 (마크다운 리스트 형식)",
                "recommendedAreas": "재참여 시 추천하는 도전 영역 (마크다운 형식)"
            }
            """);

        return sb.toString();
    }

    private String buildIndividualReportPrompt(Project project, ReviewSummaryResponse reviewSummary,
                                                String userName, JobRole jobRole) {
        StringBuilder sb = new StringBuilder();

        sb.append("다음 프로젝트의 심사 결과를 바탕으로 개인 성장 리포트를 작성해주세요.\n\n");

        sb.append("## 대상자 정보\n");
        sb.append("- 이름: ").append(userName).append("\n");
        sb.append("- 역할: ").append(getRoleLabel(jobRole)).append("\n\n");

        sb.append("## 프로젝트 정보\n");
        sb.append("- 프로젝트명: ").append(project.getName()).append("\n");
        sb.append("- 팀명: ").append(project.getTeam().getName()).append("\n");
        sb.append("- 문제 정의: ").append(project.getProblemDefinition()).append("\n");
        sb.append("- 핵심 솔루션: ").append(project.getSolution()).append("\n\n");

        sb.append("## 심사 결과\n");
        sb.append("- 전체 평균: ").append(String.format("%.2f", reviewSummary.getWeightedAverageScore())).append("/5.0\n\n");

        sb.append("## 역할별 관련 루브릭 점수\n");
        for (RubricItem item : getRelevantRubrics(jobRole)) {
            Double score = reviewSummary.getRubricAverages().get(item);
            sb.append("- ").append(item.getLabel()).append(": ")
              .append(score != null ? String.format("%.2f", score) : "N/A").append("/5.0\n");
        }

        sb.append("\n## 요청 사항\n");
        sb.append("위 정보를 바탕으로 ").append(getRoleLabel(jobRole)).append(" 역할에 맞는 개인 성장 리포트를 다음 JSON 형식으로 응답해주세요:\n");
        sb.append("""
            {
                "roleSpecificFeedback": "해당 역할에 특화된 피드백 3-5가지 (마크다운 리스트 형식)",
                "nextProjectActions": "다음 프로젝트에서 적용할 개인 액션 아이템 3-5가지 (마크다운 리스트 형식)",
                "portfolioImprovements": "포트폴리오 보완을 위한 구체적인 가이드 (마크다운 형식)",
                "recommendedAreas": "성장을 위해 추천하는 학습/도전 영역 (마크다운 형식)"
            }
            """);

        return sb.toString();
    }

    private String getRoleLabel(JobRole role) {
        return switch (role) {
            case PLANNER -> "기획자";
            case UXUI -> "UX/UI 디자이너";
            case FRONTEND -> "프론트엔드 개발자";
            case BACKEND -> "백엔드 개발자";
        };
    }

    private List<RubricItem> getRelevantRubrics(JobRole role) {
        return switch (role) {
            case PLANNER -> List.of(RubricItem.PROBLEM_DEFINITION, RubricItem.USER_VALUE, RubricItem.COLLABORATION);
            case UXUI -> List.of(RubricItem.UX_COMPLETENESS, RubricItem.USER_VALUE, RubricItem.COLLABORATION);
            case FRONTEND, BACKEND -> List.of(RubricItem.TECHNICAL_FEASIBILITY, RubricItem.AI_USAGE, RubricItem.COLLABORATION);
        };
    }

    private Map<String, String> parseReportResponse(String response) {
        try {
            // JSON 부분만 추출 (AI 응답에 추가 텍스트가 있을 수 있음)
            String jsonContent = extractJson(response);
            return objectMapper.readValue(jsonContent, Map.class);
        } catch (JsonProcessingException e) {
            log.error("리포트 응답 파싱 실패: {}", e.getMessage());
            return null;
        }
    }

    private String extractJson(String response) {
        int start = response.indexOf('{');
        int end = response.lastIndexOf('}');
        if (start != -1 && end != -1 && end > start) {
            return response.substring(start, end + 1);
        }
        return response;
    }
}
