package com.restartpoint.domain.mentoring.dto;

import com.restartpoint.domain.mentoring.entity.MentoringSession.SessionStatus;
import com.restartpoint.domain.profile.entity.JobRole;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 사용자의 멘토링 진행 현황
 */
@Getter
@Builder
public class MentoringProgressResponse {

    private Long userId;
    private Long seasonId;
    private JobRole jobRole;
    private String mentoringTitle;
    private Integer totalModules;
    private Integer completedModules;
    private Integer inProgressModules;
    private Double progressPercentage;
    private Double averageFeedbackScore;
    private List<ModuleProgress> moduleProgress;

    @Getter
    @Builder
    public static class ModuleProgress {
        private Long moduleId;
        private Integer weekNumber;
        private String title;
        private SessionStatus status;
        private Integer feedbackScore;
        private Integer estimatedMinutes;
    }
}
