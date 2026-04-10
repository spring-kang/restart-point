package com.restartpoint.domain.review.controller;

import com.restartpoint.domain.review.dto.ReviewGuideResponse;
import com.restartpoint.domain.review.dto.ReviewGuideResponse.ReviewGuideStatus;
import com.restartpoint.domain.review.service.ReviewGuideService;
import com.restartpoint.global.common.ApiResponse;
import com.restartpoint.global.security.CurrentUser;
import com.restartpoint.global.security.CustomUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 심사 가이드 학습 컨트롤러
 */
@RestController
@RequestMapping("/api/v1/review-guide")
@RequiredArgsConstructor
public class ReviewGuideController {

    private final ReviewGuideService reviewGuideService;

    /**
     * 심사 가이드 콘텐츠 조회
     */
    @GetMapping
    public ResponseEntity<ApiResponse<ReviewGuideResponse>> getGuide(
            @CurrentUser CustomUserPrincipal principal) {
        ReviewGuideResponse guide = reviewGuideService.getGuide(principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success(guide));
    }

    /**
     * 가이드 완료 상태 조회
     */
    @GetMapping("/status")
    public ResponseEntity<ApiResponse<ReviewGuideStatus>> getStatus(
            @CurrentUser CustomUserPrincipal principal) {
        ReviewGuideStatus status = reviewGuideService.getCompletionStatus(principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success(status));
    }

    /**
     * 루브릭 학습 완료 처리
     */
    @PostMapping("/complete/rubric")
    public ResponseEntity<ApiResponse<ReviewGuideStatus>> completeRubricLearning(
            @CurrentUser CustomUserPrincipal principal) {
        ReviewGuideStatus status = reviewGuideService.completeRubricLearning(principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success(status, "루브릭 학습을 완료했습니다."));
    }

    /**
     * 사례 비교 학습 완료 처리
     */
    @PostMapping("/complete/examples")
    public ResponseEntity<ApiResponse<ReviewGuideStatus>> completeExampleComparison(
            @CurrentUser CustomUserPrincipal principal) {
        ReviewGuideStatus status = reviewGuideService.completeExampleComparison(principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success(status, "사례 비교 학습을 완료했습니다."));
    }

    /**
     * 연습 평가 완료 처리
     */
    @PostMapping("/complete/practice")
    public ResponseEntity<ApiResponse<ReviewGuideStatus>> completePracticeEvaluation(
            @CurrentUser CustomUserPrincipal principal) {
        ReviewGuideStatus status = reviewGuideService.completePracticeEvaluation(principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success(status, "연습 평가를 완료했습니다."));
    }
}
