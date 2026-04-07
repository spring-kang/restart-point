package com.restartpoint.domain.user.controller;

import com.restartpoint.domain.user.dto.UserResponse;
import com.restartpoint.domain.user.service.UserService;
import com.restartpoint.global.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserService userService;

    @GetMapping("/certifications/pending")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getPendingCertifications() {
        List<UserResponse> response = userService.getPendingCertifications();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{userId}/certification/approve")
    public ResponseEntity<ApiResponse<UserResponse>> approveCertification(@PathVariable Long userId) {
        UserResponse response = userService.approveCertification(userId);
        return ResponseEntity.ok(ApiResponse.success(response, "수료 인증이 승인되었습니다."));
    }

    @PostMapping("/{userId}/certification/reject")
    public ResponseEntity<ApiResponse<UserResponse>> rejectCertification(@PathVariable Long userId) {
        UserResponse response = userService.rejectCertification(userId);
        return ResponseEntity.ok(ApiResponse.success(response, "수료 인증이 거절되었습니다."));
    }
}
