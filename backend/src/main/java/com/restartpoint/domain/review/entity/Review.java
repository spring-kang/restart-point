package com.restartpoint.domain.review.entity;

import com.restartpoint.domain.project.entity.Project;
import com.restartpoint.domain.user.entity.User;
import com.restartpoint.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 프로젝트 심사 엔티티
 */
@Entity
@Table(name = "reviews", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"project_id", "reviewer_id"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Review extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewer_id", nullable = false)
    private User reviewer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReviewType reviewType;

    @Column(length = 2000)
    private String overallComment;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @OneToMany(mappedBy = "review", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReviewScore> scores = new ArrayList<>();

    @Builder
    public Review(Project project, User reviewer, ReviewType reviewType, String overallComment) {
        this.project = project;
        this.reviewer = reviewer;
        this.reviewType = reviewType;
        this.overallComment = overallComment;
        this.submittedAt = LocalDateTime.now();
    }

    public void addScore(ReviewScore score) {
        this.scores.add(score);
        score.setReview(this);
    }

    /**
     * 전체 평균 점수 계산
     */
    public double calculateAverageScore() {
        if (scores.isEmpty()) {
            return 0.0;
        }
        return scores.stream()
                .mapToInt(ReviewScore::getScore)
                .average()
                .orElse(0.0);
    }

    /**
     * 전체 점수 합계
     */
    public int calculateTotalScore() {
        return scores.stream()
                .mapToInt(ReviewScore::getScore)
                .sum();
    }
}
