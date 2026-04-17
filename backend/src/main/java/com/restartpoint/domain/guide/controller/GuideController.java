package com.restartpoint.domain.guide.controller;

import com.restartpoint.domain.guide.dto.*;
import com.restartpoint.domain.guide.service.GuideService;
import com.restartpoint.global.security.CurrentUser;
import com.restartpoint.global.security.CustomUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Guide", description = "시즌별 단계별 가이드 API")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class GuideController {

    private final GuideService guideService;

    // ========== ProjectTemplate API (관리자용) ==========

    @Operation(summary = "시즌 가이드 템플릿 생성", description = "시즌별 프로젝트 가이드 템플릿을 생성합니다.")
    @PostMapping("/seasons/{seasonId}/templates")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProjectTemplateResponse> createTemplate(
            @PathVariable Long seasonId,
            @Valid @RequestBody ProjectTemplateRequest request) {
        return ResponseEntity.ok(guideService.createTemplate(seasonId, request));
    }

    @Operation(summary = "시즌 기간 기반 템플릿 자동 생성", description = "시즌의 프로젝트 기간에서 주차를 계산하여 템플릿을 생성합니다.")
    @PostMapping("/seasons/{seasonId}/templates/auto")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProjectTemplateResponse> createTemplateFromSeason(
            @PathVariable Long seasonId,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String description) {
        return ResponseEntity.ok(guideService.createTemplateFromSeason(seasonId, title, description));
    }

    @Operation(summary = "시즌 템플릿 목록 조회", description = "특정 시즌의 모든 가이드 템플릿을 조회합니다.")
    @GetMapping("/seasons/{seasonId}/templates")
    public ResponseEntity<List<ProjectTemplateResponse>> getTemplates(@PathVariable Long seasonId) {
        return ResponseEntity.ok(guideService.getTemplatesBySeasonId(seasonId));
    }

    @Operation(summary = "활성 템플릿 조회", description = "특정 시즌의 활성화된 가이드 템플릿을 조회합니다.")
    @GetMapping("/seasons/{seasonId}/templates/active")
    public ResponseEntity<ProjectTemplateResponse> getActiveTemplate(@PathVariable Long seasonId) {
        return ResponseEntity.ok(guideService.getActiveTemplate(seasonId));
    }

    @Operation(summary = "템플릿 상세 조회", description = "가이드 템플릿의 상세 정보와 주차별 가이드를 조회합니다.")
    @GetMapping("/templates/{templateId}")
    public ResponseEntity<ProjectTemplateResponse> getTemplate(@PathVariable Long templateId) {
        return ResponseEntity.ok(guideService.getTemplate(templateId));
    }

    @Operation(summary = "템플릿 수정", description = "가이드 템플릿을 수정합니다.")
    @PutMapping("/templates/{templateId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProjectTemplateResponse> updateTemplate(
            @PathVariable Long templateId,
            @Valid @RequestBody ProjectTemplateRequest request) {
        return ResponseEntity.ok(guideService.updateTemplate(templateId, request));
    }

    @Operation(summary = "템플릿 활성화", description = "가이드 템플릿을 활성화합니다. 같은 시즌의 다른 템플릿은 비활성화됩니다.")
    @PostMapping("/templates/{templateId}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> activateTemplate(@PathVariable Long templateId) {
        guideService.activateTemplate(templateId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "템플릿 삭제", description = "가이드 템플릿을 삭제합니다.")
    @DeleteMapping("/templates/{templateId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteTemplate(@PathVariable Long templateId) {
        guideService.deleteTemplate(templateId);
        return ResponseEntity.noContent().build();
    }

    // ========== WeeklyGuideline API (관리자용) ==========

    @Operation(summary = "주차별 가이드 생성", description = "템플릿에 주차별 가이드를 추가합니다.")
    @PostMapping("/templates/{templateId}/guidelines")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<WeeklyGuidelineResponse> createGuideline(
            @PathVariable Long templateId,
            @Valid @RequestBody WeeklyGuidelineRequest request) {
        return ResponseEntity.ok(guideService.createGuideline(templateId, request));
    }

    @Operation(summary = "템플릿의 모든 가이드 조회", description = "템플릿에 속한 모든 주차별 가이드를 조회합니다.")
    @GetMapping("/templates/{templateId}/guidelines")
    public ResponseEntity<List<WeeklyGuidelineResponse>> getGuidelines(@PathVariable Long templateId) {
        return ResponseEntity.ok(guideService.getGuidelinesByTemplateId(templateId));
    }

    @Operation(summary = "가이드 상세 조회", description = "주차별 가이드의 상세 정보를 조회합니다.")
    @GetMapping("/guidelines/{guidelineId}")
    public ResponseEntity<WeeklyGuidelineResponse> getGuideline(@PathVariable Long guidelineId) {
        return ResponseEntity.ok(guideService.getGuideline(guidelineId));
    }

    @Operation(summary = "시즌/주차로 가이드 조회", description = "시즌 ID와 주차 번호로 가이드를 조회합니다.")
    @GetMapping("/seasons/{seasonId}/guidelines/week/{weekNumber}")
    public ResponseEntity<WeeklyGuidelineResponse> getGuidelineBySeasonAndWeek(
            @PathVariable Long seasonId,
            @PathVariable Integer weekNumber) {
        return ResponseEntity.ok(guideService.getGuidelineBySeasonAndWeek(seasonId, weekNumber));
    }

    @Operation(summary = "가이드 수정", description = "주차별 가이드를 수정합니다.")
    @PutMapping("/guidelines/{guidelineId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<WeeklyGuidelineResponse> updateGuideline(
            @PathVariable Long guidelineId,
            @Valid @RequestBody WeeklyGuidelineRequest request) {
        return ResponseEntity.ok(guideService.updateGuideline(guidelineId, request));
    }

    @Operation(summary = "가이드 삭제", description = "주차별 가이드를 삭제합니다.")
    @DeleteMapping("/guidelines/{guidelineId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteGuideline(@PathVariable Long guidelineId) {
        guideService.deleteGuideline(guidelineId);
        return ResponseEntity.noContent().build();
    }

    // ========== GuidelineCompletion API (사용자용) ==========

    @Operation(summary = "가이드 완료 표시", description = "해당 주차 가이드를 완료 처리합니다.")
    @PostMapping("/guidelines/{guidelineId}/complete")
    public ResponseEntity<GuidelineCompletionResponse> markGuidelineComplete(
            @Parameter(hidden = true) @CurrentUser CustomUserPrincipal userPrincipal,
            @PathVariable Long guidelineId,
            @RequestBody(required = false) GuidelineCompletionRequest request) {
        return ResponseEntity.ok(guideService.markGuidelineComplete(
                userPrincipal.getUserId(), guidelineId,
                request != null ? request : new GuidelineCompletionRequest()));
    }

    @Operation(summary = "가이드 완료 취소", description = "해당 주차 가이드의 완료를 취소합니다.")
    @DeleteMapping("/guidelines/{guidelineId}/complete")
    public ResponseEntity<Void> markGuidelineIncomplete(
            @Parameter(hidden = true) @CurrentUser CustomUserPrincipal userPrincipal,
            @PathVariable Long guidelineId) {
        guideService.markGuidelineIncomplete(userPrincipal.getUserId(), guidelineId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "체크리스트 진행 업데이트", description = "가이드의 체크리스트 진행 상황을 업데이트합니다.")
    @PutMapping("/guidelines/{guidelineId}/checklist")
    public ResponseEntity<GuidelineCompletionResponse> updateChecklistProgress(
            @Parameter(hidden = true) @CurrentUser CustomUserPrincipal userPrincipal,
            @PathVariable Long guidelineId,
            @RequestBody List<Integer> completedIndexes) {
        return ResponseEntity.ok(guideService.updateChecklistProgress(
                userPrincipal.getUserId(), guidelineId, completedIndexes));
    }

    @Operation(summary = "내 가이드 진행 현황 조회", description = "해당 시즌에서 내 가이드 진행 현황을 조회합니다.")
    @GetMapping("/seasons/{seasonId}/my-progress")
    public ResponseEntity<GuidelineProgressResponse> getMyProgress(
            @Parameter(hidden = true) @CurrentUser CustomUserPrincipal userPrincipal,
            @PathVariable Long seasonId) {
        return ResponseEntity.ok(guideService.getUserProgress(userPrincipal.getUserId(), seasonId));
    }

    @Operation(summary = "내 가이드 완료 목록 조회", description = "해당 시즌에서 완료한 가이드 목록을 조회합니다.")
    @GetMapping("/seasons/{seasonId}/my-completions")
    public ResponseEntity<List<GuidelineCompletionResponse>> getMyCompletions(
            @Parameter(hidden = true) @CurrentUser CustomUserPrincipal userPrincipal,
            @PathVariable Long seasonId) {
        return ResponseEntity.ok(guideService.getUserCompletions(userPrincipal.getUserId(), seasonId));
    }
}
