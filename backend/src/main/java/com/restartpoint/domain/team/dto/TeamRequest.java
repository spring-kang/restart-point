package com.restartpoint.domain.team.dto;

import com.restartpoint.domain.profile.entity.JobRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class TeamRequest {

    @NotBlank(message = "팀 이름은 필수입니다.")
    private String name;

    private String description;

    @NotNull(message = "시즌 ID는 필수입니다.")
    private Long seasonId;

    // 리더의 역할 (팀 ��성 시 필수)
    @NotNull(message = "리더 역할은 필수입니다.")
    private JobRole leaderRole;

    // 모집 중인 역할
    private Boolean recruitingPlanner = false;
    private Boolean recruitingUxui = false;
    private Boolean recruitingFrontend = false;
    private Boolean recruitingBackend = false;
}
