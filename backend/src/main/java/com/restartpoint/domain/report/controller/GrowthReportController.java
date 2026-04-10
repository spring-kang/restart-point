package com.restartpoint.domain.report.controller;

import com.restartpoint.domain.report.dto.GrowthReportResponse;
import com.restartpoint.domain.report.service.GrowthReportService;
import com.restartpoint.global.common.ApiResponse;
import com.restartpoint.global.security.CurrentUser;
import com.restartpoint.global.security.CustomUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class GrowthReportController {

    private final GrowthReportService growthReportService;

    /**
     * 내 성장 리포트 목록 조회
     */
    @GetMapping("/users/me/growth-reports")
    public ResponseEntity<ApiResponse<List<GrowthReportResponse>>> getMyReports(
            @CurrentUser CustomUserPrincipal principal) {
        List<GrowthReportResponse> reports = growthReportService.getMyReports(principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success(reports));
    }

    /**
     * 프로젝트 팀 리포트 조회
     */
    @GetMapping("/projects/{projectId}/growth-reports/team")
    public ResponseEntity<ApiResponse<GrowthReportResponse>> getTeamReport(
            @CurrentUser CustomUserPrincipal principal,
            @PathVariable Long projectId) {
        GrowthReportResponse report = growthReportService.getTeamReport(principal.getUserId(), projectId);
        return ResponseEntity.ok(ApiResponse.success(report));
    }

    /**
     * 프로젝트 개인 리포트 조회
     */
    @GetMapping("/projects/{projectId}/growth-reports/me")
    public ResponseEntity<ApiResponse<GrowthReportResponse>> getMyIndividualReport(
            @CurrentUser CustomUserPrincipal principal,
            @PathVariable Long projectId) {
        GrowthReportResponse report = growthReportService.getIndividualReport(principal.getUserId(), projectId);
        return ResponseEntity.ok(ApiResponse.success(report));
    }

    /**
     * 프로젝트의 모든 리포트 조회 (팀원용)
     */
    @GetMapping("/projects/{projectId}/growth-reports")
    public ResponseEntity<ApiResponse<List<GrowthReportResponse>>> getProjectReports(
            @CurrentUser CustomUserPrincipal principal,
            @PathVariable Long projectId) {
        List<GrowthReportResponse> reports = growthReportService.getProjectReports(principal.getUserId(), projectId);
        return ResponseEntity.ok(ApiResponse.success(reports));
    }

    /**
     * 리포트 재생성 (AI 생성 실패 시)
     */
    @PostMapping("/growth-reports/{reportId}/regenerate")
    public ResponseEntity<ApiResponse<GrowthReportResponse>> regenerateReport(
            @CurrentUser CustomUserPrincipal principal,
            @PathVariable Long reportId) {
        GrowthReportResponse report = growthReportService.regenerateReport(principal.getUserId(), reportId);
        return ResponseEntity.ok(ApiResponse.success(report, "리포트가 재생성되었습니다."));
    }

    /**
     * 프로젝트 리포트 생성 (운영자용)
     */
    @PostMapping("/projects/{projectId}/growth-reports/generate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> generateProjectReports(
            @PathVariable Long projectId) {
        growthReportService.generateReportsForProject(projectId);
        return ResponseEntity.ok(ApiResponse.success(null, "리포트 생성이 시작되었습니다."));
    }

    /**
     * 시즌 전체 리포트 생성 (운영자용)
     */
    @PostMapping("/seasons/{seasonId}/growth-reports/generate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> generateSeasonReports(
            @PathVariable Long seasonId) {
        growthReportService.generateReportsForSeason(seasonId);
        return ResponseEntity.ok(ApiResponse.success(null, "시즌 리포트 생성이 시작되었습니다."));
    }
}
