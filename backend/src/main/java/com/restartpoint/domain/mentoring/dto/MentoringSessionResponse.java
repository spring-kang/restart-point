package com.restartpoint.domain.mentoring.dto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.restartpoint.domain.mentoring.entity.MentoringSession;
import com.restartpoint.domain.mentoring.entity.MentoringSession.SessionStatus;
import com.restartpoint.domain.profile.entity.JobRole;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Getter
@Builder
public class MentoringSessionResponse {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private Long id;
    private Long menteeUserId;
    private String menteeName;
    private Long moduleId;
    private Integer weekNumber;
    private String moduleTitle;
    private JobRole jobRole;
    private Long mentorId;
    private String mentorName;
    private SessionStatus status;
    private LocalDateTime sessionDate;
    private String sessionNotes;
    private String questions;
    private List<Integer> completedTaskIndexes;
    private String mentorFeedback;
    private Integer feedbackScore;
    private String nextSteps;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;

    public static MentoringSessionResponse from(MentoringSession session) {
        return MentoringSessionResponse.builder()
                .id(session.getId())
                .menteeUserId(session.getMentee().getUser().getId())
                .menteeName(session.getMentee().getUser().getName())
                .moduleId(session.getModule().getId())
                .weekNumber(session.getModule().getWeekNumber())
                .moduleTitle(session.getModule().getTitle())
                .jobRole(session.getModule().getMentoring().getJobRole())
                .mentorId(session.getMentor() != null ? session.getMentor().getId() : null)
                .mentorName(session.getMentor() != null ? session.getMentor().getName() : null)
                .status(session.getStatus())
                .sessionDate(session.getSessionDate())
                .sessionNotes(session.getSessionNotes())
                .questions(session.getQuestions())
                .completedTaskIndexes(parseJsonList(session.getCompletedTasks()))
                .mentorFeedback(session.getMentorFeedback())
                .feedbackScore(session.getFeedbackScore())
                .nextSteps(session.getNextSteps())
                .completedAt(session.getCompletedAt())
                .createdAt(session.getCreatedAt())
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
