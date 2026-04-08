package com.restartpoint.domain.user.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class EmailVerificationResponse {

    private String signupToken;
}
