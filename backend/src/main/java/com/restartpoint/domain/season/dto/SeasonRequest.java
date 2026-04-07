package com.restartpoint.domain.season.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
public class SeasonRequest {

    @NotBlank(message = "시즌 제목은 필수입니다.")
    private String title;

    private String description;

    @NotNull(message = "모집 시작일은 필수입니다.")
    private LocalDateTime recruitmentStartAt;

    @NotNull(message = "모집 종료일은 필수입니다.")
    private LocalDateTime recruitmentEndAt;

    @NotNull(message = "팀빌딩 시작일은 필수입니다.")
    private LocalDateTime teamBuildingStartAt;

    @NotNull(message = "팀빌딩 종료일은 필수입니다.")
    private LocalDateTime teamBuildingEndAt;

    @NotNull(message = "프로젝트 시작일은 필수입니다.")
    private LocalDateTime projectStartAt;

    @NotNull(message = "프로젝트 종료일은 필수입니다.")
    private LocalDateTime projectEndAt;

    @NotNull(message = "심사 시작일은 필수입니다.")
    private LocalDateTime reviewStartAt;

    @NotNull(message = "심사 종료일은 필수입니다.")
    private LocalDateTime reviewEndAt;

    @Min(value = 0, message = "현직자 심사 비중은 0 이상이어야 합니다.")
    @Max(value = 100, message = "현직자 심사 비중은 100 이하여야 합니다.")
    private Integer expertReviewWeight = 70;

    @Min(value = 0, message = "예비 참여자 심사 비중은 0 이상이어야 합니다.")
    @Max(value = 100, message = "예비 참여자 심사 비중은 100 이하여야 합니다.")
    private Integer candidateReviewWeight = 30;
}
