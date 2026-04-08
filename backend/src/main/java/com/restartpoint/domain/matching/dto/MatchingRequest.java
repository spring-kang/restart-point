package com.restartpoint.domain.matching.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class MatchingRequest {

    @NotNull(message = "시즌 ID는 필수입니다.")
    private Long seasonId;

    private Integer limit = 5; // 기본 추천 개수
}
