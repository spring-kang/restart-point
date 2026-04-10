package com.restartpoint.domain.admin.controller;

import com.restartpoint.domain.admin.dto.SeasonDashboardResponse;
import com.restartpoint.domain.admin.service.AdminDashboardService;
import com.restartpoint.global.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 운영자 대시보드 API 컨트롤러
 */
@RestController
@RequestMapping("/api/v1/admin/dashboard")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;

    /**
     * 전체 대시보드 조회 (시즌 목록, 대기 인증 수 등)
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getOverallDashboard() {
        Map<String, Object> response = adminDashboardService.getOverallDashboard();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 시즌별 대시보드 조회
     * 참가자 현황, 팀 현황, 프로젝트 현황, 심사 현황, 리포트 현황, 위험 팀 목록
     */
    @GetMapping("/seasons/{seasonId}")
    public ResponseEntity<ApiResponse<SeasonDashboardResponse>> getSeasonDashboard(
            @PathVariable Long seasonId
    ) {
        SeasonDashboardResponse response = adminDashboardService.getSeasonDashboard(seasonId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
