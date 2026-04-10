package com.restartpoint.domain.review.controller;

import com.restartpoint.domain.project.dto.ProjectResponse;
import com.restartpoint.domain.project.entity.Project;
import com.restartpoint.domain.review.dto.*;
import com.restartpoint.domain.review.entity.RubricItem;
import com.restartpoint.domain.review.service.ReviewAssistantService;
import com.restartpoint.domain.review.service.ReviewPatternService;
import com.restartpoint.domain.review.service.ReviewService;
import org.springframework.security.access.prepost.PreAuthorize;
import com.restartpoint.global.common.ApiResponse;
import com.restartpoint.global.security.CurrentUser;
import com.restartpoint.global.security.CustomUserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;
    private final ReviewPatternService reviewPatternService;
    private final ReviewAssistantService reviewAssistantService;

    /**
     * 심사 제출
     */
    @PostMapping("/projects/{projectId}/reviews")
    public ResponseEntity<ApiResponse<ReviewResponse>> createReview(
            @CurrentUser CustomUserPrincipal principal,
            @PathVariable Long projectId,
            @Valid @RequestBody ReviewCreateRequest request) {
        ReviewResponse review = reviewService.createReview(principal.getUserId(), projectId, request);
        return ResponseEntity.ok(ApiResponse.success(review, "심사가 제출되었습니다."));
    }

    /**
     * 프로젝트별 심사 목록 조회
     */
    @GetMapping("/projects/{projectId}/reviews")
    public ResponseEntity<ApiResponse<List<ReviewResponse>>> getReviewsByProject(
            @CurrentUser CustomUserPrincipal principal,
            @PathVariable Long projectId) {
        List<ReviewResponse> reviews = reviewService.getReviewsByProject(principal.getUserId(), projectId);
        return ResponseEntity.ok(ApiResponse.success(reviews));
    }

    /**
     * 프로젝트 심사 요약 (집계)
     */
    @GetMapping("/projects/{projectId}/review-summary")
    public ResponseEntity<ApiResponse<ReviewSummaryResponse>> getReviewSummary(
            @CurrentUser CustomUserPrincipal principal,
            @PathVariable Long projectId) {
        ReviewSummaryResponse summary = reviewService.getReviewSummary(principal.getUserId(), projectId);
        return ResponseEntity.ok(ApiResponse.success(summary));
    }

    /**
     * 심사 가능한 프로젝트 목록 조회
     * 심사 대상 프로젝트의 심사용 정보를 반환 (민감 정보 제외)
     */
    @GetMapping("/seasons/{seasonId}/reviewable-projects")
    public ResponseEntity<ApiResponse<List<ProjectResponse>>> getReviewableProjects(
            @CurrentUser CustomUserPrincipal principal,
            @PathVariable Long seasonId) {
        List<Project> projects = reviewService.getReviewableProjects(principal.getUserId(), seasonId);
        List<ProjectResponse> responses = projects.stream()
                .map(ProjectResponse::forReview)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    /**
     * 내가 심사한 목록 조회
     */
    @GetMapping("/users/me/reviews")
    public ResponseEntity<ApiResponse<List<ReviewResponse>>> getMyReviews(
            @CurrentUser CustomUserPrincipal principal) {
        List<ReviewResponse> reviews = reviewService.getMyReviews(principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success(reviews));
    }

    /**
     * 루브릭 항목 목록 조회
     */
    @GetMapping("/rubric-items")
    public ResponseEntity<ApiResponse<List<RubricItemResponse>>> getRubricItems() {
        List<RubricItemResponse> items = Arrays.stream(RubricItem.values())
                .map(item -> new RubricItemResponse(item, item.getLabel(), item.getDescription()))
                .toList();
        return ResponseEntity.ok(ApiResponse.success(items));
    }

    /**
     * 내 심사 패턴 분석 조회
     */
    @GetMapping("/users/me/review-pattern")
    public ResponseEntity<ApiResponse<ReviewPatternAnalysisResponse>> getMyReviewPattern(
            @CurrentUser CustomUserPrincipal principal) {
        ReviewPatternAnalysisResponse analysis = reviewPatternService.analyzeMyPattern(principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success(analysis));
    }

    /**
     * 루브릭 항목 응답 DTO
     */
    public record RubricItemResponse(RubricItem item, String label, String description) {}

    // === 운영자 전용 AI 심사 분석 API ===

    /**
     * 프로젝트 심사 AI 분석 (운영자 전용)
     * 심사 코멘트 요약, 강점/약점, 이상치 감지, 현직자/예비참여자 비교 분석
     */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/projects/{projectId}/review-analysis")
    public ResponseEntity<ApiResponse<ReviewAnalysisResponse>> getProjectReviewAnalysis(
            @PathVariable Long projectId) {
        ReviewAnalysisResponse analysis = reviewAssistantService.analyzeProjectReviews(projectId);
        return ResponseEntity.ok(ApiResponse.success(analysis));
    }

    /**
     * 시즌 전체 심사 AI 분석 (운영자 전용)
     */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/seasons/{seasonId}/review-analysis")
    public ResponseEntity<ApiResponse<List<ReviewAnalysisResponse>>> getSeasonReviewAnalysis(
            @PathVariable Long seasonId) {
        List<ReviewAnalysisResponse> analyses = reviewAssistantService.analyzeSeasonReviews(seasonId);
        return ResponseEntity.ok(ApiResponse.success(analyses));
    }
}
