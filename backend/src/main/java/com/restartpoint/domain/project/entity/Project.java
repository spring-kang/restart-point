package com.restartpoint.domain.project.entity;

import com.restartpoint.domain.team.entity.Team;
import com.restartpoint.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "projects")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Project extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false, unique = true)
    private Team team;

    @Column(nullable = false)
    private String name;

    // 문제 정의
    @Column(name = "problem_definition", length = 2000)
    private String problemDefinition;

    // 타깃 사용자
    @Column(name = "target_users", length = 1000)
    private String targetUsers;

    // 핵심 솔루션
    @Column(length = 2000)
    private String solution;

    // AI 활용 방식
    @Column(name = "ai_usage", length = 1000)
    private String aiUsage;

    // 외부 링크
    @Column(name = "figma_url")
    private String figmaUrl;

    @Column(name = "github_url")
    private String githubUrl;

    @Column(name = "notion_url")
    private String notionUrl;

    @Column(name = "demo_url")
    private String demoUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProjectStatus status;

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("weekNumber ASC")
    private List<Checkpoint> checkpoints = new ArrayList<>();

    // 팀 회고 (최종 제출 시)
    @Column(name = "team_retrospective", length = 3000)
    private String teamRetrospective;

    @Builder
    public Project(Team team, String name, String problemDefinition, String targetUsers,
                   String solution, String aiUsage, String figmaUrl, String githubUrl,
                   String notionUrl, String demoUrl) {
        this.team = team;
        this.name = name;
        this.problemDefinition = problemDefinition;
        this.targetUsers = targetUsers;
        this.solution = solution;
        this.aiUsage = aiUsage;
        this.figmaUrl = figmaUrl;
        this.githubUrl = githubUrl;
        this.notionUrl = notionUrl;
        this.demoUrl = demoUrl;
        this.status = ProjectStatus.DRAFT;
    }

    public void update(String name, String problemDefinition, String targetUsers,
                       String solution, String aiUsage, String figmaUrl, String githubUrl,
                       String notionUrl, String demoUrl) {
        this.name = name;
        this.problemDefinition = problemDefinition;
        this.targetUsers = targetUsers;
        this.solution = solution;
        this.aiUsage = aiUsage;
        this.figmaUrl = figmaUrl;
        this.githubUrl = githubUrl;
        this.notionUrl = notionUrl;
        this.demoUrl = demoUrl;
    }

    public void startProject() {
        this.status = ProjectStatus.IN_PROGRESS;
    }

    public void submit(String teamRetrospective) {
        this.teamRetrospective = teamRetrospective;
        this.status = ProjectStatus.SUBMITTED;
    }

    public void complete() {
        this.status = ProjectStatus.COMPLETED;
    }

    public void addCheckpoint(Checkpoint checkpoint) {
        this.checkpoints.add(checkpoint);
    }
}
