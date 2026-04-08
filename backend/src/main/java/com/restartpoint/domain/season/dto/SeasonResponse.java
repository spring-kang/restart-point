package com.restartpoint.domain.season.dto;

import com.restartpoint.domain.season.entity.Season;
import com.restartpoint.domain.season.entity.SeasonStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class SeasonResponse {

    private Long id;
    private String title;
    private String description;
    private SeasonStatus status;
    private LocalDateTime recruitmentStartAt;
    private LocalDateTime recruitmentEndAt;
    private LocalDateTime teamBuildingStartAt;
    private LocalDateTime teamBuildingEndAt;
    private LocalDateTime projectStartAt;
    private LocalDateTime projectEndAt;
    private LocalDateTime reviewStartAt;
    private LocalDateTime reviewEndAt;
    private Integer expertReviewWeight;
    private Integer candidateReviewWeight;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 계산된 필드
    private String currentPhase;
    private boolean canJoin;

    public static SeasonResponse from(Season season) {
        return SeasonResponse.builder()
                .id(season.getId())
                .title(season.getTitle())
                .description(season.getDescription())
                .status(season.getStatus())
                .recruitmentStartAt(season.getRecruitmentStartAt())
                .recruitmentEndAt(season.getRecruitmentEndAt())
                .teamBuildingStartAt(season.getTeamBuildingStartAt())
                .teamBuildingEndAt(season.getTeamBuildingEndAt())
                .projectStartAt(season.getProjectStartAt())
                .projectEndAt(season.getProjectEndAt())
                .reviewStartAt(season.getReviewStartAt())
                .reviewEndAt(season.getReviewEndAt())
                .expertReviewWeight(season.getExpertReviewWeight())
                .candidateReviewWeight(season.getCandidateReviewWeight())
                .createdAt(season.getCreatedAt())
                .updatedAt(season.getUpdatedAt())
                .currentPhase(calculateCurrentPhase(season))
                .canJoin(season.isRecruiting())
                .build();
    }

    private static String calculateCurrentPhase(Season season) {
        LocalDateTime now = LocalDateTime.now();

        if (season.getStatus() == SeasonStatus.DRAFT) {
            return "준비 중";
        }
        if (now.isBefore(season.getRecruitmentStartAt())) {
            return "시작 전";
        }
        if (now.isBefore(season.getRecruitmentEndAt())) {
            return "모집 중";
        }
        if (now.isBefore(season.getTeamBuildingEndAt())) {
            return "팀빌딩 중";
        }
        if (now.isBefore(season.getProjectEndAt())) {
            return "프로젝트 진행 중";
        }
        if (now.isBefore(season.getReviewEndAt())) {
            return "심사 중";
        }
        return "종료";
    }
}
