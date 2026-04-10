package com.restartpoint.domain.review.controller;

import com.restartpoint.domain.project.dto.ProjectResponse;
import com.restartpoint.domain.project.entity.Project;
import com.restartpoint.domain.review.dto.*;
import com.restartpoint.domain.review.entity.RubricItem;
import com.restartpoint.domain.review.service.ReviewService;
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
     * 루브릭 항목 응답 DTO
     */
    public record RubricItemResponse(RubricItem item, String label, String description) {}
}
