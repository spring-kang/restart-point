package com.restartpoint.domain.review.dto;

import com.restartpoint.domain.review.entity.RubricItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * AI 심사 보조 분석 결과 DTO
 * 운영자가 심사 데이터를 검토하는데 활용
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewAnalysisResponse {

    private Long projectId;
    private String projectName;
    private String teamName;

    // 심사 통계
    private int totalReviewCount;
    private int expertReviewCount;
    private int candidateReviewCount;

    // 점수 요약
    private double overallAverageScore;
    private double expertAverageScore;
    private double candidateAverageScore;
    private double scoreDifference; // 현직자 - 예비참여자 점수 차이

    // 루브릭별 분석
    private List<RubricAnalysis> rubricAnalyses;

    // AI 분석 결과
    private String commentSummary;          // 심사 코멘트 종합 요약
    private List<String> strengths;         // 주요 강점
    private List<String> weaknesses;        // 주요 약점
    private List<OutlierScore> outliers;    // 이상치 점수
    private String expertVsCandidateAnalysis; // 현직자 vs 예비참여자 분석

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RubricAnalysis {
        private RubricItem rubricItem;
        private String label;
        private double averageScore;
        private double expertAverageScore;
        private double candidateAverageScore;
        private double scoreDifference;
        private String aiInsight; // AI가 분석한 해당 항목 인사이트
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OutlierScore {
        private Long reviewId;
        private String reviewerName;
        private String reviewType; // EXPERT or CANDIDATE
        private RubricItem rubricItem;
        private int score;
        private double averageScore;
        private double deviation; // 평균 대비 편차
        private String possibleReason; // AI가 분석한 이상치 원인
    }
}
