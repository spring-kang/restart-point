package com.restartpoint.domain.profile.entity;

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
@Table(name = "profiles")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Profile extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JobRole jobRole;

    @ElementCollection
    @CollectionTable(name = "profile_tech_stacks", joinColumns = @JoinColumn(name = "profile_id"))
    @Column(name = "tech_stack")
    private List<String> techStacks = new ArrayList<>();

    @Column(name = "portfolio_url")
    private String portfolioUrl;

    @ElementCollection
    @CollectionTable(name = "profile_domains", joinColumns = @JoinColumn(name = "profile_id"))
    @Column(name = "domain")
    private List<String> interestedDomains = new ArrayList<>();

    @Column(name = "available_hours_per_week")
    private Integer availableHoursPerWeek;

    @Enumerated(EnumType.STRING)
    @Column(name = "collaboration_style")
    private CollaborationStyle collaborationStyle;

    @Column(name = "improvement_goal", length = 500)
    private String improvementGoal;

    @Enumerated(EnumType.STRING)
    @Column(name = "preferred_difficulty")
    private ProjectDifficulty preferredDifficulty;

    @Column(length = 1000)
    private String introduction;

    @Builder
    public Profile(User user, JobRole jobRole, List<String> techStacks,
                   String portfolioUrl, List<String> interestedDomains,
                   Integer availableHoursPerWeek, CollaborationStyle collaborationStyle,
                   String improvementGoal, ProjectDifficulty preferredDifficulty,
                   String introduction) {
        this.user = user;
        this.jobRole = jobRole;
        this.techStacks = techStacks != null ? techStacks : new ArrayList<>();
        this.portfolioUrl = portfolioUrl;
        this.interestedDomains = interestedDomains != null ? interestedDomains : new ArrayList<>();
        this.availableHoursPerWeek = availableHoursPerWeek;
        this.collaborationStyle = collaborationStyle;
        this.improvementGoal = improvementGoal;
        this.preferredDifficulty = preferredDifficulty;
        this.introduction = introduction;
    }

    public void update(JobRole jobRole, List<String> techStacks,
                       String portfolioUrl, List<String> interestedDomains,
                       Integer availableHoursPerWeek, CollaborationStyle collaborationStyle,
                       String improvementGoal, ProjectDifficulty preferredDifficulty,
                       String introduction) {
        this.jobRole = jobRole;
        this.techStacks = techStacks != null ? techStacks : new ArrayList<>();
        this.portfolioUrl = portfolioUrl;
        this.interestedDomains = interestedDomains != null ? interestedDomains : new ArrayList<>();
        this.availableHoursPerWeek = availableHoursPerWeek;
        this.collaborationStyle = collaborationStyle;
        this.improvementGoal = improvementGoal;
        this.preferredDifficulty = preferredDifficulty;
        this.introduction = introduction;
    }

    /**
     * 프로필 완성 여부를 확인합니다.
     * 팀 참여를 위한 최소 조건: 직무, 기술 스택(1개 이상), 자기소개(50자 이상)
     */
    public boolean isComplete() {
        return this.jobRole != null
                && this.techStacks != null && !this.techStacks.isEmpty()
                && this.introduction != null && this.introduction.length() >= 50;
    }
}
