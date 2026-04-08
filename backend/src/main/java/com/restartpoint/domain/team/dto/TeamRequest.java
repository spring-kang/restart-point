package com.restartpoint.domain.team.dto;

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

    // 모집 중인 역할
    private Boolean recruitingPlanner = false;
    private Boolean recruitingUxui = false;
    private Boolean recruitingFrontend = false;
    private Boolean recruitingBackend = false;
}
