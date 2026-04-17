package com.restartpoint.domain.guide.dto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.restartpoint.domain.guide.entity.GuidelineCompletion;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Getter
@Builder
public class GuidelineCompletionResponse {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private Long id;
    private Long userId;
    private Long guidelineId;
    private Integer weekNumber;
    private String guidelineTitle;
    private Long checkpointId;
    private Boolean completed;
    private List<Integer> completedChecklistIndexes;
    private String completionNotes;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;

    public static GuidelineCompletionResponse from(GuidelineCompletion completion) {
        return GuidelineCompletionResponse.builder()
                .id(completion.getId())
                .userId(completion.getUser().getId())
                .guidelineId(completion.getGuideline().getId())
                .weekNumber(completion.getGuideline().getWeekNumber())
                .guidelineTitle(completion.getGuideline().getTitle())
                .checkpointId(completion.getCheckpoint() != null ? completion.getCheckpoint().getId() : null)
                .completed(completion.getCompleted())
                .completedChecklistIndexes(parseJsonList(completion.getCompletedChecklist()))
                .completionNotes(completion.getCompletionNotes())
                .completedAt(completion.getCompletedAt())
                .createdAt(completion.getCreatedAt())
                .build();
    }

    private static List<Integer> parseJsonList(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<Integer>>() {});
        } catch (JsonProcessingException e) {
            return Collections.emptyList();
        }
    }
}
