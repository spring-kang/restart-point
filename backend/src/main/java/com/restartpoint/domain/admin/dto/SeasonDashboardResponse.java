package com.restartpoint.domain.admin.dto;

import com.restartpoint.domain.profile.entity.JobRole;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

/**
 * 시즌별 대시보드 응답 DTO
 */
@Getter
@Builder
public class SeasonDashboardResponse {

    private Long seasonId;
    private String seasonTitle;
    private String seasonStatus;

    // 참가자 통계
    private ParticipantStats participantStats;

    // 팀 통계
    private TeamStats teamStats;

    // 프로젝트 통계
    private ProjectStats projectStats;

    // 심사 통계
    private ReviewStats reviewStats;

    // 리포트 통계
    private ReportStats reportStats;

    // 위험 팀 목록 (미완성/미제출)
    private List<RiskTeam> riskTeams;

    @Getter
    @Builder
    public static class ParticipantStats {
        private int totalParticipants;
        private int certifiedParticipants;
        private int pendingCertifications;
        private Map<JobRole, Integer> roleDistribution;
    }

    @Getter
    @Builder
    public static class TeamStats {
        private int totalTeams;
        private int completeTeams;
        private int incompleteTeams;
        private int recruitingTeams;
        private List<IncompleteTeam> incompleteTeamList;
    }

    @Getter
    @Builder
    public static class IncompleteTeam {
        private Long teamId;
        private String teamName;
        private int currentMembers;
        private int requiredMembers;
        private List<JobRole> missingRoles;
    }

    @Getter
    @Builder
    public static class ProjectStats {
        private int totalProjects;
        private int submittedProjects;
        private int inProgressProjects;
        private double submissionRate;
        private int checkpointMissingCount;
    }

    @Getter
    @Builder
    public static class ReviewStats {
        private int totalReviews;
        private int completedReviews;
        private int pendingReviews;
        private double reviewCompletionRate;
        private double averageScore;
        private ScoreDistribution scoreDistribution;
    }

    @Getter
    @Builder
    public static class ScoreDistribution {
        private int excellent;  // 4.5 이상
        private int good;       // 3.5 ~ 4.5
        private int average;    // 2.5 ~ 3.5
        private int belowAverage; // 2.5 미만
    }

    @Getter
    @Builder
    public static class ReportStats {
        private int totalReports;
        private int generatedReports;
        private int pendingReports;
        private double generationRate;
    }

    @Getter
    @Builder
    public static class RiskTeam {
        private Long teamId;
        private String teamName;
        private String projectName;
        private String riskType;  // INCOMPLETE_TEAM, CHECKPOINT_MISSING, SUBMISSION_DELAYED
        private String riskDescription;
        private int riskLevel;  // 1: 낮음, 2: 보통, 3: 높음
    }
}
