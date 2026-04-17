package com.restartpoint.domain.mentoring.entity;

import com.restartpoint.domain.team.entity.TeamMember;
import com.restartpoint.domain.user.entity.User;
import com.restartpoint.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 멘토링 세션
 * 실제 진행된 멘토링 기록과 피드백을 저장
 */
@Entity
@Table(name = "mentoring_sessions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MentoringSession extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 멘토링을 받는 팀원
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mentee_id", nullable = false)
    private TeamMember mentee;

    // 해당 주차 모듈
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "module_id", nullable = false)
    private MentoringModule module;

    // 멘토 (EXPERT 역할 사용자, 선택사항 - 셀프 학습 가능)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mentor_id")
    private User mentor;

    // 세션 상태
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SessionStatus status = SessionStatus.NOT_STARTED;

    // 세션 일시
    @Column(name = "session_date")
    private LocalDateTime sessionDate;

    // 세션 메모 (멘티 작성)
    @Column(name = "session_notes", length = 3000)
    private String sessionNotes;

    // 멘토 피드백
    @Column(name = "mentor_feedback", length = 3000)
    private String mentorFeedback;

    // 점수 (1-5)
    @Column(name = "feedback_score")
    private Integer feedbackScore;

    // 완료된 실습 과제 (JSON 배열 - 인덱스)
    @Column(name = "completed_tasks", length = 500)
    private String completedTasks;

    // 질문사항
    @Column(length = 2000)
    private String questions;

    // 다음 학습 계획
    @Column(name = "next_steps", length = 1000)
    private String nextSteps;

    // 완료 일시
    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Builder
    public MentoringSession(TeamMember mentee, MentoringModule module, User mentor) {
        this.mentee = mentee;
        this.module = module;
        this.mentor = mentor;
        this.status = SessionStatus.NOT_STARTED;
    }

    public void startSession() {
        this.status = SessionStatus.IN_PROGRESS;
        this.sessionDate = LocalDateTime.now();
    }

    public void updateNotes(String sessionNotes, String questions, String completedTasks) {
        this.sessionNotes = sessionNotes;
        this.questions = questions;
        this.completedTasks = completedTasks;
    }

    public void provideFeedback(String mentorFeedback, Integer feedbackScore, String nextSteps) {
        this.mentorFeedback = mentorFeedback;
        this.feedbackScore = feedbackScore;
        this.nextSteps = nextSteps;
    }

    public void complete() {
        this.status = SessionStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
    }

    public void assignMentor(User mentor) {
        this.mentor = mentor;
    }

    public enum SessionStatus {
        NOT_STARTED,    // 시작 전
        IN_PROGRESS,    // 진행 중
        PENDING_FEEDBACK, // 피드백 대기
        COMPLETED       // 완료
    }
}
