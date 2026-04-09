package com.restartpoint.domain.project.dto;

import com.restartpoint.domain.profile.entity.JobRole;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class MemberProgressRequest {

    @NotNull(message = "사용자 ID는 필수입니다.")
    private Long userId;

    @NotNull(message = "역할은 필수입니다.")
    private JobRole jobRole;

    private String completedTasks;
    private String inProgressTasks;
    private String personalBlockers;

    @Min(value = 0, message = "기여도는 0 이상이어야 합니다.")
    @Max(value = 100, message = "기여도는 100 이하여야 합니다.")
    private Integer contributionPercentage;
}
