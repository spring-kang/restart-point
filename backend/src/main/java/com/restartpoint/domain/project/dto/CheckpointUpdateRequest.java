package com.restartpoint.domain.project.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class CheckpointUpdateRequest {

    private String weeklyGoal;
    private String progressSummary;
    private String blockers;
    private String nextWeekPlan;

    // 역할별 진행 상황
    private List<MemberProgressRequest> memberProgresses;
}
