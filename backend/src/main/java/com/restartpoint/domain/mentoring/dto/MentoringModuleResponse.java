package com.restartpoint.domain.mentoring.dto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.restartpoint.domain.mentoring.entity.MentoringModule;
import com.restartpoint.domain.profile.entity.JobRole;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Getter
@Builder
public class MentoringModuleResponse {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private Long id;
    private Long mentoringId;
    private JobRole jobRole;
    private Integer weekNumber;
    private String title;
    private String description;
    private String learningContent;
    private List<String> keyPoints;
    private List<String> commonMistakes;
    private List<String> practiceTasks;
    private List<String> referenceMaterials;
    private Integer estimatedMinutes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static MentoringModuleResponse from(MentoringModule module) {
        return MentoringModuleResponse.builder()
                .id(module.getId())
                .mentoringId(module.getMentoring().getId())
                .jobRole(module.getMentoring().getJobRole())
                .weekNumber(module.getWeekNumber())
                .title(module.getTitle())
                .description(module.getDescription())
                .learningContent(module.getLearningContent())
                .keyPoints(parseJsonList(module.getKeyPoints()))
                .commonMistakes(parseJsonList(module.getCommonMistakes()))
                .practiceTasks(parseJsonList(module.getPracticeTasks()))
                .referenceMaterials(parseJsonList(module.getReferenceMaterials()))
                .estimatedMinutes(module.getEstimatedMinutes())
                .createdAt(module.getCreatedAt())
                .updatedAt(module.getUpdatedAt())
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
