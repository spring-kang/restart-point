package com.restartpoint.domain.mentoring.entity;

import com.restartpoint.domain.profile.entity.JobRole;
import com.restartpoint.domain.season.entity.Season;
import com.restartpoint.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 직무별 멘토링 프로그램
 * 시즌별로 각 직무(기획/디자인/프론트엔드/백엔드)에 대한 멘토링 가이드를 정의
 */
@Entity
@Table(name = "job_role_mentorings", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"season_id", "job_role"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class JobRoleMentoring extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "season_id", nullable = false)
    private Season season;

    @Enumerated(EnumType.STRING)
    @Column(name = "job_role", nullable = false)
    private JobRole jobRole;

    @Column(nullable = false)
    private String title;

    @Column(length = 2000)
    private String description;

    // 멘토링 목표
    @Column(name = "learning_objectives", length = 2000)
    private String learningObjectives;

    // 활성화 여부
    @Column(nullable = false)
    private Boolean active = true;

    @OneToMany(mappedBy = "mentoring", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("weekNumber ASC")
    private List<MentoringModule> modules = new ArrayList<>();

    @Builder
    public JobRoleMentoring(Season season, JobRole jobRole, String title,
                            String description, String learningObjectives) {
        this.season = season;
        this.jobRole = jobRole;
        this.title = title;
        this.description = description;
        this.learningObjectives = learningObjectives;
        this.active = true;
    }

    public void update(String title, String description, String learningObjectives) {
        this.title = title;
        this.description = description;
        this.learningObjectives = learningObjectives;
    }

    public void activate() {
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
    }

    public void addModule(MentoringModule module) {
        this.modules.add(module);
    }
}
