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
import com.restartpoint.domain.season.repository.SeasonRepository;
import com.restartpoint.global.exception.BusinessException;
import com.restartpoint.global.exception.ErrorCode;
import com.restartpoint.global.config.CacheConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
    private final SeasonRepository seasonRepository;

    // 이상치 판단 기준: 평균에서 이 값 이상 벗어나면 이상치
    private static final double OUTLIER_THRESHOLD = 1.5;

    // 시즌 분석 시 프로젝트별 병렬 처리용
    private final ExecutorService seasonExecutor = Executors.newFixedThreadPool(3);

    /**
     * 프로젝트 심사 분석 조회
     */
    @Cacheable(cacheNames = CacheConfig.PROJECT_REVIEW_ANALYSIS_CACHE, key = "#projectId")
    public ReviewAnalysisResponse analyzeProjectReviews(Long projectId) {
        Project project = projectRepository.findByIdWithTeam(projectId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROJECT_NOT_FOUND));
        return buildProjectAnalysis(project);
    }

    private ReviewAnalysisResponse buildProjectAnalysis(Project project) {
        Long projectId = project.getId();

        List<Review> expertReviews = reviewRepository.findByProjectIdWithReviewer(projectId).stream()
                .filter(r -> r.getReviewType() == ReviewType.EXPERT)
                .toList();

        if (expertReviews.isEmpty()) {
            return buildEmptyAnalysis(project);
        }

        // 기본 통계 계산
        double overallAvg = calculateAverageScore(expertReviews);
        double expertAvg = calculateAverageScore(expertReviews);

        // 루브릭별 평균 계산
        Map<RubricItem, Double> rubricAvg = calculateRubricAverages(expertReviews);
        Map<RubricItem, Double> expertRubricAvg = calculateRubricAverages(expertReviews);

        String commentSummary = buildCommentSummary(project.getName(), expertReviews, rubricAvg);
        Map<String, List<String>> strengthsWeaknesses = buildStrengthWeaknesses(rubricAvg);
        String expertVsCandidateAnalysis = buildReviewPolicySummary();
        List<RubricAnalysis> rubricAnalyses = buildRubricAnalyses(expertReviews, rubricAvg, expertRubricAvg);
        List<OutlierScore> outliers = detectOutliers(expertReviews, rubricAvg);

        return ReviewAnalysisResponse.builder()
                .projectId(projectId)
                .projectName(project.getName())
                .teamName(project.getTeam().getName())
                .totalReviewCount(expertReviews.size())
                .expertReviewCount(expertReviews.size())
                .candidateReviewCount(0)
                .overallAverageScore(round(overallAvg))
                .expertAverageScore(round(expertAvg))
                .candidateAverageScore(0)
                .scoreDifference(0)
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
        // 시즌 존재 여부 검증
        if (!seasonRepository.existsById(seasonId)) {
            throw new BusinessException(ErrorCode.SEASON_NOT_FOUND);
        }

        List<Project> projects = projectRepository.findAllBySeasonId(seasonId);

        if (projects.isEmpty()) {
            return List.of();
        }

        // 프로젝트별 분석을 병렬로 실행 (seasonExecutor 사용 - 교착 방지)
        List<CompletableFuture<ReviewAnalysisResponse>> futures = projects.stream()
                .map(project -> CompletableFuture.supplyAsync(
                        () -> buildProjectAnalysis(project),
                        seasonExecutor))
                .toList();

        // 모든 분석 완료 대기 후 결과 수집
        return futures.stream()
                .map(future -> {
                    try {
                        return future.join();
                    } catch (Exception e) {
                        log.error("시즌 심사 분석 집계 실패", e);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
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

    private List<OutlierScore> detectOutliers(List<Review> reviews, Map<RubricItem, Double> rubricAverages) {

        // 먼저 이상치 기본 정보 수집
        List<OutlierScore> baseOutliers = new ArrayList<>();

        for (Review review : reviews) {
            for (ReviewScore score : review.getScores()) {
                double avg = rubricAverages.getOrDefault(score.getRubricItem(), 3.0);
                double deviation = score.getScore() - avg;

                if (Math.abs(deviation) >= OUTLIER_THRESHOLD) {
                    baseOutliers.add(OutlierScore.builder()
                            .reviewId(review.getId())
                            .reviewerName(review.getReviewer().getName())
                            .reviewType(review.getReviewType().name())
                            .rubricItem(score.getRubricItem())
                            .score(score.getScore())
                            .averageScore(round(avg))
                            .deviation(round(deviation))
                            .possibleReason(buildOutlierReason(score.getScore(), avg, deviation))
                            .build());
                }
            }
        }

        // 편차 절대값 기준 내림차순 정렬하여 상위 10개만 선택
        List<OutlierScore> topOutliers = baseOutliers.stream()
                .sorted((a, b) -> Double.compare(Math.abs(b.getDeviation()), Math.abs(a.getDeviation())))
                .limit(10)
                .toList();

        if (topOutliers.isEmpty()) {
            return List.of();
        }

        return topOutliers;
    }

    private List<RubricAnalysis> buildRubricAnalyses(
            List<Review> reviews,
            Map<RubricItem, Double> rubricAvg,
            Map<RubricItem, Double> expertRubricAvg) {

        return Arrays.stream(RubricItem.values())
                .map(item -> {
                    double avg = rubricAvg.getOrDefault(item, 0.0);
                    double expertAvg = expertRubricAvg.getOrDefault(item, 0.0);
                    long commentCount = reviews.stream()
                            .flatMap(r -> r.getScores().stream())
                            .filter(s -> s.getRubricItem() == item && s.getComment() != null && !s.getComment().isBlank())
                            .count();

                    return RubricAnalysis.builder()
                            .rubricItem(item)
                            .label(item.getLabel())
                            .averageScore(round(avg))
                            .expertAverageScore(round(expertAvg))
                            .candidateAverageScore(0)
                            .scoreDifference(0)
                            .aiInsight(buildRubricInsight(avg, commentCount))
                            .build();
                })
                .toList();
    }

    private String buildCommentSummary(
            String projectName,
            List<Review> reviews,
            Map<RubricItem, Double> rubricAvg) {
        long commentCount = reviews.stream()
                .map(Review::getOverallComment)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(comment -> !comment.isEmpty())
                .count();

        RubricItem strongestItem = rubricAvg.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
        RubricItem weakestItem = rubricAvg.entrySet().stream()
                .min(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);

        List<String> summaryParts = new ArrayList<>();
        summaryParts.add(String.format("전문가 심사 %d건을 기준으로 '%s' 프로젝트를 집계했습니다.", reviews.size(), projectName));

        if (strongestItem != null && weakestItem != null) {
            summaryParts.add(String.format("가장 높은 항목은 %s(%.2f점), 가장 낮은 항목은 %s(%.2f점)입니다.",
                    strongestItem.getLabel(),
                    round(rubricAvg.getOrDefault(strongestItem, 0.0)),
                    weakestItem.getLabel(),
                    round(rubricAvg.getOrDefault(weakestItem, 0.0))));
        }

        if (commentCount > 0) {
            summaryParts.add(String.format("종합 코멘트 %d건이 남아 있어 상세 의견을 함께 검토할 수 있습니다.", commentCount));
        } else {
            summaryParts.add("작성된 종합 코멘트는 없어서 점수 통계 중심으로 확인하면 됩니다.");
        }

        return String.join(" ", summaryParts);
    }

    private Map<String, List<String>> buildStrengthWeaknesses(Map<RubricItem, Double> rubricAvg) {
        Map<String, List<String>> result = new HashMap<>();

        List<String> strengths = rubricAvg.entrySet().stream()
                .sorted(Map.Entry.<RubricItem, Double>comparingByValue().reversed())
                .limit(3)
                .map(entry -> String.format("%s 평균이 %.2f점으로 상대적으로 높습니다.",
                        entry.getKey().getLabel(),
                        round(entry.getValue())))
                .toList();

        List<String> weaknesses = rubricAvg.entrySet().stream()
                .sorted(Map.Entry.comparingByValue())
                .limit(3)
                .map(entry -> String.format("%s 평균이 %.2f점으로 보완 우선순위가 높습니다.",
                        entry.getKey().getLabel(),
                        round(entry.getValue())))
                .toList();

        result.put("strengths", strengths);
        result.put("weaknesses", weaknesses);
        return result;
    }

    private String buildReviewPolicySummary() {
        return "현재 시즌 심사는 전문가 평가만 운영합니다. 프로젝트별 평균, 강점, 약점, 이상치 분석은 모두 전문가 심사 데이터 기준의 통계로 제공합니다.";
    }

    private String buildRubricInsight(double averageScore, long commentCount) {
        String level = averageScore >= 4.5 ? "매우 우수" :
                averageScore >= 4.0 ? "우수" :
                        averageScore >= 3.0 ? "보통" : "개선 필요";

        if (commentCount > 0) {
            return String.format("평균 %.2f점으로 %s 수준이며, 관련 세부 코멘트 %d건을 함께 확인할 수 있습니다.",
                    round(averageScore), level, commentCount);
        }

        return String.format("평균 %.2f점으로 %s 수준이며, 현재는 점수 통계 중심으로 해석하면 됩니다.",
                round(averageScore), level);
    }

    private String buildOutlierReason(int score, double averageScore, double deviation) {
        if (deviation > 0) {
            return String.format("%d점으로 평균 %.2f점보다 높습니다. 긍정적 판단이 강했던 심사라 세부 코멘트 확인이 필요합니다.",
                    score, round(averageScore));
        }

        return String.format("%d점으로 평균 %.2f점보다 낮습니다. 보완 필요 항목으로 인식됐을 가능성이 높아 세부 코멘트 확인이 필요합니다.",
                score, round(averageScore));
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
