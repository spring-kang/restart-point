package com.restartpoint.domain.project.dto;

import com.restartpoint.domain.profile.entity.JobRole;
import com.restartpoint.domain.project.entity.MemberProgress;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MemberProgressResponse {

    private Long id;
    private Long userId;
    private String userName;
    private JobRole jobRole;
    private String completedTasks;
    private String inProgressTasks;
    private String personalBlockers;
    private Integer contributionPercentage;

    public static MemberProgressResponse from(MemberProgress progress) {
        return MemberProgressResponse.builder()
                .id(progress.getId())
                .userId(progress.getUser().getId())
                .userName(progress.getUser().getName())
                .jobRole(progress.getJobRole())
                .completedTasks(progress.getCompletedTasks())
                .inProgressTasks(progress.getInProgressTasks())
                .personalBlockers(progress.getPersonalBlockers())
                .contributionPercentage(progress.getContributionPercentage())
                .build();
    }
}
