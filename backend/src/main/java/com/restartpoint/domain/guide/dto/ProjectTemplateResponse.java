package com.restartpoint.domain.guide.dto;

import com.restartpoint.domain.guide.entity.ProjectTemplate;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Builder
public class ProjectTemplateResponse {

    private Long id;
    private Long seasonId;
    private String seasonTitle;
    private String title;
    private String description;
    private Integer totalWeeks;
    private Boolean active;
    private Integer guidelineCount;
    private List<WeeklyGuidelineResponse> guidelines;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ProjectTemplateResponse from(ProjectTemplate template) {
        return ProjectTemplateResponse.builder()
                .id(template.getId())
                .seasonId(template.getSeason().getId())
                .seasonTitle(template.getSeason().getTitle())
                .title(template.getTitle())
                .description(template.getDescription())
                .totalWeeks(template.getTotalWeeks())
                .active(template.getActive())
                .guidelineCount(template.getGuidelines().size())
                .createdAt(template.getCreatedAt())
                .updatedAt(template.getUpdatedAt())
                .build();
    }

    public static ProjectTemplateResponse fromWithGuidelines(ProjectTemplate template) {
        return ProjectTemplateResponse.builder()
                .id(template.getId())
                .seasonId(template.getSeason().getId())
                .seasonTitle(template.getSeason().getTitle())
                .title(template.getTitle())
                .description(template.getDescription())
                .totalWeeks(template.getTotalWeeks())
                .active(template.getActive())
                .guidelineCount(template.getGuidelines().size())
                .guidelines(template.getGuidelines().stream()
                        .map(WeeklyGuidelineResponse::from)
                        .collect(Collectors.toList()))
                .createdAt(template.getCreatedAt())
                .updatedAt(template.getUpdatedAt())
                .build();
    }
}
