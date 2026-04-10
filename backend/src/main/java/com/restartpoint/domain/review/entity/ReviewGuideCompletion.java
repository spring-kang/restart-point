package com.restartpoint.domain.review.entity;

import com.restartpoint.domain.user.entity.User;
import com.restartpoint.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * 심사 가이드 학습 완료 기록
 * 다음 기수 참여자가 심사 전에 루브릭 학습을 완료했는지 추적
 */
@Entity
@Table(name = "review_guide_completions", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ReviewGuideCompletion extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // 각 섹션별 학습 완료 여부
    @Builder.Default
    private boolean rubricLearningCompleted = false;

    @Builder.Default
    private boolean exampleComparisonCompleted = false;

    @Builder.Default
    private boolean practiceEvaluationCompleted = false;

    // 전체 가이드 완료 여부
    @Builder.Default
    private boolean fullyCompleted = false;

    public void completeRubricLearning() {
        this.rubricLearningCompleted = true;
        checkFullCompletion();
    }

    public void completeExampleComparison() {
        this.exampleComparisonCompleted = true;
        checkFullCompletion();
    }

    public void completePracticeEvaluation() {
        this.practiceEvaluationCompleted = true;
        checkFullCompletion();
    }

    private void checkFullCompletion() {
        this.fullyCompleted = rubricLearningCompleted && exampleComparisonCompleted && practiceEvaluationCompleted;
    }

    public void completeAll() {
        this.rubricLearningCompleted = true;
        this.exampleComparisonCompleted = true;
        this.practiceEvaluationCompleted = true;
        this.fullyCompleted = true;
    }
}
