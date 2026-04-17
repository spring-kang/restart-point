package com.restartpoint.infra.notion.dto;

import com.restartpoint.infra.notion.NotionIntegration;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class NotionIntegrationResponse {

    private Long id;
    private Long userId;
    private String workspaceId;
    private String workspaceName;
    private Boolean active;
    private LocalDateTime lastSyncedAt;
    private LocalDateTime connectedAt;

    public static NotionIntegrationResponse from(NotionIntegration integration) {
        return NotionIntegrationResponse.builder()
                .id(integration.getId())
                .userId(integration.getUser().getId())
                .workspaceId(integration.getWorkspaceId())
                .workspaceName(integration.getWorkspaceName())
                .active(integration.getActive())
                .lastSyncedAt(integration.getLastSyncedAt())
                .connectedAt(integration.getConnectedAt())
                .build();
    }
}
