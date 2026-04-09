package com.restartpoint.domain.project.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class CheckpointCreateRequest {

    @NotNull(message = "주차 번호는 필수입니다.")
    @Min(value = 1, message = "주차 번호는 1 이상이어야 합니다.")
    private Integer weekNumber;

    private String weeklyGoal;
    private String progressSummary;
    private String blockers;
    private String nextWeekPlan;

    // 역할별 진행 상황
    private List<MemberProgressRequest> memberProgresses;
}
