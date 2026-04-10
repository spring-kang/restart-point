package com.restartpoint.domain.report.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.restartpoint.domain.profile.entity.JobRole;
import com.restartpoint.domain.profile.entity.Profile;
import com.restartpoint.domain.profile.repository.ProfileRepository;
import com.restartpoint.domain.project.entity.Project;
import com.restartpoint.domain.project.entity.ProjectStatus;
import com.restartpoint.domain.project.repository.ProjectRepository;
import com.restartpoint.domain.report.dto.GrowthReportResponse;
import com.restartpoint.domain.report.entity.GrowthReport;
import com.restartpoint.domain.report.entity.ReportType;
import com.restartpoint.domain.report.repository.GrowthReportRepository;
import com.restartpoint.domain.review.dto.ReviewSummaryResponse;
import com.restartpoint.domain.review.entity.RubricItem;
import com.restartpoint.domain.review.service.ReviewService;
import com.restartpoint.domain.season.entity.Season;
import com.restartpoint.domain.season.entity.SeasonStatus;
import com.restartpoint.domain.team.entity.Team;
import com.restartpoint.domain.team.entity.TeamMember;
import com.restartpoint.domain.team.entity.TeamMemberStatus;
import com.restartpoint.domain.team.repository.TeamMemberRepository;
import com.restartpoint.domain.user.entity.User;
import com.restartpoint.domain.user.repository.UserRepository;
import com.restartpoint.global.exception.BusinessException;
import com.restartpoint.global.exception.ErrorCode;
import com.restartpoint.infra.ai.AiGrowthReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class GrowthReportService {

    private final GrowthReportRepository growthReportRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final ProfileRepository profileRepository;
    private final ReviewService reviewService;
    private final AiGrowthReportService aiGrowthReportService;
    private final ObjectMapper objectMapper;

    /**
     * 프로젝트의 팀 리포트 조회
     */
    public GrowthReportResponse getTeamReport(Long userId, Long projectId) {
        Project project = findProjectById(projectId);
        validateAccess(userId, project);

        GrowthReport report = growthReportRepository.findTeamReportByProjectId(projectId)
                .orElseThrow(() -> new BusinessException(ErrorCode.REPORT_NOT_FOUND));

        return GrowthReportResponse.from(report);
    }

    /**
     * 프로젝트의 개인 리포트 조회
     */
    public GrowthReportResponse getIndividualReport(Long userId, Long projectId) {
        Project project = findProjectById(projectId);
        validateAccess(userId, project);

        GrowthReport report = growthReportRepository.findIndividualReport(projectId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.REPORT_NOT_FOUND));

        return GrowthReportResponse.from(report);
    }

    /**
     * 사용자의 모든 리포트 목록 조회
     */
    public List<GrowthReportResponse> getMyReports(Long userId) {
        return growthReportRepository.findAllByUserId(userId).stream()
                .map(GrowthReportResponse::simpleFrom)
                .toList();
    }

    /**
     * 프로젝트의 리포트 조회 (팀 리포트 + 본인 개인 리포트만)
     */
    public List<GrowthReportResponse> getProjectReports(Long userId, Long projectId) {
        Project project = findProjectById(projectId);
        validateAccess(userId, project);

        List<GrowthReportResponse> result = new ArrayList<>();

        // 팀 리포트 추가
        growthReportRepository.findTeamReportByProjectId(projectId)
                .ifPresent(report -> result.add(GrowthReportResponse.from(report)));

        // 본인 개인 리포트만 추가 (타인 리포트는 제외)
        growthReportRepository.findIndividualReport(projectId, userId)
                .ifPresent(report -> result.add(GrowthReportResponse.from(report)));

        return result;
    }

    /**
     * 프로젝트의 성장 리포트 생성 (시즌 종료 시 호출)
     */
    @Transactional
    public void generateReportsForProject(Long projectId) {
        Project project = findProjectById(projectId);

        // 이미 리포트가 생성되었는지 확인
        if (growthReportRepository.existsByProjectIdAndReportType(projectId, ReportType.TEAM)) {
            log.info("이미 리포트가 생성된 프로젝트: {}", projectId);
            return;
        }

        // 제출된 프로젝트만 리포트 생성
        if (project.getStatus() != ProjectStatus.SUBMITTED && project.getStatus() != ProjectStatus.COMPLETED) {
            log.info("제출되지 않은 프로젝트는 리포트 생성 제외: {}", projectId);
            return;
        }

        // 심사 요약 조회
        ReviewSummaryResponse reviewSummary = getReviewSummaryForReport(project);
        if (reviewSummary == null) {
            log.warn("심사 결과가 없어 리포트 생성 불가: {}", projectId);
            return;
        }

        // 팀 리포트 생성
        createTeamReport(project, reviewSummary);

        // 팀원별 개인 리포트 생성
        createIndividualReports(project, reviewSummary);

        log.info("성장 리포트 생성 완료 - 프로젝트: {}", project.getName());
    }

    /**
     * 시즌의 모든 프로젝트 리포트 생성
     */
    @Async
    @Transactional
    public void generateReportsForSeason(Long seasonId) {
        List<Project> projects = projectRepository.findBySeasonIdAndStatus(
                seasonId, ProjectStatus.SUBMITTED, null).getContent();

        log.info("시즌 {} 리포트 생성 시작 - 대상 프로젝트 수: {}", seasonId, projects.size());

        for (Project project : projects) {
            try {
                generateReportsForProject(project.getId());
            } catch (Exception e) {
                log.error("프로젝트 {} 리포트 생성 실패: {}", project.getId(), e.getMessage());
            }
        }

        log.info("시즌 {} 리포트 생성 완료", seasonId);
    }

    /**
     * 리포트 재생성 (AI 생성 실패한 경우)
     */
    @Transactional
    public GrowthReportResponse regenerateReport(Long userId, Long reportId) {
        GrowthReport report = findReportById(reportId);
        validateReportAccess(userId, report);

        Project project = report.getProject();
        ReviewSummaryResponse reviewSummary = getReviewSummaryForReport(project);

        if (reviewSummary == null) {
            throw new BusinessException(ErrorCode.REPORT_GENERATION_FAILED, "심사 결과가 없습니다.");
        }

        if (report.getReportType() == ReportType.TEAM) {
            generateTeamReportContent(report, project, reviewSummary);
        } else {
            User user = report.getUser();
            JobRole jobRole = getJobRole(user.getId(), project.getTeam());
            generateIndividualReportContent(report, project, reviewSummary, user, jobRole);
        }

        return GrowthReportResponse.from(report);
    }

    // === Private 메서드 ===

    private void createTeamReport(Project project, ReviewSummaryResponse reviewSummary) {
        GrowthReport teamReport = GrowthReport.builder()
                .project(project)
                .reportType(ReportType.TEAM)
                .build();

        // 점수 요약 저장
        teamReport.setScoreSummary(
                reviewSummary.getWeightedAverageScore(),
                serializeRubricScores(reviewSummary)
        );

        growthReportRepository.save(teamReport);

        // AI 리포트 생성
        generateTeamReportContent(teamReport, project, reviewSummary);
    }

    private void createIndividualReports(Project project, ReviewSummaryResponse reviewSummary) {
        Team team = project.getTeam();

        // 팀 리더 리포트 생성
        User leader = team.getLeader();
        JobRole leaderRole = getJobRole(leader.getId(), team);
        createIndividualReport(project, reviewSummary, leader, leaderRole);

        // 팀원 리포트 생성
        List<TeamMember> members = teamMemberRepository.findByTeamId(team.getId()).stream()
                .filter(m -> m.getStatus() == TeamMemberStatus.ACCEPTED)
                .toList();

        for (TeamMember member : members) {
            createIndividualReport(project, reviewSummary, member.getUser(), member.getRole());
        }
    }

    private void createIndividualReport(Project project, ReviewSummaryResponse reviewSummary,
                                         User user, JobRole jobRole) {
        GrowthReport individualReport = GrowthReport.builder()
                .project(project)
                .user(user)
                .reportType(ReportType.INDIVIDUAL)
                .build();

        individualReport.setScoreSummary(
                reviewSummary.getWeightedAverageScore(),
                serializeRubricScores(reviewSummary)
        );

        growthReportRepository.save(individualReport);

        // AI 리포트 생성
        generateIndividualReportContent(individualReport, project, reviewSummary, user, jobRole);
    }

    private void generateTeamReportContent(GrowthReport report, Project project, ReviewSummaryResponse reviewSummary) {
        try {
            Map<String, String> aiReport = aiGrowthReportService.generateTeamReport(project, reviewSummary);

            if (aiReport != null) {
                report.updateContent(
                        aiReport.get("teamStrengths"),
                        aiReport.get("teamImprovements"),
                        null,
                        aiReport.get("nextProjectActions"),
                        null,
                        aiReport.get("recommendedAreas")
                );
                log.info("팀 리포트 AI 생성 완료 - 프로젝트: {}", project.getName());
            } else {
                log.warn("팀 리포트 AI 생성 실패 - 프로젝트: {}", project.getName());
            }
        } catch (Exception e) {
            log.error("팀 리포트 생성 중 오류: {}", e.getMessage());
        }
    }

    private void generateIndividualReportContent(GrowthReport report, Project project,
                                                  ReviewSummaryResponse reviewSummary,
                                                  User user, JobRole jobRole) {
        try {
            Map<String, String> aiReport = aiGrowthReportService.generateIndividualReport(
                    project, reviewSummary, user.getId(), user.getName(), jobRole);

            if (aiReport != null) {
                report.updateContent(
                        null,
                        null,
                        aiReport.get("roleSpecificFeedback"),
                        aiReport.get("nextProjectActions"),
                        aiReport.get("portfolioImprovements"),
                        aiReport.get("recommendedAreas")
                );
                log.info("개인 리포트 AI 생성 완료 - 사용자: {}", user.getName());
            } else {
                log.warn("개인 리포트 AI 생성 실패 - 사용자: {}", user.getName());
            }
        } catch (Exception e) {
            log.error("개인 리포트 생성 중 오류: {}", e.getMessage());
        }
    }

    private ReviewSummaryResponse getReviewSummaryForReport(Project project) {
        try {
            // 운영자 권한으로 심사 요약 조회 (내부 호출)
            return reviewService.getReviewSummaryInternal(project.getId());
        } catch (Exception e) {
            log.error("심사 요약 조회 실패: {}", e.getMessage());
            return null;
        }
    }

    private JobRole getJobRole(Long userId, Team team) {
        // 리더인 경우 프로필에서 조회
        if (team.getLeader().getId().equals(userId)) {
            return profileRepository.findByUserId(userId)
                    .map(Profile::getJobRole)
                    .orElse(JobRole.PLANNER);
        }

        // 팀원인 경우 TeamMember에서 조회
        return teamMemberRepository.findByTeamId(team.getId()).stream()
                .filter(m -> m.getUser().getId().equals(userId))
                .findFirst()
                .map(TeamMember::getRole)
                .orElse(JobRole.PLANNER);
    }

    private String serializeRubricScores(ReviewSummaryResponse reviewSummary) {
        try {
            return objectMapper.writeValueAsString(reviewSummary.getRubricAverages());
        } catch (JsonProcessingException e) {
            log.error("루브릭 점수 직렬화 실패: {}", e.getMessage());
            return null;
        }
    }

    private void validateAccess(Long userId, Project project) {
        User user = findUserById(userId);

        // 관리자는 항상 접근 가능
        if (user.getRole() == com.restartpoint.domain.user.entity.Role.ADMIN) {
            return;
        }

        // 팀원인지 확인
        if (!isTeamMember(userId, project)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
    }

    /**
     * 리포트 접근 권한 검증 (개인 리포트는 본인만 접근 가능)
     */
    private void validateReportAccess(Long userId, GrowthReport report) {
        User user = findUserById(userId);

        // 관리자는 항상 접근 가능
        if (user.getRole() == com.restartpoint.domain.user.entity.Role.ADMIN) {
            return;
        }

        // 팀원인지 먼저 확인
        if (!isTeamMember(userId, report.getProject())) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        // 개인 리포트는 본인만 접근 가능
        if (report.getReportType() == ReportType.INDIVIDUAL) {
            if (report.getUser() == null || !report.getUser().getId().equals(userId)) {
                throw new BusinessException(ErrorCode.ACCESS_DENIED, "개인 리포트는 본인만 재생성할 수 있습니다.");
            }
        }
    }

    private boolean isTeamMember(Long userId, Project project) {
        Team team = project.getTeam();

        // 리더인지 확인
        if (team.getLeader().getId().equals(userId)) {
            return true;
        }

        // 팀원인지 확인
        return teamMemberRepository.findByTeamId(team.getId()).stream()
                .anyMatch(m -> m.getUser().getId().equals(userId)
                        && m.getStatus() == TeamMemberStatus.ACCEPTED);
    }

    private Project findProjectById(Long projectId) {
        return projectRepository.findByIdWithTeam(projectId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROJECT_NOT_FOUND));
    }

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    private GrowthReport findReportById(Long reportId) {
        return growthReportRepository.findById(reportId)
                .orElseThrow(() -> new BusinessException(ErrorCode.REPORT_NOT_FOUND));
    }
}
