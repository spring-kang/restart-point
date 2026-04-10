package com.restartpoint.domain.review.entity;

import com.restartpoint.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 루브릭별 심사 점수 엔티티
 */
@Entity
@Table(name = "review_scores")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReviewScore extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id", nullable = false)
    private Review review;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RubricItem rubricItem;

    @Column(nullable = false)
    private int score;  // 1-5점

    @Column(length = 500)
    private String comment;

    @Builder
    public ReviewScore(RubricItem rubricItem, int score, String comment) {
        this.rubricItem = rubricItem;
        this.score = validateScore(score);
        this.comment = comment;
    }

    private int validateScore(int score) {
        if (score < 1 || score > 5) {
            throw new IllegalArgumentException("점수는 1~5 사이여야 합니다.");
        }
        return score;
    }
}
