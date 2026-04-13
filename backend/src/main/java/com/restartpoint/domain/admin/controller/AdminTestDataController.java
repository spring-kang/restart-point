package com.restartpoint.domain.admin.controller;

import com.restartpoint.domain.admin.dto.TestDataSeedResponse;
import com.restartpoint.domain.admin.service.AdminTestDataService;
import com.restartpoint.global.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/test-data")
@RequiredArgsConstructor
public class AdminTestDataController {

    private final AdminTestDataService adminTestDataService;

    @PostMapping("/review-seed")
    public ResponseEntity<ApiResponse<TestDataSeedResponse>> resetReviewSeed() {
        TestDataSeedResponse response = adminTestDataService.resetReviewSeed();
        return ResponseEntity.ok(ApiResponse.success(response, "리뷰 테스트 데이터가 재설정되었습니다."));
    }
}
