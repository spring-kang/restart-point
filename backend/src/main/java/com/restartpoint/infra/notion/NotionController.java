package com.restartpoint.infra.notion;

import com.restartpoint.global.security.CurrentUser;
import com.restartpoint.global.security.CustomUserPrincipal;
import com.restartpoint.infra.notion.dto.NotionConnectRequest;
import com.restartpoint.infra.notion.dto.NotionIntegrationResponse;
import com.restartpoint.infra.notion.dto.ProjectNotionSyncResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@Tag(name = "Notion", description = "Notion 연동 API")
@RestController
@RequestMapping("/api/v1/notion")
@RequiredArgsConstructor
public class NotionController {

    private final NotionService notionService;

    // ========== Notion Integration ==========

    @Operation(summary = "Notion OAuth URL 생성", description = "Notion 연동을 위한 OAuth URL을 생성합니다.")
    @GetMapping("/oauth/url")
    public ResponseEntity<Map<String, String>> getOAuthUrl() {
        String state = UUID.randomUUID().toString();
        String url = notionService.getOAuthUrl(state);
        return ResponseEntity.ok(Map.of("url", url, "state", state));
    }

    @Operation(summary = "Notion 연동", description = "OAuth 인증 코드로 Notion을 연동합니다.")
    @PostMapping("/connect")
    public ResponseEntity<NotionIntegrationResponse> connectNotion(
            @Parameter(hidden = true) @CurrentUser CustomUserPrincipal userPrincipal,
            @Valid @RequestBody NotionConnectRequest request) {
        return ResponseEntity.ok(notionService.connectNotion(userPrincipal.getUserId(), request));
    }

    @Operation(summary = "Notion 연동 해제", description = "Notion 연동을 해제합니다.")
    @PostMapping("/disconnect")
    public ResponseEntity<Void> disconnectNotion(
            @Parameter(hidden = true) @CurrentUser CustomUserPrincipal userPrincipal) {
        notionService.disconnectNotion(userPrincipal.getUserId());
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Notion 연동 상태 조회", description = "현재 Notion 연동 상태를 조회합니다.")
    @GetMapping("/status")
    public ResponseEntity<NotionIntegrationResponse> getIntegrationStatus(
            @Parameter(hidden = true) @CurrentUser CustomUserPrincipal userPrincipal) {
        return ResponseEntity.ok(notionService.getIntegrationStatus(userPrincipal.getUserId()));
    }

    @Operation(summary = "Notion 연동 여부 확인", description = "Notion이 연동되어 있는지 확인합니다.")
    @GetMapping("/connected")
    public ResponseEntity<Boolean> isConnected(
            @Parameter(hidden = true) @CurrentUser CustomUserPrincipal userPrincipal) {
        return ResponseEntity.ok(notionService.isConnected(userPrincipal.getUserId()));
    }

    // ========== Project Sync ==========

    @Operation(summary = "프로젝트 Notion 동기화 설정", description = "프로젝트를 Notion과 연결합니다.")
    @PostMapping("/projects/{projectId}/setup")
    public ResponseEntity<ProjectNotionSyncResponse> setupProjectSync(
            @Parameter(hidden = true) @CurrentUser CustomUserPrincipal userPrincipal,
            @PathVariable Long projectId) {
        return ResponseEntity.ok(notionService.setupProjectSync(userPrincipal.getUserId(), projectId));
    }

    @Operation(summary = "프로젝트 Notion 동기화", description = "프로젝트 정보를 Notion과 동기화합니다.")
    @PostMapping("/projects/{projectId}/sync")
    public ResponseEntity<ProjectNotionSyncResponse> syncProject(
            @Parameter(hidden = true) @CurrentUser CustomUserPrincipal userPrincipal,
            @PathVariable Long projectId) {
        return ResponseEntity.ok(notionService.syncProject(userPrincipal.getUserId(), projectId));
    }

    @Operation(summary = "프로젝트 동기화 상태 조회", description = "프로젝트의 Notion 동기화 상태를 조회합니다.")
    @GetMapping("/projects/{projectId}/status")
    public ResponseEntity<ProjectNotionSyncResponse> getSyncStatus(@PathVariable Long projectId) {
        return ResponseEntity.ok(notionService.getSyncStatus(projectId));
    }

    @Operation(summary = "자동 동기화 활성화", description = "프로젝트의 자동 Notion 동기화를 활성화합니다.")
    @PostMapping("/projects/{projectId}/auto-sync/enable")
    public ResponseEntity<ProjectNotionSyncResponse> enableAutoSync(@PathVariable Long projectId) {
        return ResponseEntity.ok(notionService.enableAutoSync(projectId));
    }

    @Operation(summary = "자동 동기화 비활성화", description = "프로젝트의 자동 Notion 동기화를 비활성화합니다.")
    @PostMapping("/projects/{projectId}/auto-sync/disable")
    public ResponseEntity<ProjectNotionSyncResponse> disableAutoSync(@PathVariable Long projectId) {
        return ResponseEntity.ok(notionService.disableAutoSync(projectId));
    }
}
