package com.restartpoint.domain.guide.entity;

import com.restartpoint.domain.project.entity.Checkpoint;
import com.restartpoint.domain.user.entity.User;
import com.restartpoint.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 가이드라인 완료 추적
 * 사용자별로 각 주차 가이드의 완료 상태를 추적
 */
@Entity
@Table(name = "guideline_completions", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "guideline_id"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GuidelineCompletion extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "guideline_id", nullable = false)
    private WeeklyGuideline guideline;

    // 연관된 체크포인트 (선택사항 - 체크포인트 작성 시 자동 완료 처리 가능)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "checkpoint_id")
    private Checkpoint checkpoint;

    // 완료 여부
    @Column(nullable = false)
    private Boolean completed = false;

    // 완료된 체크리스트 항목 (JSON 배열 - 체크리스트 인덱스)
    @Column(name = "completed_checklist", length = 500)
    private String completedChecklist;

    // 완료 메모
    @Column(name = "completion_notes", length = 1000)
    private String completionNotes;

    // 완료 일시
    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Builder
    public GuidelineCompletion(User user, WeeklyGuideline guideline, Checkpoint checkpoint) {
        this.user = user;
        this.guideline = guideline;
        this.checkpoint = checkpoint;
        this.completed = false;
    }

    public void markCompleted(String completionNotes) {
        this.completed = true;
        this.completionNotes = completionNotes;
        this.completedAt = LocalDateTime.now();
    }

    public void markIncomplete() {
        this.completed = false;
        this.completedAt = null;
    }

    public void updateCompletedChecklist(String completedChecklist) {
        this.completedChecklist = completedChecklist;
    }

    public void linkCheckpoint(Checkpoint checkpoint) {
        this.checkpoint = checkpoint;
    }
}
