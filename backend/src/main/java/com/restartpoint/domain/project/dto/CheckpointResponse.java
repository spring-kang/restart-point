package com.restartpoint.domain.project.dto;

import com.restartpoint.domain.project.entity.Checkpoint;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Builder
public class CheckpointResponse {

    private Long id;
    private Long projectId;
    private Integer weekNumber;
    private String weeklyGoal;
    private String progressSummary;
    private String blockers;
    private String nextWeekPlan;
    private Long createdById;
    private String createdByName;
    private String aiFeedback;
    private List<MemberProgressResponse> memberProgresses;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static CheckpointResponse from(Checkpoint checkpoint) {
        return CheckpointResponse.builder()
                .id(checkpoint.getId())
                .projectId(checkpoint.getProject().getId())
                .weekNumber(checkpoint.getWeekNumber())
                .weeklyGoal(checkpoint.getWeeklyGoal())
                .progressSummary(checkpoint.getProgressSummary())
                .blockers(checkpoint.getBlockers())
                .nextWeekPlan(checkpoint.getNextWeekPlan())
                .createdById(checkpoint.getCreatedBy() != null ? checkpoint.getCreatedBy().getId() : null)
                .createdByName(checkpoint.getCreatedBy() != null ? checkpoint.getCreatedBy().getName() : null)
                .aiFeedback(checkpoint.getAiFeedback())
                .memberProgresses(checkpoint.getMemberProgresses().stream()
                        .map(MemberProgressResponse::from)
                        .collect(Collectors.toList()))
                .createdAt(checkpoint.getCreatedAt())
                .updatedAt(checkpoint.getUpdatedAt())
                .build();
    }
}
