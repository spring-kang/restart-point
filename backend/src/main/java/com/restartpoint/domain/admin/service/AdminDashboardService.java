package com.restartpoint.domain.admin.service;

import com.restartpoint.domain.admin.dto.SeasonDashboardResponse;
import com.restartpoint.domain.admin.dto.SeasonDashboardResponse.*;
import com.restartpoint.domain.profile.entity.JobRole;
import com.restartpoint.domain.project.entity.Project;
import com.restartpoint.domain.project.entity.ProjectStatus;
import com.restartpoint.domain.project.repository.ProjectRepository;
import com.restartpoint.domain.report.entity.GrowthReport;
import com.restartpoint.domain.report.repository.GrowthReportRepository;
import com.restartpoint.domain.review.entity.Review;
import com.restartpoint.domain.review.repository.ReviewRepository;
import com.restartpoint.domain.season.entity.Season;
import com.restartpoint.domain.season.repository.SeasonRepository;
import com.restartpoint.domain.team.entity.Team;
import com.restartpoint.domain.team.entity.TeamMember;
import com.restartpoint.domain.team.entity.TeamMemberStatus;
import com.restartpoint.domain.team.entity.TeamStatus;
import com.restartpoint.domain.team.repository.TeamMemberRepository;
import com.restartpoint.domain.team.repository.TeamRepository;
import com.restartpoint.domain.user.entity.CertificationStatus;
import com.restartpoint.domain.user.repository.UserRepository;
import com.restartpoint.global.exception.BusinessException;
import com.restartpoint.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 운영자 대시보드 서비스
 * 시즌별 현황 통계 제공
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class AdminDashboardService {

    private final SeasonRepository seasonRepository;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final ProjectRepository projectRepository;
    private final ReviewRepository reviewRepository;
    private final GrowthReportRepository growthReportRepository;
    private final UserRepository userRepository;

    private static final int REQUIRED_TEAM_SIZE = 4;

    /**
     * 시즌별 대시보드 조회
     */
    public SeasonDashboardResponse getSeasonDashboard(Long seasonId) {
        Season season = seasonRepository.findById(seasonId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SEASON_NOT_FOUND));

        List<Team> teams = teamRepository.findBySeason(season);
        List<Project> projects = projectRepository.findAllBySeasonId(seasonId);
        List<Review> reviews = reviewRepository.findAllBySeasonId(seasonId);
        List<GrowthReport> reports = growthReportRepository.findAllBySeasonId(seasonId);

        return SeasonDashboardResponse.builder()
                .seasonId(seasonId)
                .seasonTitle(season.getTitle())
                .seasonStatus(season.getStatus().name())
                .participantStats(buildParticipantStats(teams))
                .teamStats(buildTeamStats(teams))
                .projectStats(buildProjectStats(projects))
                .reviewStats(buildReviewStats(reviews, projects.size()))
                .reportStats(buildReportStats(reports, teams.size()))
                .riskTeams(buildRiskTeams(teams, projects))
                .build();
    }

    /**
     * 전체 대시보드 조회 (모든 활성 시즌)
     */
    public Map<String, Object> getOverallDashboard() {
        long pendingCertifications = userRepository.countByCertificationStatus(CertificationStatus.PENDING);

        List<Season> activeSeasons = seasonRepository.findByStatusNot(
                com.restartpoint.domain.season.entity.SeasonStatus.COMPLETED);

        Map<String, Object> result = new HashMap<>();
        result.put("pendingCertifications", pendingCertifications);
        result.put("activeSeasonCount", activeSeasons.size());
        result.put("activeSeasons", activeSeasons.stream()
                .map(s -> Map.of(
                        "id", s.getId(),
                        "title", s.getTitle(),
                        "status", s.getStatus().name()
                ))
                .toList());

        return result;
    }

    // === Private 헬퍼 메서드 ===

    private ParticipantStats buildParticipantStats(List<Team> teams) {
        Set<Long> participantIds = new HashSet<>();
        Map<JobRole, Integer> roleDistribution = new EnumMap<>(JobRole.class);

        // 초기화
        for (JobRole role : JobRole.values()) {
            roleDistribution.put(role, 0);
        }

        for (Team team : teams) {
            // 리더 추가
            participantIds.add(team.getLeader().getId());

            // 팀원 추가
            List<TeamMember> members = teamMemberRepository.findByTeamId(team.getId());
            for (TeamMember member : members) {
                if (member.getStatus() == TeamMemberStatus.ACCEPTED) {
                    participantIds.add(member.getUser().getId());
                    roleDistribution.merge(member.getRole(), 1, Integer::sum);
                }
            }
        }

        long pendingCount = userRepository.countByCertificationStatus(CertificationStatus.PENDING);

        return ParticipantStats.builder()
                .totalParticipants(participantIds.size())
                .certifiedParticipants(participantIds.size()) // 시즌 참가자는 인증 완료자
                .pendingCertifications((int) pendingCount)
                .roleDistribution(roleDistribution)
                .build();
    }

    private TeamStats buildTeamStats(List<Team> teams) {
        int completeTeams = 0;
        int incompleteTeams = 0;
        int recruitingTeams = 0;
        List<IncompleteTeam> incompleteTeamList = new ArrayList<>();

        for (Team team : teams) {
            List<TeamMember> members = teamMemberRepository.findByTeamId(team.getId());
            int acceptedCount = (int) members.stream()
                    .filter(m -> m.getStatus() == TeamMemberStatus.ACCEPTED)
                    .count() + 1; // +1 for leader

            if (team.getStatus() == TeamStatus.RECRUITING) {
                recruitingTeams++;
            }

            if (acceptedCount >= REQUIRED_TEAM_SIZE) {
                completeTeams++;
            } else {
                incompleteTeams++;

                // 부족한 역할 계산
                Set<JobRole> filledRoles = members.stream()
                        .filter(m -> m.getStatus() == TeamMemberStatus.ACCEPTED)
                        .map(TeamMember::getRole)
                        .collect(Collectors.toSet());

                List<JobRole> missingRoles = Arrays.stream(JobRole.values())
                        .filter(role -> !filledRoles.contains(role))
                        .toList();

                incompleteTeamList.add(IncompleteTeam.builder()
                        .teamId(team.getId())
                        .teamName(team.getName())
                        .currentMembers(acceptedCount)
                        .requiredMembers(REQUIRED_TEAM_SIZE)
                        .missingRoles(missingRoles)
                        .build());
            }
        }

        return TeamStats.builder()
                .totalTeams(teams.size())
                .completeTeams(completeTeams)
                .incompleteTeams(incompleteTeams)
                .recruitingTeams(recruitingTeams)
                .incompleteTeamList(incompleteTeamList)
                .build();
    }

    private ProjectStats buildProjectStats(List<Project> projects) {
        int submitted = 0;
        int inProgress = 0;
        int checkpointMissing = 0;

        for (Project project : projects) {
            if (project.getStatus() == ProjectStatus.SUBMITTED ||
                project.getStatus() == ProjectStatus.COMPLETED) {
                submitted++;
            } else if (project.getStatus() == ProjectStatus.IN_PROGRESS) {
                inProgress++;
            }

            // 체크포인트 미제출 확인 (checkpoints가 비어있는 경우)
            if (project.getCheckpoints() == null || project.getCheckpoints().isEmpty()) {
                checkpointMissing++;
            }
        }

        double submissionRate = projects.isEmpty() ? 0 :
                (submitted * 100.0) / projects.size();

        return ProjectStats.builder()
                .totalProjects(projects.size())
                .submittedProjects(submitted)
                .inProgressProjects(inProgress)
                .submissionRate(Math.round(submissionRate * 10) / 10.0)
                .checkpointMissingCount(checkpointMissing)
                .build();
    }

    private ReviewStats buildReviewStats(List<Review> reviews, int projectCount) {
        if (reviews.isEmpty()) {
            return ReviewStats.builder()
                    .totalReviews(0)
                    .completedReviews(0)
                    .pendingReviews(0)
                    .reviewCompletionRate(0)
                    .averageScore(0)
                    .scoreDistribution(ScoreDistribution.builder()
                            .excellent(0).good(0).average(0).belowAverage(0).build())
                    .build();
        }

        double totalScore = 0;
        int excellent = 0, good = 0, average = 0, belowAverage = 0;

        for (Review review : reviews) {
            double avgScore = review.calculateAverageScore();
            totalScore += avgScore;

            if (avgScore >= 4.5) excellent++;
            else if (avgScore >= 3.5) good++;
            else if (avgScore >= 2.5) average++;
            else belowAverage++;
        }

        double avgScore = totalScore / reviews.size();

        return ReviewStats.builder()
                .totalReviews(reviews.size())
                .completedReviews(reviews.size())
                .pendingReviews(0) // 현재는 완료된 리뷰만 조회
                .reviewCompletionRate(100)
                .averageScore(Math.round(avgScore * 100) / 100.0)
                .scoreDistribution(ScoreDistribution.builder()
                        .excellent(excellent)
                        .good(good)
                        .average(average)
                        .belowAverage(belowAverage)
                        .build())
                .build();
    }

    private ReportStats buildReportStats(List<GrowthReport> reports, int teamCount) {
        int generated = (int) reports.stream().filter(GrowthReport::isGenerated).count();
        int pending = reports.size() - generated;

        double generationRate = reports.isEmpty() ? 0 :
                (generated * 100.0) / reports.size();

        return ReportStats.builder()
                .totalReports(reports.size())
                .generatedReports(generated)
                .pendingReports(pending)
                .generationRate(Math.round(generationRate * 10) / 10.0)
                .build();
    }

    private List<RiskTeam> buildRiskTeams(List<Team> teams, List<Project> projects) {
        List<RiskTeam> riskTeams = new ArrayList<>();
        Map<Long, Project> teamProjectMap = projects.stream()
                .collect(Collectors.toMap(p -> p.getTeam().getId(), p -> p, (a, b) -> a));

        for (Team team : teams) {
            Project project = teamProjectMap.get(team.getId());

            // 1. 팀원 수 부족
            List<TeamMember> members = teamMemberRepository.findByTeamId(team.getId());
            int memberCount = (int) members.stream()
                    .filter(m -> m.getStatus() == TeamMemberStatus.ACCEPTED)
                    .count() + 1;

            if (memberCount < REQUIRED_TEAM_SIZE) {
                riskTeams.add(RiskTeam.builder()
                        .teamId(team.getId())
                        .teamName(team.getName())
                        .projectName(project != null ? project.getName() : "-")
                        .riskType("INCOMPLETE_TEAM")
                        .riskDescription(String.format("팀원 부족 (%d/%d명)", memberCount, REQUIRED_TEAM_SIZE))
                        .riskLevel(3)
                        .build());
            }

            if (project != null) {
                // 2. 체크포인트 미제출 위험
                if (project.getCheckpoints() == null || project.getCheckpoints().isEmpty()) {
                    riskTeams.add(RiskTeam.builder()
                            .teamId(team.getId())
                            .teamName(team.getName())
                            .projectName(project.getName())
                            .riskType("CHECKPOINT_MISSING")
                            .riskDescription("체크포인트 미제출")
                            .riskLevel(2)
                            .build());
                }

                // 3. 제출 지연 위험 (IN_PROGRESS 상태이면서 마감이 임박한 경우)
                // 실제로는 시즌 마감일과 비교해야 함
                if (project.getStatus() == ProjectStatus.IN_PROGRESS) {
                    riskTeams.add(RiskTeam.builder()
                            .teamId(team.getId())
                            .teamName(team.getName())
                            .projectName(project.getName())
                            .riskType("SUBMISSION_DELAYED")
                            .riskDescription("프로젝트 미제출")
                            .riskLevel(2)
                            .build());
                }
            }
        }

        // 위험도 높은 순으로 정렬
        riskTeams.sort((a, b) -> Integer.compare(b.getRiskLevel(), a.getRiskLevel()));

        return riskTeams;
    }
}
