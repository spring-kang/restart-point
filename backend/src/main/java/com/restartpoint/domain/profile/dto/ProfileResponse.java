package com.restartpoint.domain.profile.dto;

import com.restartpoint.domain.profile.entity.CollaborationStyle;
import com.restartpoint.domain.profile.entity.JobRole;
import com.restartpoint.domain.profile.entity.Profile;
import com.restartpoint.domain.profile.entity.ProjectDifficulty;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class ProfileResponse {

    private Long id;
    private Long userId;
    private String userName;
    private JobRole jobRole;
    private List<String> techStacks;
    private String portfolioUrl;
    private List<String> interestedDomains;
    private Integer availableHoursPerWeek;
    private CollaborationStyle collaborationStyle;
    private String improvementGoal;
    private ProjectDifficulty preferredDifficulty;
    private String introduction;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ProfileResponse from(Profile profile) {
        return ProfileResponse.builder()
                .id(profile.getId())
                .userId(profile.getUser().getId())
                .userName(profile.getUser().getName())
                .jobRole(profile.getJobRole())
                .techStacks(profile.getTechStacks())
                .portfolioUrl(profile.getPortfolioUrl())
                .interestedDomains(profile.getInterestedDomains())
                .availableHoursPerWeek(profile.getAvailableHoursPerWeek())
                .collaborationStyle(profile.getCollaborationStyle())
                .improvementGoal(profile.getImprovementGoal())
                .preferredDifficulty(profile.getPreferredDifficulty())
                .introduction(profile.getIntroduction())
                .createdAt(profile.getCreatedAt())
                .updatedAt(profile.getUpdatedAt())
                .build();
    }
}
