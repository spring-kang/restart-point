package com.restartpoint.infra.notion;

import com.restartpoint.domain.user.entity.User;
import com.restartpoint.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Notion 계정 연동 정보
 * 사용자의 Notion OAuth 인증 정보를 저장
 */
@Entity
@Table(name = "notion_integrations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotionIntegration extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    // Notion Access Token (암호화 저장 권장)
    @Column(name = "access_token", nullable = false, length = 500)
    private String accessToken;

    // Notion Workspace ID
    @Column(name = "workspace_id")
    private String workspaceId;

    // Notion Workspace 이름
    @Column(name = "workspace_name")
    private String workspaceName;

    // Bot ID
    @Column(name = "bot_id")
    private String botId;

    // 연동 상태
    @Column(nullable = false)
    private Boolean active = true;

    // 마지막 동기화 일시
    @Column(name = "last_synced_at")
    private LocalDateTime lastSyncedAt;

    // 연동 일시
    @Column(name = "connected_at", nullable = false)
    private LocalDateTime connectedAt;

    @Builder
    public NotionIntegration(User user, String accessToken, String workspaceId,
                             String workspaceName, String botId) {
        this.user = user;
        this.accessToken = accessToken;
        this.workspaceId = workspaceId;
        this.workspaceName = workspaceName;
        this.botId = botId;
        this.active = true;
        this.connectedAt = LocalDateTime.now();
    }

    public void updateToken(String accessToken, String workspaceId, String workspaceName, String botId) {
        this.accessToken = accessToken;
        this.workspaceId = workspaceId;
        this.workspaceName = workspaceName;
        this.botId = botId;
    }

    public void updateLastSyncedAt() {
        this.lastSyncedAt = LocalDateTime.now();
    }

    public void activate() {
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
    }
}
