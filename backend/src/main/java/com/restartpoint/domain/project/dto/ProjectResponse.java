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
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ProjectResponse from(Project project) {
        return ProjectResponse.builder()
                .id(project.getId())
                .teamId(project.getTeam().getId())
                .teamName(project.getTeam().getName())
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
                .name(project.getName())
                .problemDefinition(project.getProblemDefinition())
                .status(project.getStatus())
                .createdAt(project.getCreatedAt())
                .updatedAt(project.getUpdatedAt())
                .build();
    }
}
