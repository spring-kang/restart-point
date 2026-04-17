package com.restartpoint.infra.notion;

import com.restartpoint.domain.project.entity.Project;
import com.restartpoint.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 프로젝트-Notion 동기화 정보
 * 프로젝트와 Notion 페이지/데이터베이스 연결 정보를 저장
 */
@Entity
@Table(name = "project_notion_syncs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProjectNotionSync extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false, unique = true)
    private Project project;

    // Notion Database ID (체크포인트 저장용)
    @Column(name = "database_id")
    private String databaseId;

    // Notion Page ID (프로젝트 메인 페이지)
    @Column(name = "page_id")
    private String pageId;

    // Notion Page URL
    @Column(name = "page_url", length = 500)
    private String pageUrl;

    // 동기화 상태
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SyncStatus status = SyncStatus.NOT_SYNCED;

    // 마지막 동기화 일시
    @Column(name = "last_synced_at")
    private LocalDateTime lastSyncedAt;

    // 동기화 에러 메시지
    @Column(name = "sync_error", length = 500)
    private String syncError;

    // 자동 동기화 활성화 여부
    @Column(name = "auto_sync", nullable = false)
    private Boolean autoSync = false;

    @Builder
    public ProjectNotionSync(Project project, String databaseId, String pageId, String pageUrl) {
        this.project = project;
        this.databaseId = databaseId;
        this.pageId = pageId;
        this.pageUrl = pageUrl;
        this.status = SyncStatus.NOT_SYNCED;
        this.autoSync = false;
    }

    public void markSyncing() {
        this.status = SyncStatus.SYNCING;
        this.syncError = null;
    }

    public void markSynced() {
        this.status = SyncStatus.SYNCED;
        this.lastSyncedAt = LocalDateTime.now();
        this.syncError = null;
    }

    public void markFailed(String error) {
        this.status = SyncStatus.FAILED;
        this.syncError = error;
    }

    public void updateNotionInfo(String databaseId, String pageId, String pageUrl) {
        this.databaseId = databaseId;
        this.pageId = pageId;
        this.pageUrl = pageUrl;
    }

    public void enableAutoSync() {
        this.autoSync = true;
    }

    public void disableAutoSync() {
        this.autoSync = false;
    }

    public enum SyncStatus {
        NOT_SYNCED,  // 동기화 안됨
        SYNCING,     // 동기화 중
        SYNCED,      // 동기화 완료
        FAILED       // 동기화 실패
    }
}
