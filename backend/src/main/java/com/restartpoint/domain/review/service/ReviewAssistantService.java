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
import com.restartpoint.infra.ai.AiReviewAssistantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

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
    private final AiReviewAssistantService aiReviewAssistantService;

    // 이상치 판단 기준: 평균에서 이 값 이상 벗어나면 이상치
    private static final double OUTLIER_THRESHOLD = 1.5;

    // 스레드 풀 분리: 교착 상태 방지
    // - seasonExecutor: 시즌 분석 시 프로젝트별 병렬 처리용
    // - aiExecutor: 프로젝트 분석 내 AI 호출 병렬 처리용
    private final ExecutorService seasonExecutor = Executors.newFixedThreadPool(3);
    private final ExecutorService aiExecutor = Executors.newFixedThreadPool(10);

    /**
     * 프로젝트 심사 분석 조회
     */
    public ReviewAnalysisResponse analyzeProjectReviews(Long projectId) {
        Project project = projectRepository.findByIdWithTeam(projectId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROJECT_NOT_FOUND));

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

        // AI 분석을 병렬로 실행
        CompletableFuture<String> commentSummaryFuture = supplySafely(
                () -> aiReviewAssistantService.summarizeComments(expertReviews, project.getName()),
                "AI 코멘트 요약",
                "AI 분석에 실패했습니다.");

        CompletableFuture<Map<String, List<String>>> strengthsWeaknessesFuture = supplySafely(
                () -> aiReviewAssistantService.analyzeStrengthsAndWeaknesses(expertReviews, project.getName(), rubricAvg),
                "AI 강점/약점 분석",
                defaultStrengthWeaknesses());

        CompletableFuture<String> expertVsCandidateFuture = supplySafely(
                aiReviewAssistantService::summarizeExpertReviewPolicy,
                "AI 심사 정책 요약",
                "현재 시즌 심사는 전문가 평가만 운영합니다.");

        // 루브릭별 분석 (병렬 처리)
        CompletableFuture<List<RubricAnalysis>> rubricAnalysesFuture = supplySafely(
                () -> buildRubricAnalyses(expertReviews, rubricAvg, expertRubricAvg),
                "AI 루브릭 분석",
                buildFallbackRubricAnalyses(rubricAvg, expertRubricAvg));

        // 이상치 감지 (병렬 처리)
        CompletableFuture<List<OutlierScore>> outliersFuture = supplySafely(
                () -> detectOutliers(expertReviews, rubricAvg, project),
                "AI 이상치 분석",
                List.of());

        // 모든 AI 분석 완료 대기
        CompletableFuture.allOf(
                commentSummaryFuture,
                strengthsWeaknessesFuture,
                expertVsCandidateFuture,
                rubricAnalysesFuture,
                outliersFuture
        ).join();

        // 결과 추출
        String commentSummary = commentSummaryFuture.join();
        Map<String, List<String>> strengthsWeaknesses = strengthsWeaknessesFuture.join();
        String expertVsCandidateAnalysis = expertVsCandidateFuture.join();
        List<RubricAnalysis> rubricAnalyses = rubricAnalysesFuture.join();
        List<OutlierScore> outliers = outliersFuture.join();

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
                        () -> analyzeProjectReviews(project.getId()),
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

    private List<OutlierScore> detectOutliers(List<Review> reviews,
            Map<RubricItem, Double> rubricAverages, Project project) {

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

        // AI로 원인 분석 (병렬 처리)
        String projectContext = String.format("%s - %s",
                project.getName(), project.getProblemDefinition());

        List<CompletableFuture<OutlierScore>> futures = topOutliers.stream()
                .map(outlier -> CompletableFuture.supplyAsync(() -> {
                    String reason = aiReviewAssistantService.analyzeOutlierReason(outlier, projectContext);
                    return OutlierScore.builder()
                            .reviewId(outlier.getReviewId())
                            .reviewerName(outlier.getReviewerName())
                            .reviewType(outlier.getReviewType())
                            .rubricItem(outlier.getRubricItem())
                            .score(outlier.getScore())
                            .averageScore(outlier.getAverageScore())
                            .deviation(outlier.getDeviation())
                            .possibleReason(reason)
                            .build();
                }, aiExecutor))
                .toList();

        return futures.stream()
                .map(CompletableFuture::join)
                .toList();
    }

    private List<RubricAnalysis> buildRubricAnalyses(
            List<Review> reviews,
            Map<RubricItem, Double> rubricAvg,
            Map<RubricItem, Double> expertRubricAvg) {

        // 각 루브릭 항목에 대해 병렬로 AI 분석 실행
        List<CompletableFuture<RubricAnalysis>> futures = Arrays.stream(RubricItem.values())
                .map(item -> CompletableFuture.supplyAsync(() -> {
                    double avg = rubricAvg.getOrDefault(item, 0.0);
                    double expertAvg = expertRubricAvg.getOrDefault(item, 0.0);

                    // 해당 항목 관련 코멘트 수집
                    List<String> relatedComments = reviews.stream()
                            .flatMap(r -> r.getScores().stream())
                            .filter(s -> s.getRubricItem() == item && s.getComment() != null)
                            .map(ReviewScore::getComment)
                            .filter(c -> !c.isBlank())
                            .toList();

                    String aiInsight = aiReviewAssistantService.generateRubricInsight(
                            item, avg, expertAvg, relatedComments);

                    return RubricAnalysis.builder()
                            .rubricItem(item)
                            .label(item.getLabel())
                            .averageScore(round(avg))
                            .expertAverageScore(round(expertAvg))
                            .candidateAverageScore(0)
                            .scoreDifference(0)
                            .aiInsight(aiInsight)
                            .build();
                }, aiExecutor))
                .toList();

        return futures.stream()
                .map(future -> {
                    try {
                        return future.join();
                    } catch (Exception e) {
                        log.warn("루브릭별 AI 분석 실패", e);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .toList();
    }

    private <T> CompletableFuture<T> supplySafely(Supplier<T> task, String taskName, T fallback) {
        return CompletableFuture.supplyAsync(task, aiExecutor)
                .exceptionally(throwable -> {
                    log.error("{} 실패, fallback 반환", taskName, throwable);
                    return fallback;
                });
    }

    private Map<String, List<String>> defaultStrengthWeaknesses() {
        Map<String, List<String>> fallback = new HashMap<>();
        fallback.put("strengths", List.of());
        fallback.put("weaknesses", List.of());
        return fallback;
    }

    private List<RubricAnalysis> buildFallbackRubricAnalyses(
            Map<RubricItem, Double> rubricAvg,
            Map<RubricItem, Double> expertRubricAvg) {
        return Arrays.stream(RubricItem.values())
                .map(item -> RubricAnalysis.builder()
                        .rubricItem(item)
                        .label(item.getLabel())
                        .averageScore(round(rubricAvg.getOrDefault(item, 0.0)))
                        .expertAverageScore(round(expertRubricAvg.getOrDefault(item, 0.0)))
                        .candidateAverageScore(0)
                        .scoreDifference(0)
                        .aiInsight("AI 분석 없이 기본 점수 통계만 표시합니다.")
                        .build())
                .toList();
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
