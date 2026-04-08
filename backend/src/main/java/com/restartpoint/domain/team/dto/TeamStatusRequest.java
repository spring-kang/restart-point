package com.restartpoint.domain.team.dto;

import com.restartpoint.domain.team.entity.TeamStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class TeamStatusRequest {

    @NotNull(message = "팀 상태는 필수입니다.")
    private TeamStatus status;
}
