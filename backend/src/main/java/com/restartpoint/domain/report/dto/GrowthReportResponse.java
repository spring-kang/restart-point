package com.restartpoint.domain.report.dto;

import com.restartpoint.domain.report.entity.GrowthReport;
import com.restartpoint.domain.report.entity.ReportType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class GrowthReportResponse {

    private Long id;
    private Long projectId;
    private String projectName;
    private String teamName;
    private Long userId;
    private String userName;
    private String userRole;
    private ReportType reportType;

    // 리포트 내용
    private String teamStrengths;
    private String teamImprovements;
    private String roleSpecificFeedback;
    private String nextProjectActions;
    private String portfolioImprovements;
    private String recommendedAreas;

    // 점수 요약
    private Double averageScore;
    private String rubricScoreSummary;

    private boolean generated;
    private LocalDateTime createdAt;

    public static GrowthReportResponse from(GrowthReport report) {
        GrowthReportResponseBuilder builder = GrowthReportResponse.builder()
                .id(report.getId())
                .projectId(report.getProject().getId())
                .projectName(report.getProject().getName())
                .teamName(report.getProject().getTeam().getName())
                .reportType(report.getReportType())
                .teamStrengths(report.getTeamStrengths())
                .teamImprovements(report.getTeamImprovements())
                .roleSpecificFeedback(report.getRoleSpecificFeedback())
                .nextProjectActions(report.getNextProjectActions())
                .portfolioImprovements(report.getPortfolioImprovements())
                .recommendedAreas(report.getRecommendedAreas())
                .averageScore(report.getAverageScore())
                .rubricScoreSummary(report.getRubricScoreSummary())
                .generated(report.isGenerated())
                .createdAt(report.getCreatedAt());

        if (report.getUser() != null) {
            builder.userId(report.getUser().getId())
                   .userName(report.getUser().getName());
        }

        return builder.build();
    }

    public static GrowthReportResponse simpleFrom(GrowthReport report) {
        GrowthReportResponseBuilder builder = GrowthReportResponse.builder()
                .id(report.getId())
                .projectId(report.getProject().getId())
                .projectName(report.getProject().getName())
                .teamName(report.getProject().getTeam().getName())
                .reportType(report.getReportType())
                .averageScore(report.getAverageScore())
                .generated(report.isGenerated())
                .createdAt(report.getCreatedAt());

        if (report.getUser() != null) {
            builder.userId(report.getUser().getId())
                   .userName(report.getUser().getName());
        }

        return builder.build();
    }
}
