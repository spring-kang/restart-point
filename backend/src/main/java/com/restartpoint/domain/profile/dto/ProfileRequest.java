package com.restartpoint.domain.profile.dto;

import com.restartpoint.domain.profile.entity.CollaborationStyle;
import com.restartpoint.domain.profile.entity.JobRole;
import com.restartpoint.domain.profile.entity.ProjectDifficulty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class ProfileRequest {

    @NotNull(message = "역할은 필수입니다.")
    private JobRole jobRole;

    private List<String> techStacks;

    private String portfolioUrl;

    private List<String> interestedDomains;

    private Integer availableHoursPerWeek;

    private CollaborationStyle collaborationStyle;

    private String improvementGoal;

    private ProjectDifficulty preferredDifficulty;

    private String introduction;
}
