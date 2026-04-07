package com.restartpoint.domain.user.controller;

import com.restartpoint.domain.user.dto.CertificationRequest;
import com.restartpoint.domain.user.dto.UserResponse;
import com.restartpoint.domain.user.service.UserService;
import com.restartpoint.global.common.ApiResponse;
import com.restartpoint.global.security.CurrentUser;
import com.restartpoint.global.security.CustomUserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getMe(@CurrentUser CustomUserPrincipal principal) {
        UserResponse response = userService.getMe(principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/me/certification")
    public ResponseEntity<ApiResponse<UserResponse>> requestCertification(
            @CurrentUser CustomUserPrincipal principal,
            @Valid @RequestBody CertificationRequest request) {
        UserResponse response = userService.requestCertification(principal.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.success(response, "수료 인증 요청이 완료되었습니다."));
    }
}
