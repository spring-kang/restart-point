package com.restartpoint.domain.project.service;

import com.restartpoint.domain.profile.entity.JobRole;
import com.restartpoint.domain.project.dto.*;
import com.restartpoint.domain.project.entity.*;
import com.restartpoint.domain.project.repository.CheckpointRepository;
import com.restartpoint.domain.project.repository.MemberProgressRepository;
import com.restartpoint.domain.project.repository.ProjectRepository;
import com.restartpoint.domain.season.entity.SeasonStatus;
import com.restartpoint.domain.team.entity.Team;
import com.restartpoint.domain.team.entity.TeamMember;
import com.restartpoint.domain.team.entity.TeamMemberStatus;
import com.restartpoint.domain.team.repository.TeamMemberRepository;
import com.restartpoint.domain.team.repository.TeamRepository;
import com.restartpoint.domain.user.entity.User;
import com.restartpoint.domain.user.repository.UserRepository;
import com.restartpoint.global.exception.BusinessException;
import com.restartpoint.global.exception.ErrorCode;
import com.restartpoint.infra.ai.AiProjectCoachService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final CheckpointRepository checkpointRepository;
    private final MemberProgressRepository memberProgressRepository;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final UserRepository userRepository;
    private final AiProjectCoachService aiProjectCoachService;

    // 프로젝트 생성 (팀 리더만 가능)
    @Transactional
    public ProjectResponse createProject(Long userId, ProjectCreateRequest request) {
        Team team = findTeamById(request.getTeamId());
        validateTeamLeader(team, userId);

        // 이미 프로젝트가 있는지 확인
        if (projectRepository.existsByTeamId(team.getId())) {
            throw new BusinessException(ErrorCode.PROJECT_ALREADY_EXISTS);
        }

        // 시즌이 프로젝트 진행 기간인지 확인
        if (team.getSeason().getStatus() != SeasonStatus.IN_PROGRESS) {
            throw new BusinessException(ErrorCode.SEASON_NOT_IN_PROGRESS);
        }

        Project project = Project.builder()
                .team(team)
                .name(request.getName())
                .problemDefinition(request.getProblemDefinition())
                .targetUsers(request.getTargetUsers())
                .solution(request.getSolution())
                .aiUsage(request.getAiUsage())
                .figmaUrl(request.getFigmaUrl())
                .githubUrl(request.getGithubUrl())
                .notionUrl(request.getNotionUrl())
                .demoUrl(request.getDemoUrl())
                .build();

        Project savedProject = projectRepository.save(project);
        return ProjectResponse.from(savedProject);
    }

    // 프로젝트 상세 조회 (팀원만 가능)
    public ProjectResponse getProject(Long userId, Long projectId) {
        Project project = findProjectByIdWithTeam(projectId);
        validateTeamMember(project.getTeam(), userId);
        return ProjectResponse.from(project);
    }

    // 팀의 프로젝트 조회 (팀원만 가능)
    public ProjectResponse getProjectByTeam(Long userId, Long teamId) {
        Project project = projectRepository.findByTeamId(teamId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROJECT_NOT_FOUND));
        Team team = findTeamById(teamId);
        validateTeamMember(team, userId);
        return ProjectResponse.from(project);
    }

    // 시즌별 프로젝트 목록 조회
    public Page<ProjectResponse> getProjectsBySeason(Long seasonId, Pageable pageable) {
        return projectRepository.findBySeasonId(seasonId, pageable)
                .map(ProjectResponse::simpleFrom);
    }

    // 시즌별 프로젝트 목록 조회 (상태 필터)
    public Page<ProjectResponse> getProjectsBySeasonAndStatus(Long seasonId, ProjectStatus status, Pageable pageable) {
        return projectRepository.findBySeasonIdAndStatus(seasonId, status, pageable)
                .map(ProjectResponse::simpleFrom);
    }

    // 프로젝트 수정 (팀원만 가능)
    @Transactional
    public ProjectResponse updateProject(Long userId, Long projectId, ProjectUpdateRequest request) {
        Project project = findProjectByIdWithTeam(projectId);
        validateTeamMember(project.getTeam(), userId);

        // 제출 완료 후에는 수정 불가
        if (project.getStatus() == ProjectStatus.SUBMITTED || project.getStatus() == ProjectStatus.COMPLETED) {
            throw new BusinessException(ErrorCode.PROJECT_ALREADY_SUBMITTED);
        }

        project.update(
                request.getName(),
                request.getProblemDefinition(),
                request.getTargetUsers(),
                request.getSolution(),
                request.getAiUsage(),
                request.getFigmaUrl(),
                request.getGithubUrl(),
                request.getNotionUrl(),
                request.getDemoUrl()
        );

        return ProjectResponse.from(project);
    }

    // 프로젝트 시작 (DRAFT -> IN_PROGRESS)
    @Transactional
    public ProjectResponse startProject(Long userId, Long projectId) {
        Project project = findProjectByIdWithTeam(projectId);
        validateTeamLeader(project.getTeam(), userId);

        if (project.getStatus() != ProjectStatus.DRAFT) {
            throw new BusinessException(ErrorCode.INVALID_PROJECT_STATUS);
        }

        project.startProject();
        return ProjectResponse.from(project);
    }

    // 프로젝트 제출
    @Transactional
    public ProjectResponse submitProject(Long userId, Long projectId, ProjectSubmitRequest request) {
        Project project = findProjectByIdWithTeam(projectId);
        validateTeamLeader(project.getTeam(), userId);

        // 제출 마감 전인지 확인
        if (project.getTeam().getSeason().getStatus() != SeasonStatus.IN_PROGRESS) {
            throw new BusinessException(ErrorCode.SUBMISSION_DEADLINE_PASSED);
        }

        // 프로젝트가 진행 중 상태인지 확인 (DRAFT에서는 제출 불가)
        if (project.getStatus() != ProjectStatus.IN_PROGRESS) {
            if (project.getStatus() == ProjectStatus.SUBMITTED || project.getStatus() == ProjectStatus.COMPLETED) {
                throw new BusinessException(ErrorCode.PROJECT_ALREADY_SUBMITTED);
            }
            throw new BusinessException(ErrorCode.INVALID_PROJECT_STATUS, "프로젝트를 먼저 시작해야 합니다.");
        }

        // 필수 항목 확인
        validateProjectForSubmission(project);

        project.submit(request.getTeamRetrospective());
        return ProjectResponse.from(project);
    }

    // 체크포인트 생성
    @Transactional
    public CheckpointResponse createCheckpoint(Long userId, Long projectId, CheckpointCreateRequest request) {
        Project project = findProjectByIdWithTeam(projectId);
        User user = findUserById(userId);
        validateTeamMember(project.getTeam(), userId);

        // 프로젝트가 진행 중인지 확인 (DRAFT에서는 체크포인트 생성 불가)
        if (project.getStatus() != ProjectStatus.IN_PROGRESS) {
            throw new BusinessException(ErrorCode.INVALID_PROJECT_STATUS, "프로젝트가 진행 중일 때만 체크포인트를 생성할 수 있습니다.");
        }

        // 같은 주차에 이미 체크포인트가 있는지 확인
        if (checkpointRepository.existsByProjectIdAndWeekNumber(projectId, request.getWeekNumber())) {
            throw new BusinessException(ErrorCode.CHECKPOINT_ALREADY_EXISTS);
        }

        Checkpoint checkpoint = Checkpoint.builder()
                .project(project)
                .weekNumber(request.getWeekNumber())
                .weeklyGoal(request.getWeeklyGoal())
                .progressSummary(request.getProgressSummary())
                .blockers(request.getBlockers())
                .nextWeekPlan(request.getNextWeekPlan())
                .createdBy(user)
                .build();

        Checkpoint savedCheckpoint = checkpointRepository.save(checkpoint);
        project.addCheckpoint(savedCheckpoint);

        // 역할별 진행 상황 저장
        if (request.getMemberProgresses() != null) {
            saveMemberProgresses(savedCheckpoint, request.getMemberProgresses(), project.getTeam());
        }

        // AI 프로젝트 코칭 피드백 생성
        generateAiFeedback(savedCheckpoint);

        return CheckpointResponse.from(savedCheckpoint);
    }

    // 체크포인트 수정
    @Transactional
    public CheckpointResponse updateCheckpoint(Long userId, Long checkpointId, CheckpointUpdateRequest request) {
        Checkpoint checkpoint = findCheckpointByIdWithProject(checkpointId);
        validateTeamMember(checkpoint.getProject().getTeam(), userId);

        // 프로젝트가 제출 완료 상태가 아닌지 확인
        Project project = checkpoint.getProject();
        if (project.getStatus() == ProjectStatus.SUBMITTED || project.getStatus() == ProjectStatus.COMPLETED) {
            throw new BusinessException(ErrorCode.PROJECT_ALREADY_SUBMITTED);
        }

        checkpoint.update(
                request.getWeeklyGoal(),
                request.getProgressSummary(),
                request.getBlockers(),
                request.getNextWeekPlan()
        );

        // 역할별 진행 상황 업데이트
        if (request.getMemberProgresses() != null) {
            // 기존 진행 상황 삭제 후 새로 저장
            memberProgressRepository.deleteByCheckpointId(checkpointId);
            saveMemberProgresses(checkpoint, request.getMemberProgresses(), project.getTeam());
        }

        // AI 프로젝트 코칭 피드백 재생성
        generateAiFeedback(checkpoint);

        return CheckpointResponse.from(checkpoint);
    }

    // AI 피드백 수동 재생성
    @Transactional
    public CheckpointResponse regenerateAiFeedback(Long userId, Long checkpointId) {
        Checkpoint checkpoint = findCheckpointByIdWithProject(checkpointId);
        validateTeamMember(checkpoint.getProject().getTeam(), userId);

        generateAiFeedback(checkpoint);

        return CheckpointResponse.from(checkpoint);
    }

    // 체크포인트 조회 (팀원만 가능)
    public CheckpointResponse getCheckpoint(Long userId, Long checkpointId) {
        Checkpoint checkpoint = findCheckpointByIdWithProject(checkpointId);
        validateTeamMember(checkpoint.getProject().getTeam(), userId);
        return CheckpointResponse.from(checkpoint);
    }

    // 프로젝트의 체크포인트 목록 조회 (팀원만 가능)
    public List<CheckpointResponse> getCheckpointsByProject(Long userId, Long projectId) {
        Project project = findProjectByIdWithTeam(projectId);
        validateTeamMember(project.getTeam(), userId);
        return checkpointRepository.findByProjectIdOrderByWeekNumberAsc(projectId).stream()
                .map(CheckpointResponse::from)
                .toList();
    }

    // 체크포인트 삭제
    @Transactional
    public void deleteCheckpoint(Long userId, Long checkpointId) {
        Checkpoint checkpoint = findCheckpointByIdWithProject(checkpointId);
        validateTeamLeader(checkpoint.getProject().getTeam(), userId);

        // 프로젝트가 제출 완료 상태가 아닌지 확인
        Project project = checkpoint.getProject();
        if (project.getStatus() == ProjectStatus.SUBMITTED || project.getStatus() == ProjectStatus.COMPLETED) {
            throw new BusinessException(ErrorCode.PROJECT_ALREADY_SUBMITTED);
        }

        checkpointRepository.delete(checkpoint);
    }

    // 헬퍼 메서드들
    private void generateAiFeedback(Checkpoint checkpoint) {
        try {
            String feedback = aiProjectCoachService.generateFeedback(checkpoint);
            checkpoint.setAiFeedback(feedback);
            log.info("AI 피드백 생성 완료 - 체크포인트 ID: {}", checkpoint.getId());
        } catch (Exception e) {
            log.error("AI 피드백 생성 실패 - 체크포인트 ID: {}, 오류: {}", checkpoint.getId(), e.getMessage());
            checkpoint.setAiFeedback("AI 피드백 생성 중 오류가 발생했습니다. 나중에 다시 시도해주세요.");
        }
    }

    private void saveMemberProgresses(Checkpoint checkpoint, List<MemberProgressRequest> requests, Team team) {
        for (MemberProgressRequest progressRequest : requests) {
            User user = findUserById(progressRequest.getUserId());

            // 해당 사용자가 팀원인지 검증
            validateTeamMember(team, user.getId());

            MemberProgress progress = MemberProgress.builder()
                    .checkpoint(checkpoint)
                    .user(user)
                    .jobRole(progressRequest.getJobRole())
                    .completedTasks(progressRequest.getCompletedTasks())
                    .inProgressTasks(progressRequest.getInProgressTasks())
                    .personalBlockers(progressRequest.getPersonalBlockers())
                    .contributionPercentage(progressRequest.getContributionPercentage())
                    .build();
            memberProgressRepository.save(progress);
            checkpoint.addMemberProgress(progress);
        }
    }

    private void validateProjectForSubmission(Project project) {
        if (project.getName() == null || project.getName().isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "프로젝트 이름은 필수입니다.");
        }
        if (project.getProblemDefinition() == null || project.getProblemDefinition().isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "문제 정의는 필수입니다.");
        }
        if (project.getTargetUsers() == null || project.getTargetUsers().isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "타깃 사용자는 필수입니다.");
        }
        if (project.getSolution() == null || project.getSolution().isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "핵심 솔루션은 필수입니다.");
        }
    }

    private Project findProjectById(Long projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROJECT_NOT_FOUND));
    }

    private Project findProjectByIdWithTeam(Long projectId) {
        return projectRepository.findByIdWithTeam(projectId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROJECT_NOT_FOUND));
    }

    private Checkpoint findCheckpointById(Long checkpointId) {
        return checkpointRepository.findById(checkpointId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHECKPOINT_NOT_FOUND));
    }

    private Checkpoint findCheckpointByIdWithProject(Long checkpointId) {
        return checkpointRepository.findByIdWithProject(checkpointId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHECKPOINT_NOT_FOUND));
    }

    private Team findTeamById(Long teamId) {
        return teamRepository.findById(teamId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TEAM_NOT_FOUND));
    }

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    private void validateTeamLeader(Team team, Long userId) {
        if (!team.getLeader().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.NOT_TEAM_LEADER);
        }
    }

    private void validateTeamMember(Team team, Long userId) {
        // 리더이거나
        if (team.getLeader().getId().equals(userId)) {
            return;
        }

        // ACCEPTED 상태의 팀원인지 확인
        boolean isMember = teamMemberRepository.findByTeamId(team.getId()).stream()
                .anyMatch(member -> member.getUser().getId().equals(userId)
                        && member.getStatus() == TeamMemberStatus.ACCEPTED);

        if (!isMember) {
            throw new BusinessException(ErrorCode.NOT_TEAM_MEMBER);
        }
    }
}
