package com.restartpoint.domain.review.dto;

import com.restartpoint.domain.review.entity.RubricItem;
import lombok.Builder;
import lombok.Getter;

import java.util.Map;

/**
 * 프로젝트별 심사 집계 결과
 */
@Getter
@Builder
public class ReviewSummaryResponse {

    private Long projectId;
    private String projectName;

    // 전체 심사 통계
    private int totalReviewCount;
    private int expertReviewCount;
    private int candidateReviewCount;

    // 가중 평균 점수 (전문가 평가 100%)
    private double weightedAverageScore;

    // 현직자 평균 점수
    private double expertAverageScore;

    // 예비 참여자 평균 점수
    private double candidateAverageScore;

    // 루브릭별 평균 점수
    private Map<RubricItem, Double> rubricAverages;

    // 루브릭별 현직자 평균 점수
    private Map<RubricItem, Double> expertRubricAverages;

    // 루브릭별 예비 참여자 평균 점수
    private Map<RubricItem, Double> candidateRubricAverages;
}
