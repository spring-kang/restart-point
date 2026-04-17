package com.restartpoint.infra.notion;

import com.restartpoint.domain.project.entity.Checkpoint;
import com.restartpoint.domain.project.entity.Project;
import com.restartpoint.domain.project.repository.CheckpointRepository;
import com.restartpoint.domain.project.repository.ProjectRepository;
import com.restartpoint.domain.team.entity.Team;
import com.restartpoint.domain.team.entity.TeamMemberStatus;
import com.restartpoint.domain.team.repository.TeamMemberRepository;
import com.restartpoint.domain.user.entity.User;
import com.restartpoint.domain.user.repository.UserRepository;
import com.restartpoint.global.exception.BusinessException;
import com.restartpoint.global.exception.ErrorCode;
import com.restartpoint.infra.notion.dto.NotionConnectRequest;
import com.restartpoint.infra.notion.dto.NotionIntegrationResponse;
import com.restartpoint.infra.notion.dto.ProjectNotionSyncResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotionService {

    private final NotionIntegrationRepository integrationRepository;
    private final ProjectNotionSyncRepository syncRepository;
    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final CheckpointRepository checkpointRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final NotionApiClient notionApiClient;

    @Value("${notion.client-id:}")
    private String clientId;

    @Value("${notion.redirect-uri:}")
    private String redirectUri;

    @Value("${notion.enabled:false}")
    private boolean notionEnabled;

    // ========== Notion Integration ==========

    @Transactional
    public NotionIntegrationResponse connectNotion(Long userId, NotionConnectRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        String accessToken;
        String workspaceId;
        String workspaceName;
        String botId;

        if (notionEnabled) {
            // 실제 Notion OAuth API 호출
            NotionApiClient.NotionOAuthResponse oauthResponse =
                    notionApiClient.exchangeCodeForToken(request.getCode(), request.getRedirectUri());
            accessToken = oauthResponse.getAccessToken();
            workspaceId = oauthResponse.getWorkspaceId();
            workspaceName = oauthResponse.getWorkspaceName();
            botId = oauthResponse.getBotId();
        } else {
            // 개발/테스트 환경: 임시 구현
            log.info("Notion integration is disabled. Using mock data.");
            accessToken = "mock_access_token_" + request.getCode();
            workspaceId = "workspace_" + System.currentTimeMillis();
            workspaceName = "Mock Workspace";
            botId = "bot_" + System.currentTimeMillis();
        }

        NotionIntegration integration = integrationRepository.findByUserId(userId)
                .map(existing -> {
                    existing.updateToken(accessToken, workspaceId, workspaceName, botId);
                    existing.activate();
                    return existing;
                })
                .orElseGet(() -> NotionIntegration.builder()
                        .user(user)
                        .accessToken(accessToken)
                        .workspaceId(workspaceId)
                        .workspaceName(workspaceName)
                        .botId(botId)
                        .build());

        NotionIntegration saved = integrationRepository.save(integration);
        return NotionIntegrationResponse.from(saved);
    }

    @Transactional
    public void disconnectNotion(Long userId) {
        NotionIntegration integration = integrationRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOTION_NOT_CONNECTED));

        integration.deactivate();
    }

    public NotionIntegrationResponse getIntegrationStatus(Long userId) {
        NotionIntegration integration = integrationRepository.findByUserIdAndActiveTrue(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOTION_NOT_CONNECTED));
        return NotionIntegrationResponse.from(integration);
    }

    public boolean isConnected(Long userId) {
        return integrationRepository.existsByUserIdAndActiveTrue(userId);
    }

    // ========== Project Sync ==========

    @Transactional
    public ProjectNotionSyncResponse setupProjectSync(Long userId, Long projectId) {
        // 사용자의 Notion 연동 확인
        NotionIntegration integration = integrationRepository.findByUserIdAndActiveTrue(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOTION_NOT_CONNECTED));

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROJECT_NOT_FOUND));

        // 프로젝트 접근 권한 확인
        validateProjectAccess(project, userId);

        // 이미 설정된 경우
        Optional<ProjectNotionSync> existingSync = syncRepository.findByProjectId(projectId);
        if (existingSync.isPresent()) {
            return ProjectNotionSyncResponse.from(existingSync.get());
        }

        String pageId;
        String databaseId;
        String pageUrl;

        if (notionEnabled) {
            // 실제 Notion API로 페이지 생성
            NotionApiClient.NotionPageResponse pageResponse =
                    notionApiClient.createProjectPage(integration.getAccessToken(), project);
            pageId = pageResponse.getPageId();
            pageUrl = pageResponse.getPageUrl();

            // 체크포인트 데이터베이스 생성
            databaseId = notionApiClient.createCheckpointDatabase(
                    integration.getAccessToken(), pageId, project);
        } else {
            // 개발/테스트 환경: 임시 구현
            log.info("Notion integration is disabled. Using mock page creation.");
            pageId = "mock_page_" + System.currentTimeMillis();
            databaseId = "mock_db_" + System.currentTimeMillis();
            pageUrl = "https://notion.so/" + pageId.replace("-", "");
        }

        ProjectNotionSync sync = ProjectNotionSync.builder()
                .project(project)
                .pageId(pageId)
                .databaseId(databaseId)
                .pageUrl(pageUrl)
                .build();

        sync.markSynced();
        ProjectNotionSync saved = syncRepository.save(sync);
        return ProjectNotionSyncResponse.from(saved);
    }

    @Transactional
    public ProjectNotionSyncResponse syncProject(Long userId, Long projectId) {
        // 사용자의 Notion 연동 확인
        NotionIntegration integration = integrationRepository.findByUserIdAndActiveTrue(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOTION_NOT_CONNECTED));

        ProjectNotionSync sync = syncRepository.findByProjectId(projectId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOTION_SYNC_NOT_FOUND));

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROJECT_NOT_FOUND));

        // 프로젝트 접근 권한 확인
        validateProjectAccess(project, userId);

        sync.markSyncing();

        try {
            if (notionEnabled) {
                // 실제 Notion API로 동기화
                List<Checkpoint> checkpoints = checkpointRepository.findByProjectIdOrderByWeekNumberAsc(projectId);
                notionApiClient.syncCheckpoints(
                        integration.getAccessToken(),
                        sync.getDatabaseId(),
                        checkpoints
                );
            } else {
                log.info("Notion integration is disabled. Skipping actual sync.");
            }

            sync.markSynced();
        } catch (Exception e) {
            log.error("Notion sync failed for project {}: {}", projectId, e.getMessage());
            sync.markFailed(e.getMessage());
            throw new BusinessException(ErrorCode.NOTION_SYNC_FAILED);
        }

        return ProjectNotionSyncResponse.from(sync);
    }

    public ProjectNotionSyncResponse getSyncStatus(Long userId, Long projectId) {
        ProjectNotionSync sync = syncRepository.findByProjectId(projectId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOTION_SYNC_NOT_FOUND));

        // 프로젝트 접근 권한 확인
        validateProjectAccess(sync.getProject(), userId);

        return ProjectNotionSyncResponse.from(sync);
    }

    @Transactional
    public ProjectNotionSyncResponse enableAutoSync(Long userId, Long projectId) {
        ProjectNotionSync sync = syncRepository.findByProjectId(projectId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOTION_SYNC_NOT_FOUND));

        // 프로젝트 접근 권한 확인
        validateProjectAccess(sync.getProject(), userId);

        sync.enableAutoSync();
        return ProjectNotionSyncResponse.from(sync);
    }

    @Transactional
    public ProjectNotionSyncResponse disableAutoSync(Long userId, Long projectId) {
        ProjectNotionSync sync = syncRepository.findByProjectId(projectId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOTION_SYNC_NOT_FOUND));

        // 프로젝트 접근 권한 확인
        validateProjectAccess(sync.getProject(), userId);

        sync.disableAutoSync();
        return ProjectNotionSyncResponse.from(sync);
    }

    // ========== OAuth URL 생성 ==========

    public String getOAuthUrl(String state) {
        return String.format(
                "https://api.notion.com/v1/oauth/authorize?client_id=%s&response_type=code&owner=user&redirect_uri=%s&state=%s",
                clientId, redirectUri, state
        );
    }

    // ========== Helper Methods ==========

    /**
     * 사용자가 해당 프로젝트에 접근할 수 있는지 확인
     * (팀 리더이거나 ACCEPTED 상태의 팀원인 경우에만 접근 가능)
     */
    private void validateProjectAccess(Project project, Long userId) {
        Team team = project.getTeam();

        // 팀 리더인 경우
        if (team.getLeader().getId().equals(userId)) {
            return;
        }

        // ACCEPTED 상태의 팀원인 경우
        boolean isTeamMember = teamMemberRepository.existsByTeamAndUserIdAndStatus(
                team, userId, TeamMemberStatus.ACCEPTED);

        if (!isTeamMember) {
            throw new BusinessException(ErrorCode.NOT_TEAM_MEMBER);
        }
    }
}
