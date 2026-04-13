package com.restartpoint.domain.admin.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;

@Getter
@Builder
public class TestDataSeedResponse {

    private String seedType;
    private boolean cleanupExecuted;
    private boolean seedExecuted;
    private OffsetDateTime executedAt;
}
