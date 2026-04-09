package com.restartpoint.domain.user.dto;

import com.restartpoint.domain.user.entity.Role;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UpdateRoleRequest {
    private Role role;
}
