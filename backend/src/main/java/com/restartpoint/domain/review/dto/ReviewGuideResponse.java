package com.restartpoint.domain.review.dto;

import com.restartpoint.domain.review.entity.RubricItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 심사 가이드 응답 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewGuideResponse {

    private List<RubricGuide> rubricGuides;
    private List<ExampleComparison> exampleComparisons;
    private ReviewGuideStatus completionStatus;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RubricGuide {
        private RubricItem rubricItem;
        private String label;
        private String description;
        private String evaluationTips;
        private List<ScoreExample> scoreExamples;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ScoreExample {
        private int score;
        private String description;
        private String example;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ExampleComparison {
        private String category;
        private ExampleProject excellentExample;
        private ExampleProject averageExample;
        private String comparisonNotes;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ExampleProject {
        private String name;
        private String problemDefinition;
        private String solution;
        private String aiUsage;
        private int expectedScore;
        private String reasonForScore;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ReviewGuideStatus {
        private boolean rubricLearningCompleted;
        private boolean exampleComparisonCompleted;
        private boolean practiceEvaluationCompleted;
        private boolean fullyCompleted;
    }
}
