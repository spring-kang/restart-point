package com.restartpoint.domain.season.entity;

import com.restartpoint.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "seasons")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Season extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SeasonStatus status;

    // 모집 기간
    @Column(name = "recruitment_start_at", nullable = false)
    private LocalDateTime recruitmentStartAt;

    @Column(name = "recruitment_end_at", nullable = false)
    private LocalDateTime recruitmentEndAt;

    // 팀빌딩 기간
    @Column(name = "team_building_start_at", nullable = false)
    private LocalDateTime teamBuildingStartAt;

    @Column(name = "team_building_end_at", nullable = false)
    private LocalDateTime teamBuildingEndAt;

    // 프로젝트 기간
    @Column(name = "project_start_at", nullable = false)
    private LocalDateTime projectStartAt;

    @Column(name = "project_end_at", nullable = false)
    private LocalDateTime projectEndAt;

    // 심사 기간
    @Column(name = "review_start_at", nullable = false)
    private LocalDateTime reviewStartAt;

    @Column(name = "review_end_at", nullable = false)
    private LocalDateTime reviewEndAt;

    // 심사 비중 설정
    @Column(name = "expert_review_weight")
    private Integer expertReviewWeight = 100;  // 전문가 심사 비중 (%)

    @Column(name = "candidate_review_weight")
    private Integer candidateReviewWeight = 0;  // 참여자 심사는 비활성화

    // 참여 자격 설정 (기존 데이터 호환성을 위해 nullable)
    @Column(name = "requires_certification")
    private Boolean requiresCertification = false;  // false: 누구나 참여 가능, true: 수료생만

    @Builder
    public Season(String title, String description,
                  LocalDateTime recruitmentStartAt, LocalDateTime recruitmentEndAt,
                  LocalDateTime teamBuildingStartAt, LocalDateTime teamBuildingEndAt,
                  LocalDateTime projectStartAt, LocalDateTime projectEndAt,
                  LocalDateTime reviewStartAt, LocalDateTime reviewEndAt,
                  Integer expertReviewWeight, Integer candidateReviewWeight,
                  Boolean requiresCertification) {
        this.title = title;
        this.description = description;
        this.status = SeasonStatus.DRAFT;
        this.recruitmentStartAt = recruitmentStartAt;
        this.recruitmentEndAt = recruitmentEndAt;
        this.teamBuildingStartAt = teamBuildingStartAt;
        this.teamBuildingEndAt = teamBuildingEndAt;
        this.projectStartAt = projectStartAt;
        this.projectEndAt = projectEndAt;
        this.reviewStartAt = reviewStartAt;
        this.reviewEndAt = reviewEndAt;
        this.expertReviewWeight = expertReviewWeight != null ? expertReviewWeight : 100;
        this.candidateReviewWeight = candidateReviewWeight != null ? candidateReviewWeight : 0;
        this.requiresCertification = requiresCertification != null ? requiresCertification : false;
    }

    public void updateStatus(SeasonStatus status) {
        this.status = status;
    }

    public void update(String title, String description,
                       LocalDateTime recruitmentStartAt, LocalDateTime recruitmentEndAt,
                       LocalDateTime teamBuildingStartAt, LocalDateTime teamBuildingEndAt,
                       LocalDateTime projectStartAt, LocalDateTime projectEndAt,
                       LocalDateTime reviewStartAt, LocalDateTime reviewEndAt,
                       Integer expertReviewWeight, Integer candidateReviewWeight,
                       Boolean requiresCertification) {
        this.title = title;
        this.description = description;
        this.recruitmentStartAt = recruitmentStartAt;
        this.recruitmentEndAt = recruitmentEndAt;
        this.teamBuildingStartAt = teamBuildingStartAt;
        this.teamBuildingEndAt = teamBuildingEndAt;
        this.projectStartAt = projectStartAt;
        this.projectEndAt = projectEndAt;
        this.reviewStartAt = reviewStartAt;
        this.reviewEndAt = reviewEndAt;
        this.expertReviewWeight = expertReviewWeight != null ? expertReviewWeight : 100;
        this.candidateReviewWeight = candidateReviewWeight != null ? candidateReviewWeight : 0;
        this.requiresCertification = requiresCertification != null ? requiresCertification : false;
    }

    /**
     * 수료 인증이 필요한 시즌인지 확인 (null 안전 처리)
     */
    public Boolean getRequiresCertification() {
        return requiresCertification != null ? requiresCertification : false;
    }

    /**
     * 해당 사용자가 이 시즌에 참여할 자격이 있는지 확인
     */
    public boolean canUserParticipate(boolean isCertified) {
        // 인증이 필요 없는 시즌이거나, 인증된 사용자면 참여 가능
        return !getRequiresCertification() || isCertified;
    }

    public boolean isRecruiting() {
        LocalDateTime now = LocalDateTime.now();
        return status == SeasonStatus.RECRUITING &&
               now.isAfter(recruitmentStartAt) && now.isBefore(recruitmentEndAt);
    }

    public boolean isTeamBuilding() {
        LocalDateTime now = LocalDateTime.now();
        return status == SeasonStatus.TEAM_BUILDING &&
               now.isAfter(teamBuildingStartAt) && now.isBefore(teamBuildingEndAt);
    }

    /**
     * 팀 생성/참여가 가능한 상태인지 확인
     * RECRUITING 또는 TEAM_BUILDING 상태이고, 해당 기간 내일 때 true 반환
     */
    public boolean canJoinTeam() {
        return isRecruiting() || isTeamBuilding();
    }
}
