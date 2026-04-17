package com.restartpoint.infra.notion.dto;

import com.restartpoint.infra.notion.ProjectNotionSync;
import com.restartpoint.infra.notion.ProjectNotionSync.SyncStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ProjectNotionSyncResponse {

    private Long id;
    private Long projectId;
    private String projectName;
    private String databaseId;
    private String pageId;
    private String pageUrl;
    private SyncStatus status;
    private LocalDateTime lastSyncedAt;
    private String syncError;
    private Boolean autoSync;

    public static ProjectNotionSyncResponse from(ProjectNotionSync sync) {
        return ProjectNotionSyncResponse.builder()
                .id(sync.getId())
                .projectId(sync.getProject().getId())
                .projectName(sync.getProject().getName())
                .databaseId(sync.getDatabaseId())
                .pageId(sync.getPageId())
                .pageUrl(sync.getPageUrl())
                .status(sync.getStatus())
                .lastSyncedAt(sync.getLastSyncedAt())
                .syncError(sync.getSyncError())
                .autoSync(sync.getAutoSync())
                .build();
    }
}
