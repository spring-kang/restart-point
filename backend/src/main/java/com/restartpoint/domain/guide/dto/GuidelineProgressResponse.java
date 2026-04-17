package com.restartpoint.domain.guide.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 사용자의 가이드 진행 현황 응답
 */
@Getter
@Builder
public class GuidelineProgressResponse {

    private Long userId;
    private Long seasonId;
    private Long templateId;
    private String templateTitle;
    private Integer totalWeeks;
    private Integer completedWeeks;
    private Double progressPercentage;
    private Integer currentWeek;
    private List<WeekProgress> weeklyProgress;

    @Getter
    @Builder
    public static class WeekProgress {
        private Integer weekNumber;
        private String title;
        private Boolean completed;
        private Integer completedChecklistCount;
        private Integer totalChecklistCount;
    }
}
