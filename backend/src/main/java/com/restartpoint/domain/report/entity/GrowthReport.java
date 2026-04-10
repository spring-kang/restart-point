package com.restartpoint.domain.report.entity;

import com.restartpoint.domain.project.entity.Project;
import com.restartpoint.domain.user.entity.User;
import com.restartpoint.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * 성장 리포트 엔티티
 * 시즌 종료 후 AI가 생성하는 팀/개인별 성장 피드백 리포트
 */
@Entity
@Table(name = "growth_reports", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"project_id", "user_id", "report_type"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class GrowthReport extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user; // 개인 리포트의 경우 사용자 정보, 팀 리포트는 null

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportType reportType;

    // 팀 강점
    @Column(columnDefinition = "TEXT")
    private String teamStrengths;

    // 팀 보완점
    @Column(columnDefinition = "TEXT")
    private String teamImprovements;

    // 역할별 개선 포인트 (개인 리포트에서 사용)
    @Column(columnDefinition = "TEXT")
    private String roleSpecificFeedback;

    // 다음 프로젝트 추천 액션
    @Column(columnDefinition = "TEXT")
    private String nextProjectActions;

    // 포트폴리오 보완 포인트
    @Column(columnDefinition = "TEXT")
    private String portfolioImprovements;

    // 재참여 추천 영역
    @Column(columnDefinition = "TEXT")
    private String recommendedAreas;

    // 심사 점수 요약
    private Double averageScore;

    // 루브릭별 점수 요약 (JSON 형태로 저장)
    @Column(columnDefinition = "TEXT")
    private String rubricScoreSummary;

    // 생성 상태 (AI 생성 실패 시 재시도 가능)
    @Builder.Default
    private boolean generated = false;

    public void updateContent(String teamStrengths, String teamImprovements,
                              String roleSpecificFeedback, String nextProjectActions,
                              String portfolioImprovements, String recommendedAreas) {
        this.teamStrengths = teamStrengths;
        this.teamImprovements = teamImprovements;
        this.roleSpecificFeedback = roleSpecificFeedback;
        this.nextProjectActions = nextProjectActions;
        this.portfolioImprovements = portfolioImprovements;
        this.recommendedAreas = recommendedAreas;
        this.generated = true;
    }

    public void setScoreSummary(Double averageScore, String rubricScoreSummary) {
        this.averageScore = averageScore;
        this.rubricScoreSummary = rubricScoreSummary;
    }
}
