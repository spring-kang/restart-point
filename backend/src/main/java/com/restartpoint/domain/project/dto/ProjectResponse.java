package com.restartpoint.domain.project.dto;

import com.restartpoint.domain.project.entity.Project;
import com.restartpoint.domain.project.entity.ProjectStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Builder
public class ProjectResponse {

    private Long id;
    private Long teamId;
    private String teamName;
    private String name;
    private String problemDefinition;
    private String targetUsers;
    private String solution;
    private String aiUsage;
    private String figmaUrl;
    private String githubUrl;
    private String notionUrl;
    private String demoUrl;
    private ProjectStatus status;
    private String teamRetrospective;
    private List<CheckpointResponse> checkpoints;
    private Long seasonId;
    private String seasonTitle;
    private Integer featuredRank;
    private LocalDateTime featuredAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ProjectResponse from(Project project) {
        return ProjectResponse.builder()
                .id(project.getId())
                .teamId(project.getTeam().getId())
                .teamName(project.getTeam().getName())
                .seasonId(project.getTeam().getSeason().getId())
                .seasonTitle(project.getTeam().getSeason().getTitle())
                .name(project.getName())
                .problemDefinition(project.getProblemDefinition())
                .targetUsers(project.getTargetUsers())
                .solution(project.getSolution())
                .aiUsage(project.getAiUsage())
                .figmaUrl(project.getFigmaUrl())
                .githubUrl(project.getGithubUrl())
                .notionUrl(project.getNotionUrl())
                .demoUrl(project.getDemoUrl())
                .status(project.getStatus())
                .teamRetrospective(project.getTeamRetrospective())
                .featuredRank(project.getFeaturedRank())
                .featuredAt(project.getFeaturedAt())
                .checkpoints(project.getCheckpoints().stream()
                        .map(CheckpointResponse::from)
                        .collect(Collectors.toList()))
                .createdAt(project.getCreatedAt())
                .updatedAt(project.getUpdatedAt())
                .build();
    }

    public static ProjectResponse simpleFrom(Project project) {
        return ProjectResponse.builder()
                .id(project.getId())
                .teamId(project.getTeam().getId())
                .teamName(project.getTeam().getName())
                .seasonId(project.getTeam().getSeason().getId())
                .seasonTitle(project.getTeam().getSeason().getTitle())
                .name(project.getName())
                .problemDefinition(project.getProblemDefinition())
                .status(project.getStatus())
                .featuredRank(project.getFeaturedRank())
                .featuredAt(project.getFeaturedAt())
                .createdAt(project.getCreatedAt())
                .updatedAt(project.getUpdatedAt())
                .build();
    }

    /**
     * 심사용 프로젝트 응답 (민감 정보 제외)
     * - 제외: teamRetrospective, checkpoints (blockers, aiFeedback, personalBlockers, contributionPercentage 포함)
     * - 포함: 기본 정보, 프로젝트 설명, 외부 링크
     */
    public static ProjectResponse forReview(Project project) {
        return ProjectResponse.builder()
                .id(project.getId())
                .teamId(project.getTeam().getId())
                .teamName(project.getTeam().getName())
                .seasonId(project.getTeam().getSeason().getId())
                .seasonTitle(project.getTeam().getSeason().getTitle())
                .name(project.getName())
                .problemDefinition(project.getProblemDefinition())
                .targetUsers(project.getTargetUsers())
                .solution(project.getSolution())
                .aiUsage(project.getAiUsage())
                .figmaUrl(project.getFigmaUrl())
                .githubUrl(project.getGithubUrl())
                .demoUrl(project.getDemoUrl())
                .status(project.getStatus())
                .featuredRank(project.getFeaturedRank())
                .featuredAt(project.getFeaturedAt())
                .createdAt(project.getCreatedAt())
                .updatedAt(project.getUpdatedAt())
                .build();
    }
}
