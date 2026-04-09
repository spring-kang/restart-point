package com.restartpoint.domain.project.entity;

import com.restartpoint.domain.profile.entity.JobRole;
import com.restartpoint.domain.user.entity.User;
import com.restartpoint.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "member_progresses")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberProgress extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "checkpoint_id", nullable = false)
    private Checkpoint checkpoint;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "job_role", nullable = false)
    private JobRole jobRole;

    // 이번 주 완료한 작업
    @Column(name = "completed_tasks", length = 1000)
    private String completedTasks;

    // 진행 중인 작업
    @Column(name = "in_progress_tasks", length = 1000)
    private String inProgressTasks;

    // 개인 막힘 요소
    @Column(name = "personal_blockers", length = 500)
    private String personalBlockers;

    // 기여도 (%)
    @Column(name = "contribution_percentage")
    private Integer contributionPercentage;

    @Builder
    public MemberProgress(Checkpoint checkpoint, User user, JobRole jobRole,
                          String completedTasks, String inProgressTasks,
                          String personalBlockers, Integer contributionPercentage) {
        this.checkpoint = checkpoint;
        this.user = user;
        this.jobRole = jobRole;
        this.completedTasks = completedTasks;
        this.inProgressTasks = inProgressTasks;
        this.personalBlockers = personalBlockers;
        this.contributionPercentage = contributionPercentage;
    }

    public void update(String completedTasks, String inProgressTasks,
                       String personalBlockers, Integer contributionPercentage) {
        this.completedTasks = completedTasks;
        this.inProgressTasks = inProgressTasks;
        this.personalBlockers = personalBlockers;
        this.contributionPercentage = contributionPercentage;
    }
}
