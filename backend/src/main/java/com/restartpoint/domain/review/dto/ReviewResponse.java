package com.restartpoint.domain.review.dto;

import com.restartpoint.domain.review.entity.Review;
import com.restartpoint.domain.review.entity.ReviewType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class ReviewResponse {

    private Long id;
    private Long projectId;
    private String projectName;
    private Long reviewerId;
    private String reviewerName;
    private ReviewType reviewType;
    private String overallComment;
    private double averageScore;
    private int totalScore;
    private List<ReviewScoreResponse> scores;
    private LocalDateTime submittedAt;

    public static ReviewResponse from(Review review) {
        return ReviewResponse.builder()
                .id(review.getId())
                .projectId(review.getProject().getId())
                .projectName(review.getProject().getName())
                .reviewerId(review.getReviewer().getId())
                .reviewerName(review.getReviewer().getName())
                .reviewType(review.getReviewType())
                .overallComment(review.getOverallComment())
                .averageScore(review.calculateAverageScore())
                .totalScore(review.calculateTotalScore())
                .scores(review.getScores().stream()
                        .map(ReviewScoreResponse::from)
                        .toList())
                .submittedAt(review.getSubmittedAt())
                .build();
    }

    /**
     * 점수 없이 간략한 정보만 반환
     */
    public static ReviewResponse simpleFrom(Review review) {
        return ReviewResponse.builder()
                .id(review.getId())
                .projectId(review.getProject().getId())
                .projectName(review.getProject().getName())
                .reviewerId(review.getReviewer().getId())
                .reviewerName(review.getReviewer().getName())
                .reviewType(review.getReviewType())
                .overallComment(review.getOverallComment())
                .averageScore(review.calculateAverageScore())
                .totalScore(review.calculateTotalScore())
                .submittedAt(review.getSubmittedAt())
                .build();
    }
}
