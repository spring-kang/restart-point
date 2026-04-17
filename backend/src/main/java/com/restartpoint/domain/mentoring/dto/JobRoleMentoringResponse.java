package com.restartpoint.domain.mentoring.dto;

import com.restartpoint.domain.mentoring.entity.JobRoleMentoring;
import com.restartpoint.domain.profile.entity.JobRole;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Builder
public class JobRoleMentoringResponse {

    private Long id;
    private Long seasonId;
    private String seasonTitle;
    private JobRole jobRole;
    private String title;
    private String description;
    private String learningObjectives;
    private Boolean active;
    private Integer moduleCount;
    private List<MentoringModuleResponse> modules;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static JobRoleMentoringResponse from(JobRoleMentoring mentoring) {
        return JobRoleMentoringResponse.builder()
                .id(mentoring.getId())
                .seasonId(mentoring.getSeason().getId())
                .seasonTitle(mentoring.getSeason().getTitle())
                .jobRole(mentoring.getJobRole())
                .title(mentoring.getTitle())
                .description(mentoring.getDescription())
                .learningObjectives(mentoring.getLearningObjectives())
                .active(mentoring.getActive())
                .moduleCount(mentoring.getModules().size())
                .createdAt(mentoring.getCreatedAt())
                .updatedAt(mentoring.getUpdatedAt())
                .build();
    }

    public static JobRoleMentoringResponse fromWithModules(JobRoleMentoring mentoring) {
        return JobRoleMentoringResponse.builder()
                .id(mentoring.getId())
                .seasonId(mentoring.getSeason().getId())
                .seasonTitle(mentoring.getSeason().getTitle())
                .jobRole(mentoring.getJobRole())
                .title(mentoring.getTitle())
                .description(mentoring.getDescription())
                .learningObjectives(mentoring.getLearningObjectives())
                .active(mentoring.getActive())
                .moduleCount(mentoring.getModules().size())
                .modules(mentoring.getModules().stream()
                        .map(MentoringModuleResponse::from)
                        .collect(Collectors.toList()))
                .createdAt(mentoring.getCreatedAt())
                .updatedAt(mentoring.getUpdatedAt())
                .build();
    }
}
