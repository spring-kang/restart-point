package com.restartpoint.domain.review.dto;

import com.restartpoint.domain.review.entity.RubricItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 심사 패턴 분석 응답 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewPatternAnalysisResponse {

    private int totalReviewCount;
    private double averageScore;
    private Map<RubricItem, Double> rubricAverages;

    // AI 분석 결과
    private String overallTendency;
    private String strengths;
    private String areasForImprovement;
    private String comparisonWithExperts;
    private String recommendations;

    // 세부 통계
    private ScoreDistribution scoreDistribution;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ScoreDistribution {
        private int score1Count;
        private int score2Count;
        private int score3Count;
        private int score4Count;
        private int score5Count;
    }
}
