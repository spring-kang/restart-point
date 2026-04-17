package com.restartpoint.domain.mentoring.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.restartpoint.domain.mentoring.dto.*;
import com.restartpoint.domain.mentoring.entity.JobRoleMentoring;
import com.restartpoint.domain.mentoring.entity.MentoringModule;
import com.restartpoint.domain.mentoring.entity.MentoringSession;
import com.restartpoint.domain.mentoring.entity.MentoringSession.SessionStatus;
import com.restartpoint.domain.mentoring.repository.JobRoleMentoringRepository;
import com.restartpoint.domain.mentoring.repository.MentoringModuleRepository;
import com.restartpoint.domain.mentoring.repository.MentoringSessionRepository;
import com.restartpoint.domain.payment.entity.Subscription;
import com.restartpoint.domain.payment.repository.SubscriptionRepository;
import com.restartpoint.domain.profile.entity.JobRole;
import com.restartpoint.domain.season.entity.Season;
import com.restartpoint.domain.season.repository.SeasonRepository;
import com.restartpoint.domain.team.entity.TeamMember;
import com.restartpoint.domain.team.repository.TeamMemberRepository;
import com.restartpoint.domain.user.entity.User;
import com.restartpoint.domain.user.repository.UserRepository;
import com.restartpoint.global.exception.BusinessException;
import com.restartpoint.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MentoringService {

    private final JobRoleMentoringRepository mentoringRepository;
    private final MentoringModuleRepository moduleRepository;
    private final MentoringSessionRepository sessionRepository;
    private final SeasonRepository seasonRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final UserRepository userRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final ObjectMapper objectMapper;

    // ========== JobRoleMentoring 관련 ==========

    @Transactional
    public JobRoleMentoringResponse createMentoring(Long seasonId, JobRoleMentoringRequest request) {
        Season season = seasonRepository.findById(seasonId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SEASON_NOT_FOUND));

        // 중복 검사
        if (mentoringRepository.existsBySeasonIdAndJobRole(seasonId, request.getJobRole())) {
            throw new BusinessException(ErrorCode.DUPLICATE_MENTORING);
        }

        JobRoleMentoring mentoring = JobRoleMentoring.builder()
                .season(season)
                .jobRole(request.getJobRole())
                .title(request.getTitle())
                .description(request.getDescription())
                .learningObjectives(request.getLearningObjectives())
                .build();

        JobRoleMentoring saved = mentoringRepository.save(mentoring);
        return JobRoleMentoringResponse.from(saved);
    }

    public List<JobRoleMentoringResponse> getMentoringsBySeasonId(Long seasonId) {
        return mentoringRepository.findBySeasonIdAndActiveTrue(seasonId).stream()
                .map(JobRoleMentoringResponse::from)
                .collect(Collectors.toList());
    }

    public JobRoleMentoringResponse getMentoring(Long mentoringId) {
        JobRoleMentoring mentoring = mentoringRepository.findByIdWithModules(mentoringId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MENTORING_NOT_FOUND));
        return JobRoleMentoringResponse.fromWithModules(mentoring);
    }

    public JobRoleMentoringResponse getMentoringBySeasonAndJobRole(Long seasonId, JobRole jobRole) {
        JobRoleMentoring mentoring = mentoringRepository.findBySeasonIdAndJobRoleWithModules(seasonId, jobRole)
                .orElseThrow(() -> new BusinessException(ErrorCode.MENTORING_NOT_FOUND));
        return JobRoleMentoringResponse.fromWithModules(mentoring);
    }

    @Transactional
    public JobRoleMentoringResponse updateMentoring(Long mentoringId, JobRoleMentoringRequest request) {
        JobRoleMentoring mentoring = mentoringRepository.findById(mentoringId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MENTORING_NOT_FOUND));

        mentoring.update(request.getTitle(), request.getDescription(), request.getLearningObjectives());
        return JobRoleMentoringResponse.from(mentoring);
    }

    @Transactional
    public void deleteMentoring(Long mentoringId) {
        JobRoleMentoring mentoring = mentoringRepository.findById(mentoringId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MENTORING_NOT_FOUND));
        mentoringRepository.delete(mentoring);
    }

    // ========== MentoringModule 관련 ==========

    @Transactional
    public MentoringModuleResponse createModule(Long mentoringId, MentoringModuleRequest request) {
        JobRoleMentoring mentoring = mentoringRepository.findById(mentoringId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MENTORING_NOT_FOUND));

        // 중복 주차 검사
        if (moduleRepository.existsByMentoringIdAndWeekNumber(mentoringId, request.getWeekNumber())) {
            throw new BusinessException(ErrorCode.DUPLICATE_MODULE_WEEK);
        }

        MentoringModule module = MentoringModule.builder()
                .mentoring(mentoring)
                .weekNumber(request.getWeekNumber())
                .title(request.getTitle())
                .description(request.getDescription())
                .learningContent(request.getLearningContent())
                .keyPoints(toJson(request.getKeyPoints()))
                .commonMistakes(toJson(request.getCommonMistakes()))
                .practiceTasks(toJson(request.getPracticeTasks()))
                .referenceMaterials(toJson(request.getReferenceMaterials()))
                .estimatedMinutes(request.getEstimatedMinutes())
                .build();

        mentoring.addModule(module);
        MentoringModule saved = moduleRepository.save(module);
        return MentoringModuleResponse.from(saved);
    }

    public List<MentoringModuleResponse> getModulesByMentoringId(Long mentoringId) {
        return moduleRepository.findByMentoringIdOrderByWeekNumberAsc(mentoringId).stream()
                .map(MentoringModuleResponse::from)
                .collect(Collectors.toList());
    }

    public MentoringModuleResponse getModule(Long moduleId) {
        MentoringModule module = moduleRepository.findById(moduleId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MENTORING_MODULE_NOT_FOUND));
        return MentoringModuleResponse.from(module);
    }

    public MentoringModuleResponse getModuleBySeasonJobRoleAndWeek(Long seasonId, JobRole jobRole, Integer weekNumber) {
        MentoringModule module = moduleRepository.findBySeasonIdAndJobRoleAndWeekNumber(seasonId, jobRole, weekNumber)
                .orElseThrow(() -> new BusinessException(ErrorCode.MENTORING_MODULE_NOT_FOUND));
        return MentoringModuleResponse.from(module);
    }

    @Transactional
    public MentoringModuleResponse updateModule(Long moduleId, MentoringModuleRequest request) {
        MentoringModule module = moduleRepository.findById(moduleId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MENTORING_MODULE_NOT_FOUND));

        module.update(
                request.getTitle(),
                request.getDescription(),
                request.getLearningContent(),
                toJson(request.getKeyPoints()),
                toJson(request.getCommonMistakes()),
                toJson(request.getPracticeTasks()),
                toJson(request.getReferenceMaterials()),
                request.getEstimatedMinutes()
        );

        return MentoringModuleResponse.from(module);
    }

    @Transactional
    public void deleteModule(Long moduleId) {
        MentoringModule module = moduleRepository.findById(moduleId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MENTORING_MODULE_NOT_FOUND));
        moduleRepository.delete(module);
    }

    // ========== MentoringSession 관련 ==========

    @Transactional
    public MentoringSessionResponse startSession(Long userId, Long moduleId) {
        MentoringModule module = moduleRepository.findById(moduleId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MENTORING_MODULE_NOT_FOUND));

        // 사용자의 TeamMember 찾기 (해당 시즌)
        Long seasonId = module.getMentoring().getSeason().getId();

        // 멘토링 접근 권한 확인 (구독 필요)
        validateMentoringAccess(userId, seasonId);

        TeamMember teamMember = teamMemberRepository.findByUserIdAndSeasonId(userId, seasonId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TEAM_MEMBER_NOT_FOUND));

        // 이미 세션 존재 여부 확인
        if (sessionRepository.existsByMenteeIdAndModuleId(teamMember.getId(), moduleId)) {
            throw new BusinessException(ErrorCode.SESSION_ALREADY_EXISTS);
        }

        MentoringSession session = MentoringSession.builder()
                .mentee(teamMember)
                .module(module)
                .build();

        session.startSession();
        MentoringSession saved = sessionRepository.save(session);
        return MentoringSessionResponse.from(saved);
    }

    public MentoringSessionResponse getSession(Long sessionId, Long userId) {
        MentoringSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MENTORING_SESSION_NOT_FOUND));

        // 세션 접근 권한 확인 (멘티 본인 또는 담당 멘토만 조회 가능)
        validateSessionAccess(session, userId);

        return MentoringSessionResponse.from(session);
    }

    public List<MentoringSessionResponse> getMySessionsBySeasonId(Long userId, Long seasonId) {
        return sessionRepository.findByUserIdAndSeasonId(userId, seasonId).stream()
                .map(MentoringSessionResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public MentoringSessionResponse updateSessionNotes(Long sessionId, Long userId, MentoringSessionRequest request) {
        MentoringSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MENTORING_SESSION_NOT_FOUND));

        // 멘티 본인인지 확인
        if (!session.getMentee().getUser().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.NOT_SESSION_MENTEE);
        }

        session.updateNotes(
                request.getSessionNotes(),
                request.getQuestions(),
                toJson(request.getCompletedTaskIndexes())
        );

        return MentoringSessionResponse.from(session);
    }

    @Transactional
    public MentoringSessionResponse requestFeedback(Long sessionId, Long userId) {
        MentoringSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MENTORING_SESSION_NOT_FOUND));

        if (!session.getMentee().getUser().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.NOT_SESSION_MENTEE);
        }

        if (session.getStatus() != SessionStatus.IN_PROGRESS) {
            throw new BusinessException(ErrorCode.INVALID_SESSION_STATUS);
        }

        // 피드백 대기 상태로 변경
        session.startSession(); // 이미 IN_PROGRESS이므로 상태 유지 후 별도 처리
        // 실제로는 status를 PENDING_FEEDBACK으로 변경하는 메서드 필요

        return MentoringSessionResponse.from(session);
    }

    @Transactional
    public MentoringSessionResponse provideFeedback(Long sessionId, Long mentorId, MentoringFeedbackRequest request) {
        MentoringSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MENTORING_SESSION_NOT_FOUND));

        User mentor = userRepository.findById(mentorId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 멘토 할당
        if (session.getMentor() == null) {
            session.assignMentor(mentor);
        } else if (!session.getMentor().getId().equals(mentorId)) {
            throw new BusinessException(ErrorCode.NOT_SESSION_MENTOR);
        }

        session.provideFeedback(
                request.getMentorFeedback(),
                request.getFeedbackScore(),
                request.getNextSteps()
        );

        session.complete();
        return MentoringSessionResponse.from(session);
    }

    @Transactional
    public MentoringSessionResponse completeSession(Long sessionId, Long userId) {
        MentoringSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MENTORING_SESSION_NOT_FOUND));

        if (!session.getMentee().getUser().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.NOT_SESSION_MENTEE);
        }

        session.complete();
        return MentoringSessionResponse.from(session);
    }

    public MentoringProgressResponse getUserProgress(Long userId, Long seasonId, JobRole jobRole) {
        JobRoleMentoring mentoring = mentoringRepository.findBySeasonIdAndJobRoleWithModules(seasonId, jobRole)
                .orElseThrow(() -> new BusinessException(ErrorCode.MENTORING_NOT_FOUND));

        List<MentoringSession> sessions = sessionRepository.findByUserIdAndSeasonId(userId, seasonId);
        Map<Long, MentoringSession> sessionMap = sessions.stream()
                .collect(Collectors.toMap(s -> s.getModule().getId(), s -> s));

        List<MentoringProgressResponse.ModuleProgress> moduleProgress = new ArrayList<>();
        int completedCount = 0;
        int inProgressCount = 0;

        for (MentoringModule module : mentoring.getModules()) {
            MentoringSession session = sessionMap.get(module.getId());
            SessionStatus status = session != null ? session.getStatus() : SessionStatus.NOT_STARTED;
            Integer feedbackScore = session != null ? session.getFeedbackScore() : null;

            if (status == SessionStatus.COMPLETED) {
                completedCount++;
            } else if (status == SessionStatus.IN_PROGRESS || status == SessionStatus.PENDING_FEEDBACK) {
                inProgressCount++;
            }

            moduleProgress.add(MentoringProgressResponse.ModuleProgress.builder()
                    .moduleId(module.getId())
                    .weekNumber(module.getWeekNumber())
                    .title(module.getTitle())
                    .status(status)
                    .feedbackScore(feedbackScore)
                    .estimatedMinutes(module.getEstimatedMinutes())
                    .build());
        }

        int totalModules = mentoring.getModules().size();
        double progressPercentage = totalModules > 0 ?
                (double) completedCount / totalModules * 100 : 0;

        Double averageScore = sessionRepository.findAverageFeedbackScoreByUserId(userId)
                .orElse(null);

        return MentoringProgressResponse.builder()
                .userId(userId)
                .seasonId(seasonId)
                .jobRole(jobRole)
                .mentoringTitle(mentoring.getTitle())
                .totalModules(totalModules)
                .completedModules(completedCount)
                .inProgressModules(inProgressCount)
                .progressPercentage(Math.round(progressPercentage * 10) / 10.0)
                .averageFeedbackScore(averageScore)
                .moduleProgress(moduleProgress)
                .build();
    }

    // ========== Helper Methods ==========

    /**
     * 사용자가 해당 시즌의 멘토링 기능에 접근할 수 있는지 확인
     * (멘토링이 포함된 활성 구독이 있어야 함)
     */
    private void validateMentoringAccess(Long userId, Long seasonId) {
        Subscription subscription = subscriptionRepository.findActiveByUserIdAndSeasonId(userId, seasonId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SUBSCRIPTION_NOT_FOUND,
                        "멘토링 기능을 이용하려면 구독이 필요합니다."));

        if (!subscription.hasMentoring()) {
            throw new BusinessException(ErrorCode.MENTORING_ACCESS_DENIED,
                    "현재 구독 플랜에 멘토링이 포함되어 있지 않습니다.");
        }
    }

    /**
     * 사용자가 해당 세션에 접근할 수 있는지 확인
     * (멘티 본인 또는 담당 멘토만 접근 가능)
     */
    private void validateSessionAccess(MentoringSession session, Long userId) {
        // 멘티 본인인지 확인
        if (session.getMentee().getUser().getId().equals(userId)) {
            return;
        }

        // 담당 멘토인지 확인
        if (session.getMentor() != null && session.getMentor().getId().equals(userId)) {
            return;
        }

        throw new BusinessException(ErrorCode.ACCESS_DENIED, "해당 세션에 접근할 권한이 없습니다.");
    }

    private String toJson(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return null;
        }
    }
}
