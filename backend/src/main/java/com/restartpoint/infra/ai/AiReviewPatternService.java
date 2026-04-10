package com.restartpoint.infra.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.restartpoint.domain.review.dto.ReviewPatternAnalysisResponse;
import com.restartpoint.domain.review.dto.ReviewPatternAnalysisResponse.ScoreDistribution;
import com.restartpoint.domain.review.entity.Review;
import com.restartpoint.domain.review.entity.ReviewScore;
import com.restartpoint.domain.review.entity.ReviewType;
import com.restartpoint.domain.review.entity.RubricItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * AI 기반 심사 패턴 분석 서비스
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AiReviewPatternService {

    private final GroqService groqService;
    private final ObjectMapper objectMapper;

    private static final String SYSTEM_PROMPT = """
        당신은 프로젝트 심사 패턴을 분석하는 교육 전문가입니다.
        사용자의 심사 데이터를 분석하여 평가 경향과 개선점을 제공합니다.

        분석 시 다음 사항을 고려하세요:
        1. 점수 분포의 균형 (너무 관대하거나 엄격하지 않은지)
        2. 루브릭 항목별 평가 일관성
        3. 전문가 심사자와의 차이점
        4. 평가 근거의 구체성

        응답은 반드시 다음 JSON 형식으로 제공하세요:
        {
            "overallTendency": "전반적인 평가 경향 설명 (1-2문장)",
            "strengths": "평가에서 잘하는 점 (2-3개 항목)",
            "areasForImprovement": "개선할 수 있는 점 (2-3개 항목)",
            "comparisonWithExperts": "전문가 심사와 비교한 분석",
            "recommendations": "다음 심사에서 참고할 조언"
        }
        """;

    /**
     * 사용자의 심사 패턴을 분석합니다.
     */
    public ReviewPatternAnalysisResponse analyzePattern(
            List<Review> userReviews,
            Map<RubricItem, Double> expertAverages) {

        if (userReviews.isEmpty()) {
            return buildEmptyAnalysis();
        }

        // 통계 계산
        int totalReviewCount = userReviews.size();
        double averageScore = calculateAverageScore(userReviews);
        Map<RubricItem, Double> rubricAverages = calculateRubricAverages(userReviews);
        ScoreDistribution scoreDistribution = calculateScoreDistribution(userReviews);

        // AI 분석 요청
        String analysisPrompt = buildAnalysisPrompt(userReviews, rubricAverages, expertAverages);
        String aiResponse = groqService.chat(SYSTEM_PROMPT, analysisPrompt);

        // AI 응답 파싱
        Map<String, String> aiAnalysis = parseAiResponse(aiResponse);

        return ReviewPatternAnalysisResponse.builder()
                .totalReviewCount(totalReviewCount)
                .averageScore(Math.round(averageScore * 100.0) / 100.0)
                .rubricAverages(rubricAverages)
                .overallTendency(aiAnalysis.getOrDefault("overallTendency", "분석 중입니다."))
                .strengths(aiAnalysis.getOrDefault("strengths", ""))
                .areasForImprovement(aiAnalysis.getOrDefault("areasForImprovement", ""))
                .comparisonWithExperts(aiAnalysis.getOrDefault("comparisonWithExperts", ""))
                .recommendations(aiAnalysis.getOrDefault("recommendations", ""))
                .scoreDistribution(scoreDistribution)
                .build();
    }

    private String buildAnalysisPrompt(
            List<Review> reviews,
            Map<RubricItem, Double> userAverages,
            Map<RubricItem, Double> expertAverages) {

        StringBuilder prompt = new StringBuilder();
        prompt.append("다음은 한 사용자의 프로젝트 심사 데이터입니다.\n\n");

        // 심사 통계
        prompt.append("## 심사 통계\n");
        prompt.append(String.format("- 총 심사 횟수: %d개 프로젝트\n", reviews.size()));
        prompt.append(String.format("- 평균 점수: %.2f점\n\n", calculateAverageScore(reviews)));

        // 루브릭별 평균 점수
        prompt.append("## 루브릭별 평균 점수 (사용자 vs 전문가)\n");
        for (RubricItem item : RubricItem.values()) {
            double userAvg = userAverages.getOrDefault(item, 0.0);
            double expertAvg = expertAverages.getOrDefault(item, 0.0);
            double diff = userAvg - expertAvg;
            String diffSign = diff >= 0 ? "+" : "";
            prompt.append(String.format("- %s: 사용자 %.2f점, 전문가 %.2f점 (차이: %s%.2f)\n",
                    item.getLabel(), userAvg, expertAvg, diffSign, diff));
        }
        prompt.append("\n");

        // 점수 분포
        ScoreDistribution dist = calculateScoreDistribution(reviews);
        prompt.append("## 점수 분포\n");
        prompt.append(String.format("- 1점: %d회, 2점: %d회, 3점: %d회, 4점: %d회, 5점: %d회\n\n",
                dist.getScore1Count(), dist.getScore2Count(), dist.getScore3Count(),
                dist.getScore4Count(), dist.getScore5Count()));

        // 최근 심사 코멘트 샘플 (최대 3개)
        prompt.append("## 최근 심사 코멘트 샘플\n");
        int commentCount = 0;
        for (Review review : reviews) {
            if (review.getOverallComment() != null && !review.getOverallComment().isBlank()) {
                prompt.append(String.format("- \"%s\"\n", truncate(review.getOverallComment(), 100)));
                commentCount++;
                if (commentCount >= 3) break;
            }
        }
        if (commentCount == 0) {
            prompt.append("- (코멘트 없음)\n");
        }

        prompt.append("\n위 데이터를 분석하여 사용자의 심사 패턴에 대한 피드백을 제공해주세요.");

        return prompt.toString();
    }

    private Map<String, String> parseAiResponse(String response) {
        Map<String, String> result = new HashMap<>();

        if (response == null || response.isBlank()) {
            return result;
        }

        try {
            // JSON 추출 시도
            int jsonStart = response.indexOf('{');
            int jsonEnd = response.lastIndexOf('}');
            if (jsonStart >= 0 && jsonEnd > jsonStart) {
                String jsonStr = response.substring(jsonStart, jsonEnd + 1);
                Map<String, Object> parsed = objectMapper.readValue(jsonStr, Map.class);
                for (Map.Entry<String, Object> entry : parsed.entrySet()) {
                    if (entry.getValue() != null) {
                        result.put(entry.getKey(), entry.getValue().toString());
                    }
                }
            }
        } catch (Exception e) {
            log.warn("AI 응답 파싱 실패, 기본값 사용: {}", e.getMessage());
            result.put("overallTendency", response);
        }

        return result;
    }

    private double calculateAverageScore(List<Review> reviews) {
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
            averages.put(item, Math.round(avg * 100.0) / 100.0);
        }

        return averages;
    }

    private ScoreDistribution calculateScoreDistribution(List<Review> reviews) {
        int[] counts = new int[6]; // index 1-5 사용

        for (Review review : reviews) {
            for (ReviewScore score : review.getScores()) {
                int s = score.getScore();
                if (s >= 1 && s <= 5) {
                    counts[s]++;
                }
            }
        }

        return ScoreDistribution.builder()
                .score1Count(counts[1])
                .score2Count(counts[2])
                .score3Count(counts[3])
                .score4Count(counts[4])
                .score5Count(counts[5])
                .build();
    }

    private ReviewPatternAnalysisResponse buildEmptyAnalysis() {
        return ReviewPatternAnalysisResponse.builder()
                .totalReviewCount(0)
                .averageScore(0.0)
                .rubricAverages(new EnumMap<>(RubricItem.class))
                .overallTendency("아직 심사 기록이 없습니다.")
                .strengths("")
                .areasForImprovement("")
                .comparisonWithExperts("")
                .recommendations("프로젝트 심사에 참여하면 평가 패턴 분석을 받을 수 있습니다.")
                .scoreDistribution(ScoreDistribution.builder().build())
                .build();
    }

    private String truncate(String text, int maxLength) {
        if (text == null) return "";
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength) + "...";
    }
}
