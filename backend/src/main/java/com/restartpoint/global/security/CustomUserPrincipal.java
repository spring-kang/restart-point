package com.restartpoint.global.security;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class CustomUserPrincipal {

    private final Long userId;
    private final String email;
    private final String role;
}
