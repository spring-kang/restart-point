package com.restartpoint.domain.review.service;

import com.restartpoint.domain.project.entity.Project;
import com.restartpoint.domain.project.entity.ProjectStatus;
import com.restartpoint.domain.project.repository.ProjectRepository;
import com.restartpoint.domain.review.dto.*;
import com.restartpoint.domain.review.entity.*;
import com.restartpoint.domain.review.repository.ReviewGuideCompletionRepository;
import com.restartpoint.domain.review.repository.ReviewRepository;
import com.restartpoint.domain.review.repository.ReviewScoreRepository;
import com.restartpoint.domain.season.entity.Season;
import com.restartpoint.domain.season.entity.SeasonStatus;
import com.restartpoint.domain.season.repository.SeasonRepository;
import com.restartpoint.domain.team.entity.TeamMember;
import com.restartpoint.domain.team.entity.TeamMemberStatus;
import com.restartpoint.domain.team.repository.TeamMemberRepository;
import com.restartpoint.domain.user.entity.CertificationStatus;
import com.restartpoint.domain.user.entity.Role;
import com.restartpoint.domain.user.entity.User;
import com.restartpoint.domain.user.repository.UserRepository;
import com.restartpoint.global.exception.BusinessException;
import com.restartpoint.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ReviewScoreRepository reviewScoreRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final SeasonRepository seasonRepository;
    private final ReviewGuideCompletionRepository guideCompletionRepository;

    /**
     * 심사 제출
     */
    @Transactional
    public ReviewResponse createReview(Long userId, Long projectId, ReviewCreateRequest request) {
        User reviewer = findUserById(userId);
        Project project = findProjectById(projectId);

        // 검증
        validateReviewCreation(reviewer, project, request);

        // 심사자 유형 결정
        ReviewType reviewType = determineReviewType(reviewer);

        // 심사 생성
        Review review = Review.builder()
                .project(project)
                .reviewer(reviewer)
                .reviewType(reviewType)
                .overallComment(request.getOverallComment())
                .build();

        // 루브릭별 점수 추가
        for (ReviewScoreRequest scoreRequest : request.getScores()) {
            ReviewScore score = ReviewScore.builder()
                    .rubricItem(scoreRequest.getRubricItem())
                    .score(scoreRequest.getScore())
                    .comment(scoreRequest.getComment())
                    .build();
            review.addScore(score);
        }

        Review savedReview = reviewRepository.save(review);
        log.info("심사 제출 완료 - 심사자: {}, 프로젝트: {}, 유형: {}",
                reviewer.getName(), project.getName(), reviewType);

        return ReviewResponse.from(savedReview);
    }

    /**
     * 프로젝트별 심사 목록 조회
     */
    public List<ReviewResponse> getReviewsByProject(Long userId, Long projectId) {
        Project project = findProjectById(projectId);

        // 팀원이거나 운영자만 조회 가능
        User user = findUserById(userId);
        if (user.getRole() != Role.ADMIN) {
            validateTeamMemberOrReviewer(user, project);
        }

        return reviewRepository.findByProjectIdWithReviewer(projectId).stream()
                .map(ReviewResponse::from)
                .toList();
    }

    /**
     * 프로젝트 심사 요약 (집계)
     */
    public ReviewSummaryResponse getReviewSummary(Long userId, Long projectId) {
        Project project = findProjectById(projectId);
        Season season = project.getTeam().getSeason();

        // 팀원이거나 운영자만 조회 가능
        User user = findUserById(userId);
        if (user.getRole() != Role.ADMIN) {
            validateTeamMemberOrReviewer(user, project);
        }

        List<Review> allReviews = reviewRepository.findByProjectIdWithReviewer(projectId);
        List<Review> expertReviews = allReviews.stream()
                .filter(r -> r.getReviewType() == ReviewType.EXPERT)
                .toList();
        List<Review> candidateReviews = allReviews.stream()
                .filter(r -> r.getReviewType() == ReviewType.CANDIDATE)
                .toList();

        // 평균 점수 계산
        double expertAvg = calculateAverageScore(expertReviews);
        double candidateAvg = calculateAverageScore(candidateReviews);

        // 가중 평균 계산
        int expertWeight = season.getExpertReviewWeight();
        int candidateWeight = season.getCandidateReviewWeight();
        double weightedAvg = calculateWeightedAverage(expertAvg, candidateAvg, expertWeight, candidateWeight);

        // 루브릭별 평균 점수 계산
        Map<RubricItem, Double> rubricAverages = calculateRubricAverages(allReviews);
        Map<RubricItem, Double> expertRubricAverages = calculateRubricAverages(expertReviews);
        Map<RubricItem, Double> candidateRubricAverages = calculateRubricAverages(candidateReviews);

        return ReviewSummaryResponse.builder()
                .projectId(projectId)
                .projectName(project.getName())
                .totalReviewCount(allReviews.size())
                .expertReviewCount(expertReviews.size())
                .candidateReviewCount(candidateReviews.size())
                .weightedAverageScore(Math.round(weightedAvg * 100.0) / 100.0)
                .expertAverageScore(Math.round(expertAvg * 100.0) / 100.0)
                .candidateAverageScore(Math.round(candidateAvg * 100.0) / 100.0)
                .rubricAverages(rubricAverages)
                .expertRubricAverages(expertRubricAverages)
                .candidateRubricAverages(candidateRubricAverages)
                .build();
    }

    /**
     * 프로젝트 심사 요약 (내부 호출용 - 권한 검증 없음)
     * GrowthReportService 등 내부 서비스에서 사용
     */
    public ReviewSummaryResponse getReviewSummaryInternal(Long projectId) {
        Project project = findProjectById(projectId);
        Season season = project.getTeam().getSeason();

        List<Review> allReviews = reviewRepository.findByProjectIdWithReviewer(projectId);
        if (allReviews.isEmpty()) {
            return null;
        }

        List<Review> expertReviews = allReviews.stream()
                .filter(r -> r.getReviewType() == ReviewType.EXPERT)
                .toList();
        List<Review> candidateReviews = allReviews.stream()
                .filter(r -> r.getReviewType() == ReviewType.CANDIDATE)
                .toList();

        double expertAvg = calculateAverageScore(expertReviews);
        double candidateAvg = calculateAverageScore(candidateReviews);
        int expertWeight = season.getExpertReviewWeight();
        int candidateWeight = season.getCandidateReviewWeight();
        double weightedAvg = calculateWeightedAverage(expertAvg, candidateAvg, expertWeight, candidateWeight);

        Map<RubricItem, Double> rubricAverages = calculateRubricAverages(allReviews);
        Map<RubricItem, Double> expertRubricAverages = calculateRubricAverages(expertReviews);
        Map<RubricItem, Double> candidateRubricAverages = calculateRubricAverages(candidateReviews);

        return ReviewSummaryResponse.builder()
                .projectId(projectId)
                .projectName(project.getName())
                .totalReviewCount(allReviews.size())
                .expertReviewCount(expertReviews.size())
                .candidateReviewCount(candidateReviews.size())
                .weightedAverageScore(Math.round(weightedAvg * 100.0) / 100.0)
                .expertAverageScore(Math.round(expertAvg * 100.0) / 100.0)
                .candidateAverageScore(Math.round(candidateAvg * 100.0) / 100.0)
                .rubricAverages(rubricAverages)
                .expertRubricAverages(expertRubricAverages)
                .candidateRubricAverages(candidateRubricAverages)
                .build();
    }

    /**
     * 심사 가능한 프로젝트 목록 조회
     */
    public List<Project> getReviewableProjects(Long userId, Long seasonId) {
        User user = findUserById(userId);
        Season season = findSeasonById(seasonId);

        // 심사자 자격 확인 (REVIEWER 역할이거나 수료 인증 완료)
        if (!canReview(user)) {
            return List.of(); // 심사 자격이 없으면 빈 목록 반환
        }

        // 시즌이 심사 기간인지 확인
        if (season.getStatus() != SeasonStatus.REVIEWING) {
            return List.of(); // 심사 기간이 아니면 빈 목록 반환
        }

        // 시즌의 제출된 프로젝트 목록 조회
        List<Project> submittedProjects = projectRepository.findBySeasonIdAndStatus(seasonId, ProjectStatus.SUBMITTED, null)
                .getContent();

        // 사용자가 이미 심사한 프로젝트 제외
        // 자신의 프로젝트 제외
        return submittedProjects.stream()
                .filter(project -> !isTeamMember(user.getId(), project))
                .filter(project -> !reviewRepository.existsByProjectIdAndReviewerId(project.getId(), userId))
                .toList();
    }

    /**
     * 내가 심사한 목록 조회
     */
    public List<ReviewResponse> getMyReviews(Long userId) {
        return reviewRepository.findByReviewerIdWithProject(userId).stream()
                .map(ReviewResponse::simpleFrom)
                .toList();
    }

    // === 헬퍼 메서드 ===

    private void validateReviewCreation(User reviewer, Project project, ReviewCreateRequest request) {
        // 심사자 자격 확인 (REVIEWER 역할이거나 수료 인증 완료)
        if (!canReview(reviewer)) {
            throw new BusinessException(ErrorCode.NOT_CERTIFIED_REVIEWER);
        }

        // CANDIDATE (일반 수료생) 심사자는 가이드 학습 완료 필수
        ReviewType reviewType = determineReviewType(reviewer);
        if (reviewType == ReviewType.CANDIDATE) {
            if (!guideCompletionRepository.existsByUserIdAndFullyCompletedTrue(reviewer.getId())) {
                throw new BusinessException(ErrorCode.GUIDE_NOT_COMPLETED);
            }
        }

        // 시즌이 심사 기간인지 확인
        Season season = project.getTeam().getSeason();
        if (season.getStatus() != SeasonStatus.REVIEWING) {
            throw new BusinessException(ErrorCode.SEASON_NOT_REVIEWING);
        }

        // 프로젝트가 제출 완료 상태인지 확인
        if (project.getStatus() != ProjectStatus.SUBMITTED) {
            throw new BusinessException(ErrorCode.PROJECT_NOT_SUBMITTED);
        }

        // 자신의 프로젝트가 아닌지 확인
        if (isTeamMember(reviewer.getId(), project)) {
            throw new BusinessException(ErrorCode.CANNOT_REVIEW_OWN_PROJECT);
        }

        // 이미 심사했는지 확인
        if (reviewRepository.existsByProjectIdAndReviewerId(project.getId(), reviewer.getId())) {
            throw new BusinessException(ErrorCode.REVIEW_ALREADY_EXISTS);
        }

        // 모든 루브릭 항목에 점수가 있는지 확인
        Set<RubricItem> providedItems = new HashSet<>();
        for (ReviewScoreRequest score : request.getScores()) {
            providedItems.add(score.getRubricItem());
        }
        if (providedItems.size() != RubricItem.values().length) {
            throw new BusinessException(ErrorCode.INVALID_RUBRIC_SCORES);
        }
    }

    /**
     * 심사 가능 여부 확인
     * - REVIEWER 역할: 전문가 심사자 (항상 심사 가능)
     * - 일반 사용자: 수료 인증 완료(APPROVED)된 경우만 심사 가능
     */
    private boolean canReview(User user) {
        if (user.getRole() == Role.REVIEWER || user.getRole() == Role.ADMIN) {
            return true;
        }
        return user.getCertificationStatus() == CertificationStatus.APPROVED;
    }

    private ReviewType determineReviewType(User user) {
        // REVIEWER 역할이면 EXPERT, 그 외는 CANDIDATE
        if (user.getRole() == Role.REVIEWER) {
            return ReviewType.EXPERT;
        }
        return ReviewType.CANDIDATE;
    }

    private boolean isTeamMember(Long userId, Project project) {
        Long teamId = project.getTeam().getId();

        // 팀 리더인지 확인
        if (project.getTeam().getLeader().getId().equals(userId)) {
            return true;
        }

        // 팀원인지 확인
        return teamMemberRepository.findByTeamId(teamId).stream()
                .anyMatch(member -> member.getUser().getId().equals(userId)
                        && member.getStatus() == TeamMemberStatus.ACCEPTED);
    }

    private void validateTeamMemberOrReviewer(User user, Project project) {
        // 팀원이면 OK
        if (isTeamMember(user.getId(), project)) {
            return;
        }

        // 해당 프로젝트를 심사한 사람이면 OK
        if (reviewRepository.existsByProjectIdAndReviewerId(project.getId(), user.getId())) {
            return;
        }

        throw new BusinessException(ErrorCode.ACCESS_DENIED);
    }

    private double calculateAverageScore(List<Review> reviews) {
        if (reviews.isEmpty()) {
            return 0.0;
        }
        return reviews.stream()
                .mapToDouble(Review::calculateAverageScore)
                .average()
                .orElse(0.0);
    }

    private double calculateWeightedAverage(double expertAvg, double candidateAvg, int expertWeight, int candidateWeight) {
        if (expertAvg == 0 && candidateAvg == 0) {
            return 0.0;
        }
        if (expertAvg == 0) {
            return candidateAvg;
        }
        if (candidateAvg == 0) {
            return expertAvg;
        }
        return (expertAvg * expertWeight + candidateAvg * candidateWeight) / 100.0;
    }

    private Map<RubricItem, Double> calculateRubricAverages(List<Review> reviews) {
        Map<RubricItem, Double> averages = new EnumMap<>(RubricItem.class);

        for (RubricItem item : RubricItem.values()) {
            double avg = reviews.stream()
                    .flatMap(r -> r.getScores().stream())
                    .filter(s -> s.getRubricItem() == item)
                    .mapToInt(ReviewScore::getScore)
                    .average()
                    .orElse(0.0);
            averages.put(item, Math.round(avg * 100.0) / 100.0);
        }

        return averages;
    }

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    private Project findProjectById(Long projectId) {
        return projectRepository.findByIdWithTeam(projectId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROJECT_NOT_FOUND));
    }

    private Season findSeasonById(Long seasonId) {
        return seasonRepository.findById(seasonId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SEASON_NOT_FOUND));
    }
}
