package com.restartpoint.domain.mentoring.controller;

import com.restartpoint.domain.mentoring.dto.*;
import com.restartpoint.domain.mentoring.service.MentoringService;
import com.restartpoint.domain.profile.entity.JobRole;
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

@Tag(name = "Mentoring", description = "직무별 멘토링 API")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class MentoringController {

    private final MentoringService mentoringService;

    // ========== JobRoleMentoring API (관리자용) ==========

    @Operation(summary = "직무별 멘토링 생성", description = "시즌에 직무별 멘토링 프로그램을 생성합니다.")
    @PostMapping("/seasons/{seasonId}/mentorings")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<JobRoleMentoringResponse> createMentoring(
            @PathVariable Long seasonId,
            @Valid @RequestBody JobRoleMentoringRequest request) {
        return ResponseEntity.ok(mentoringService.createMentoring(seasonId, request));
    }

    @Operation(summary = "시즌 멘토링 목록 조회", description = "특정 시즌의 모든 직무별 멘토링을 조회합니다.")
    @GetMapping("/seasons/{seasonId}/mentorings")
    public ResponseEntity<List<JobRoleMentoringResponse>> getMentorings(@PathVariable Long seasonId) {
        return ResponseEntity.ok(mentoringService.getMentoringsBySeasonId(seasonId));
    }

    @Operation(summary = "직무별 멘토링 조회", description = "특정 시즌의 특정 직무 멘토링을 조회합니다.")
    @GetMapping("/seasons/{seasonId}/mentorings/{jobRole}")
    public ResponseEntity<JobRoleMentoringResponse> getMentoringByJobRole(
            @PathVariable Long seasonId,
            @PathVariable JobRole jobRole) {
        return ResponseEntity.ok(mentoringService.getMentoringBySeasonAndJobRole(seasonId, jobRole));
    }

    @Operation(summary = "멘토링 상세 조회", description = "멘토링 프로그램의 상세 정보와 모듈을 조회합니다.")
    @GetMapping("/mentorings/{mentoringId}")
    public ResponseEntity<JobRoleMentoringResponse> getMentoring(@PathVariable Long mentoringId) {
        return ResponseEntity.ok(mentoringService.getMentoring(mentoringId));
    }

    @Operation(summary = "멘토링 수정", description = "멘토링 프로그램을 수정합니다.")
    @PutMapping("/mentorings/{mentoringId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<JobRoleMentoringResponse> updateMentoring(
            @PathVariable Long mentoringId,
            @Valid @RequestBody JobRoleMentoringRequest request) {
        return ResponseEntity.ok(mentoringService.updateMentoring(mentoringId, request));
    }

    @Operation(summary = "멘토링 삭제", description = "멘토링 프로그램을 삭제합니다.")
    @DeleteMapping("/mentorings/{mentoringId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteMentoring(@PathVariable Long mentoringId) {
        mentoringService.deleteMentoring(mentoringId);
        return ResponseEntity.noContent().build();
    }

    // ========== MentoringModule API (관리자용) ==========

    @Operation(summary = "멘토링 모듈 생성", description = "멘토링에 주차별 모듈을 추가합니다.")
    @PostMapping("/mentorings/{mentoringId}/modules")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MentoringModuleResponse> createModule(
            @PathVariable Long mentoringId,
            @Valid @RequestBody MentoringModuleRequest request) {
        return ResponseEntity.ok(mentoringService.createModule(mentoringId, request));
    }

    @Operation(summary = "멘토링 모듈 목록 조회", description = "멘토링의 모든 모듈을 조회합니다.")
    @GetMapping("/mentorings/{mentoringId}/modules")
    public ResponseEntity<List<MentoringModuleResponse>> getModules(@PathVariable Long mentoringId) {
        return ResponseEntity.ok(mentoringService.getModulesByMentoringId(mentoringId));
    }

    @Operation(summary = "모듈 상세 조회", description = "멘토링 모듈의 상세 정보를 조회합니다.")
    @GetMapping("/modules/{moduleId}")
    public ResponseEntity<MentoringModuleResponse> getModule(@PathVariable Long moduleId) {
        return ResponseEntity.ok(mentoringService.getModule(moduleId));
    }

    @Operation(summary = "시즌/직무/주차로 모듈 조회", description = "특정 시즌, 직무, 주차의 모듈을 조회합니다.")
    @GetMapping("/seasons/{seasonId}/mentorings/{jobRole}/modules/week/{weekNumber}")
    public ResponseEntity<MentoringModuleResponse> getModuleByWeek(
            @PathVariable Long seasonId,
            @PathVariable JobRole jobRole,
            @PathVariable Integer weekNumber) {
        return ResponseEntity.ok(mentoringService.getModuleBySeasonJobRoleAndWeek(seasonId, jobRole, weekNumber));
    }

    @Operation(summary = "모듈 수정", description = "멘토링 모듈을 수정합니다.")
    @PutMapping("/modules/{moduleId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MentoringModuleResponse> updateModule(
            @PathVariable Long moduleId,
            @Valid @RequestBody MentoringModuleRequest request) {
        return ResponseEntity.ok(mentoringService.updateModule(moduleId, request));
    }

    @Operation(summary = "모듈 삭제", description = "멘토링 모듈을 삭제합니다.")
    @DeleteMapping("/modules/{moduleId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteModule(@PathVariable Long moduleId) {
        mentoringService.deleteModule(moduleId);
        return ResponseEntity.noContent().build();
    }

    // ========== MentoringSession API (사용자용) ==========

    @Operation(summary = "멘토링 세션 시작", description = "해당 모듈의 멘토링 세션을 시작합니다.")
    @PostMapping("/modules/{moduleId}/sessions/start")
    public ResponseEntity<MentoringSessionResponse> startSession(
            @Parameter(hidden = true) @CurrentUser CustomUserPrincipal userPrincipal,
            @PathVariable Long moduleId) {
        return ResponseEntity.ok(mentoringService.startSession(userPrincipal.getUserId(), moduleId));
    }

    @Operation(summary = "세션 상세 조회", description = "멘토링 세션의 상세 정보를 조회합니다.")
    @GetMapping("/sessions/{sessionId}")
    public ResponseEntity<MentoringSessionResponse> getSession(
            @Parameter(hidden = true) @CurrentUser CustomUserPrincipal userPrincipal,
            @PathVariable Long sessionId) {
        return ResponseEntity.ok(mentoringService.getSession(sessionId, userPrincipal.getUserId()));
    }

    @Operation(summary = "내 세션 목록 조회", description = "해당 시즌에서 내 멘토링 세션 목록을 조회합니다.")
    @GetMapping("/seasons/{seasonId}/my-sessions")
    public ResponseEntity<List<MentoringSessionResponse>> getMySessions(
            @Parameter(hidden = true) @CurrentUser CustomUserPrincipal userPrincipal,
            @PathVariable Long seasonId) {
        return ResponseEntity.ok(mentoringService.getMySessionsBySeasonId(userPrincipal.getUserId(), seasonId));
    }

    @Operation(summary = "세션 노트 업데이트", description = "멘토링 세션의 노트, 질문, 완료 과제를 업데이트합니다.")
    @PutMapping("/sessions/{sessionId}/notes")
    public ResponseEntity<MentoringSessionResponse> updateSessionNotes(
            @Parameter(hidden = true) @CurrentUser CustomUserPrincipal userPrincipal,
            @PathVariable Long sessionId,
            @RequestBody MentoringSessionRequest request) {
        return ResponseEntity.ok(mentoringService.updateSessionNotes(
                sessionId, userPrincipal.getUserId(), request));
    }

    @Operation(summary = "세션 완료", description = "멘토링 세션을 완료 처리합니다.")
    @PostMapping("/sessions/{sessionId}/complete")
    public ResponseEntity<MentoringSessionResponse> completeSession(
            @Parameter(hidden = true) @CurrentUser CustomUserPrincipal userPrincipal,
            @PathVariable Long sessionId) {
        return ResponseEntity.ok(mentoringService.completeSession(sessionId, userPrincipal.getUserId()));
    }

    @Operation(summary = "멘토 피드백 제공", description = "멘토가 세션에 피드백을 제공합니다.")
    @PostMapping("/sessions/{sessionId}/feedback")
    @PreAuthorize("hasRole('EXPERT') or hasRole('ADMIN')")
    public ResponseEntity<MentoringSessionResponse> provideFeedback(
            @Parameter(hidden = true) @CurrentUser CustomUserPrincipal userPrincipal,
            @PathVariable Long sessionId,
            @Valid @RequestBody MentoringFeedbackRequest request) {
        return ResponseEntity.ok(mentoringService.provideFeedback(
                sessionId, userPrincipal.getUserId(), request));
    }

    @Operation(summary = "내 멘토링 진행 현황", description = "해당 시즌의 특정 직무 멘토링 진행 현황을 조회합니다.")
    @GetMapping("/seasons/{seasonId}/mentorings/{jobRole}/my-progress")
    public ResponseEntity<MentoringProgressResponse> getMyProgress(
            @Parameter(hidden = true) @CurrentUser CustomUserPrincipal userPrincipal,
            @PathVariable Long seasonId,
            @PathVariable JobRole jobRole) {
        return ResponseEntity.ok(mentoringService.getUserProgress(
                userPrincipal.getUserId(), seasonId, jobRole));
    }
}
