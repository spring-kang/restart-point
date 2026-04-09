package com.restartpoint.domain.project.entity;

import com.restartpoint.domain.user.entity.User;
import com.restartpoint.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "checkpoints")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Checkpoint extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    // 주차 번호
    @Column(name = "week_number", nullable = false)
    private Integer weekNumber;

    // 이번 주 목표
    @Column(name = "weekly_goal", length = 1000)
    private String weeklyGoal;

    // 전체 진행 상황 요약
    @Column(name = "progress_summary", length = 2000)
    private String progressSummary;

    // 막힘 요소
    @Column(length = 1000)
    private String blockers;

    // 다음 주 계획
    @Column(name = "next_week_plan", length = 1000)
    private String nextWeekPlan;

    // 작성자
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    // 역할별 진행 상황
    @OneToMany(mappedBy = "checkpoint", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MemberProgress> memberProgresses = new ArrayList<>();

    // AI 피드백 (FR-07에서 사용)
    @Column(name = "ai_feedback", length = 3000)
    private String aiFeedback;

    @Builder
    public Checkpoint(Project project, Integer weekNumber, String weeklyGoal,
                      String progressSummary, String blockers, String nextWeekPlan,
                      User createdBy) {
        this.project = project;
        this.weekNumber = weekNumber;
        this.weeklyGoal = weeklyGoal;
        this.progressSummary = progressSummary;
        this.blockers = blockers;
        this.nextWeekPlan = nextWeekPlan;
        this.createdBy = createdBy;
    }

    public void update(String weeklyGoal, String progressSummary, String blockers,
                       String nextWeekPlan) {
        this.weeklyGoal = weeklyGoal;
        this.progressSummary = progressSummary;
        this.blockers = blockers;
        this.nextWeekPlan = nextWeekPlan;
    }

    public void setAiFeedback(String aiFeedback) {
        this.aiFeedback = aiFeedback;
    }

    public void addMemberProgress(MemberProgress progress) {
        this.memberProgresses.add(progress);
    }
}
