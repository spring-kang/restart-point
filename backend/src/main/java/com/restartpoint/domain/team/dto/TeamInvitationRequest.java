package com.restartpoint.domain.team.dto;

import com.restartpoint.domain.profile.entity.JobRole;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class TeamInvitationRequest {

    @NotNull(message = "초대할 사용자 ID는 필수입니다.")
    private Long userId;

    @NotNull(message = "제안 역할은 필수입니다.")
    private JobRole role;

    private String message;

    private Integer matchScore;
}
