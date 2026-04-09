package com.restartpoint.domain.user.controller;

import com.restartpoint.domain.user.dto.UpdateRoleRequest;
import com.restartpoint.domain.user.dto.UserResponse;
import com.restartpoint.domain.user.entity.CertificationStatus;
import com.restartpoint.domain.user.entity.Role;
import com.restartpoint.domain.user.service.UserService;
import com.restartpoint.global.common.ApiResponse;
import com.restartpoint.global.security.CurrentUser;
import com.restartpoint.global.security.CustomUserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserService userService;

    // 회원 목록 조회 (검색, 필터링, 페이징)
    @GetMapping
    public ResponseEntity<ApiResponse<Page<UserResponse>>> getUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) CertificationStatus certificationStatus,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<UserResponse> response = userService.getUsers(keyword, role, certificationStatus, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // 회원 상세 조회
    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<UserResponse>> getUser(@PathVariable Long userId) {
        UserResponse response = userService.getUser(userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // 회원 역할 변경
    @PatchMapping("/{userId}/role")
    public ResponseEntity<ApiResponse<UserResponse>> updateUserRole(
            @CurrentUser CustomUserPrincipal principal,
            @PathVariable Long userId,
            @Valid @RequestBody UpdateRoleRequest request
    ) {
        UserResponse response = userService.updateUserRole(principal.getUserId(), userId, request.getRole());
        return ResponseEntity.ok(ApiResponse.success(response, "회원 역할이 변경되었습니다."));
    }

    // 회원 삭제
    @DeleteMapping("/{userId}")
    public ResponseEntity<ApiResponse<Void>> deleteUser(
            @CurrentUser CustomUserPrincipal principal,
            @PathVariable Long userId
    ) {
        userService.deleteUser(principal.getUserId(), userId);
        return ResponseEntity.ok(ApiResponse.success(null, "회원이 삭제되었습니다."));
    }

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
