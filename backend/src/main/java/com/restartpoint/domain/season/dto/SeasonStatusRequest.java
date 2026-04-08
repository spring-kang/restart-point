package com.restartpoint.domain.season.dto;

import com.restartpoint.domain.season.entity.SeasonStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class SeasonStatusRequest {

    @NotNull(message = "상태는 필수입니다.")
    private SeasonStatus status;
}
