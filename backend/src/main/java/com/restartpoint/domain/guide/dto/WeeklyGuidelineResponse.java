package com.restartpoint.domain.guide.dto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.restartpoint.domain.guide.entity.WeeklyGuideline;
import com.restartpoint.domain.profile.entity.JobRole;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Getter
@Builder
public class WeeklyGuidelineResponse {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private Long id;
    private Long templateId;
    private Integer weekNumber;
    private String title;
    private String description;
    private String keyObjectives;
    private List<String> milestones;
    private List<String> recommendedActions;
    private String guideContent;
    private JobRole focusRole;
    private List<String> checklistItems;
    private List<String> referenceLinks;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static WeeklyGuidelineResponse from(WeeklyGuideline guideline) {
        return WeeklyGuidelineResponse.builder()
                .id(guideline.getId())
                .templateId(guideline.getTemplate().getId())
                .weekNumber(guideline.getWeekNumber())
                .title(guideline.getTitle())
                .description(guideline.getDescription())
                .keyObjectives(guideline.getKeyObjectives())
                .milestones(parseJsonList(guideline.getMilestones()))
                .recommendedActions(parseJsonList(guideline.getRecommendedActions()))
                .guideContent(guideline.getGuideContent())
                .focusRole(guideline.getFocusRole())
                .checklistItems(parseJsonList(guideline.getChecklistItems()))
                .referenceLinks(parseJsonList(guideline.getReferenceLinks()))
                .createdAt(guideline.getCreatedAt())
                .updatedAt(guideline.getUpdatedAt())
                .build();
    }

    private static List<String> parseJsonList(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException e) {
            return Collections.emptyList();
        }
    }
}
