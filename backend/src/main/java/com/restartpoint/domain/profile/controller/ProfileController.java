package com.restartpoint.domain.profile.controller;

import com.restartpoint.domain.profile.dto.ProfileRequest;
import com.restartpoint.domain.profile.dto.ProfileResponse;
import com.restartpoint.domain.profile.service.ProfileService;
import com.restartpoint.global.common.ApiResponse;
import com.restartpoint.global.security.CurrentUser;
import com.restartpoint.global.security.CustomUserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping("/users/me/profile")
    public ResponseEntity<ApiResponse<ProfileResponse>> getMyProfile(
            @CurrentUser CustomUserPrincipal principal) {
        Optional<ProfileResponse> profile = profileService.getMyProfile(principal.getUserId());
        return profile
                .map(p -> ResponseEntity.ok(ApiResponse.success(p)))
                .orElse(ResponseEntity.ok(ApiResponse.success(null)));
    }

    @PutMapping("/users/me/profile")
    public ResponseEntity<ApiResponse<ProfileResponse>> createOrUpdateProfile(
            @CurrentUser CustomUserPrincipal principal,
            @Valid @RequestBody ProfileRequest request) {
        ProfileResponse response = profileService.createOrUpdateProfile(principal.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.success(response, "프로필이 저장되었습니다."));
    }

    @GetMapping("/profiles/{profileId}")
    public ResponseEntity<ApiResponse<ProfileResponse>> getProfile(@PathVariable Long profileId) {
        ProfileResponse response = profileService.getProfile(profileId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
