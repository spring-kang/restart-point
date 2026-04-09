package com.restartpoint.domain.user.dto;

import com.restartpoint.domain.user.entity.Role;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UpdateRoleRequest {
    @NotNull(message = "역할은 필수입니다.")
    private Role role;
}
