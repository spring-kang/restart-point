package com.restartpoint.domain.team.dto;

import com.restartpoint.domain.profile.entity.JobRole;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class TeamApplyRequest {

    @NotNull(message = "지원 역할은 필수입니다.")
    private JobRole role;

    private String applicationMessage;
}
