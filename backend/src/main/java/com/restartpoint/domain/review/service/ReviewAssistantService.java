package com.restartpoint.domain.review.service;

import com.restartpoint.domain.project.entity.Project;
import com.restartpoint.domain.project.repository.ProjectRepository;
import com.restartpoint.domain.review.dto.ReviewAnalysisResponse;
import com.restartpoint.domain.review.dto.ReviewAnalysisResponse.OutlierScore;
import com.restartpoint.domain.review.dto.ReviewAnalysisResponse.RubricAnalysis;
import com.restartpoint.domain.review.entity.Review;
import com.restartpoint.domain.review.entity.ReviewScore;
import com.restartpoint.domain.review.entity.ReviewType;
import com.restartpoint.domain.review.entity.RubricItem;
import com.restartpoint.domain.review.repository.ReviewRepository;
import com.restartpoint.global.exception.BusinessException;
import com.restartpoint.global.exception.ErrorCode;
import com.restartpoint.infra.ai.AiReviewAssistantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 심사 분석 서비스
 * AI를 활용하여 심사 데이터를 분석하고 운영자에게 인사이트 제공
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class ReviewAssistantService {

    private final ReviewRepository reviewRepository;
    private final ProjectRepository projectRepository;
    private final AiReviewAssistantService aiReviewAssistantService;

    // 이상치 판단 기준: 평균에서 이 값 이상 벗어나면 이상치
    private static final double OUTLIER_THRESHOLD = 1.5;

    /**
     * 프로젝트 심사 분석 조회
     */
    public ReviewAnalysisResponse analyzeProjectReviews(Long projectId) {
        Project project = projectRepository.findByIdWithTeam(projectId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROJECT_NOT_FOUND));

        List<Review> allReviews = reviewRepository.findByProjectIdWithReviewer(projectId);

        if (allReviews.isEmpty()) {
            return buildEmptyAnalysis(project);
        }

        List<Review> expertReviews = allReviews.stream()
                .filter(r -> r.getReviewType() == ReviewType.EXPERT)
                .toList();
        List<Review> candidateReviews = allReviews.stream()
                .filter(r -> r.getReviewType() == ReviewType.CANDIDATE)
                .toList();

        // 기본 통계 계산
        double overallAvg = calculateAverageScore(allReviews);
        double expertAvg = calculateAverageScore(expertReviews);
        double candidateAvg = calculateAverageScore(candidateReviews);

        // 루브릭별 평균 계산
        Map<RubricItem, Double> rubricAvg = calculateRubricAverages(allReviews);
        Map<RubricItem, Double> expertRubricAvg = calculateRubricAverages(expertReviews);
        Map<RubricItem, Double> candidateRubricAvg = calculateRubricAverages(candidateReviews);

        // 이상치 감지
        List<OutlierScore> outliers = detectOutliers(allReviews, rubricAvg, project);

        // AI 분석
        String commentSummary = aiReviewAssistantService.summarizeComments(
                allReviews, project.getName());

        Map<String, List<String>> strengthsWeaknesses = aiReviewAssistantService
                .analyzeStrengthsAndWeaknesses(allReviews, project.getName(), rubricAvg);

        String expertVsCandidateAnalysis = aiReviewAssistantService
                .analyzeExpertVsCandidateDifference(expertAvg, candidateAvg, expertRubricAvg, candidateRubricAvg);

        // 루브릭별 분석
        List<RubricAnalysis> rubricAnalyses = buildRubricAnalyses(
                allReviews, rubricAvg, expertRubricAvg, candidateRubricAvg);

        return ReviewAnalysisResponse.builder()
                .projectId(projectId)
                .projectName(project.getName())
                .teamName(project.getTeam().getName())
                .totalReviewCount(allReviews.size())
                .expertReviewCount(expertReviews.size())
                .candidateReviewCount(candidateReviews.size())
                .overallAverageScore(round(overallAvg))
                .expertAverageScore(round(expertAvg))
                .candidateAverageScore(round(candidateAvg))
                .scoreDifference(round(expertAvg - candidateAvg))
                .rubricAnalyses(rubricAnalyses)
                .commentSummary(commentSummary)
                .strengths(strengthsWeaknesses.get("strengths"))
                .weaknesses(strengthsWeaknesses.get("weaknesses"))
                .outliers(outliers)
                .expertVsCandidateAnalysis(expertVsCandidateAnalysis)
                .build();
    }

    /**
     * 시즌 전체 심사 분석 요약
     */
    public List<ReviewAnalysisResponse> analyzeSeasonReviews(Long seasonId) {
        List<Project> projects = projectRepository.findAllBySeasonId(seasonId);

        return projects.stream()
                .map(project -> analyzeProjectReviews(project.getId()))
                .filter(analysis -> analysis.getTotalReviewCount() > 0)
                .toList();
    }

    // === 헬퍼 메서드 ===

    private ReviewAnalysisResponse buildEmptyAnalysis(Project project) {
        return ReviewAnalysisResponse.builder()
                .projectId(project.getId())
                .projectName(project.getName())
                .teamName(project.getTeam().getName())
                .totalReviewCount(0)
                .expertReviewCount(0)
                .candidateReviewCount(0)
                .overallAverageScore(0)
                .expertAverageScore(0)
                .candidateAverageScore(0)
                .scoreDifference(0)
                .rubricAnalyses(List.of())
                .commentSummary("심사 데이터가 없습니다.")
                .strengths(List.of())
                .weaknesses(List.of())
                .outliers(List.of())
                .expertVsCandidateAnalysis("심사 데이터가 없습니다.")
                .build();
    }

    private double calculateAverageScore(List<Review> reviews) {
        if (reviews.isEmpty()) return 0.0;
        return reviews.stream()
                .mapToDouble(Review::calculateAverageScore)
                .average()
                .orElse(0.0);
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
            averages.put(item, avg);
        }

        return averages;
    }

    private List<OutlierScore> detectOutliers(List<Review> reviews,
            Map<RubricItem, Double> rubricAverages, Project project) {

        List<OutlierScore> outliers = new ArrayList<>();

        for (Review review : reviews) {
            for (ReviewScore score : review.getScores()) {
                double avg = rubricAverages.getOrDefault(score.getRubricItem(), 3.0);
                double deviation = score.getScore() - avg;

                if (Math.abs(deviation) >= OUTLIER_THRESHOLD) {
                    OutlierScore outlier = OutlierScore.builder()
                            .reviewId(review.getId())
                            .reviewerName(review.getReviewer().getName())
                            .reviewType(review.getReviewType().name())
                            .rubricItem(score.getRubricItem())
                            .score(score.getScore())
                            .averageScore(round(avg))
                            .deviation(round(deviation))
                            .build();

                    // AI로 원인 분석 (비동기로 하면 더 좋지만 일단 동기로)
                    String projectContext = String.format("%s - %s",
                            project.getName(), project.getProblemDefinition());
                    String reason = aiReviewAssistantService.analyzeOutlierReason(outlier, projectContext);
                    outlier = OutlierScore.builder()
                            .reviewId(outlier.getReviewId())
                            .reviewerName(outlier.getReviewerName())
                            .reviewType(outlier.getReviewType())
                            .rubricItem(outlier.getRubricItem())
                            .score(outlier.getScore())
                            .averageScore(outlier.getAverageScore())
                            .deviation(outlier.getDeviation())
                            .possibleReason(reason)
                            .build();

                    outliers.add(outlier);
                }
            }
        }

        // 편차 절대값 기준 내림차순 정렬 (가장 큰 이상치 먼저)
        outliers.sort((a, b) -> Double.compare(Math.abs(b.getDeviation()), Math.abs(a.getDeviation())));

        // 상위 10개만 반환
        return outliers.stream().limit(10).toList();
    }

    private List<RubricAnalysis> buildRubricAnalyses(
            List<Review> reviews,
            Map<RubricItem, Double> rubricAvg,
            Map<RubricItem, Double> expertRubricAvg,
            Map<RubricItem, Double> candidateRubricAvg) {

        List<RubricAnalysis> analyses = new ArrayList<>();

        for (RubricItem item : RubricItem.values()) {
            double avg = rubricAvg.getOrDefault(item, 0.0);
            double expertAvg = expertRubricAvg.getOrDefault(item, 0.0);
            double candidateAvg = candidateRubricAvg.getOrDefault(item, 0.0);

            // 해당 항목 관련 코멘트 수집
            List<String> relatedComments = reviews.stream()
                    .flatMap(r -> r.getScores().stream())
                    .filter(s -> s.getRubricItem() == item && s.getComment() != null)
                    .map(ReviewScore::getComment)
                    .filter(c -> !c.isBlank())
                    .toList();

            String aiInsight = aiReviewAssistantService.generateRubricInsight(
                    item, avg, expertAvg, candidateAvg, relatedComments);

            analyses.add(RubricAnalysis.builder()
                    .rubricItem(item)
                    .label(item.getLabel())
                    .averageScore(round(avg))
                    .expertAverageScore(round(expertAvg))
                    .candidateAverageScore(round(candidateAvg))
                    .scoreDifference(round(expertAvg - candidateAvg))
                    .aiInsight(aiInsight)
                    .build());
        }

        return analyses;
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
