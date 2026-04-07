package com.restartpoint.domain.user.dto;

import com.restartpoint.domain.user.entity.User;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AuthResponse {

    private String accessToken;
    private UserResponse user;

    public static AuthResponse of(String accessToken, User user) {
        return AuthResponse.builder()
                .accessToken(accessToken)
                .user(UserResponse.from(user))
                .build();
    }
}
