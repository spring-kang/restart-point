package com.restartpoint.domain.project.controller;

import com.restartpoint.domain.project.dto.*;
import com.restartpoint.domain.project.entity.ProjectStatus;
import com.restartpoint.domain.project.service.ProjectService;
import com.restartpoint.global.common.ApiResponse;
import com.restartpoint.global.security.CurrentUser;
import com.restartpoint.global.security.CustomUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Project", description = "프로젝트 및 우수작 API")
public class ProjectController {

    private final ProjectService projectService;

    // 프로젝트 생성
    @PostMapping("/projects")
    public ResponseEntity<ApiResponse<ProjectResponse>> createProject(
            @CurrentUser CustomUserPrincipal principal,
            @Valid @RequestBody ProjectCreateRequest request) {
        ProjectResponse project = projectService.createProject(principal.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.success(project, "프로젝트가 생성되었습니다."));
    }

    // 프로젝트 상세 조회 (팀원만 가능)
    @GetMapping("/projects/{projectId}")
    public ResponseEntity<ApiResponse<ProjectResponse>> getProject(
            @CurrentUser CustomUserPrincipal principal,
            @PathVariable Long projectId) {
        ProjectResponse project = projectService.getProject(principal.getUserId(), projectId);
        return ResponseEntity.ok(ApiResponse.success(project));
    }

    // 팀의 프로젝트 조회 (팀원만 가능)
    @GetMapping("/teams/{teamId}/project")
    public ResponseEntity<ApiResponse<ProjectResponse>> getProjectByTeam(
            @CurrentUser CustomUserPrincipal principal,
            @PathVariable Long teamId) {
        ProjectResponse project = projectService.getProjectByTeam(principal.getUserId(), teamId);
        return ResponseEntity.ok(ApiResponse.success(project));
    }

    // 시즌별 프로젝트 목록 조회
    @Operation(summary = "시즌별 프로젝트 목록 조회", description = "시즌에 속한 프로젝트 목록을 상태별로 조회합니다.")
    @GetMapping("/seasons/{seasonId}/projects")
    public ResponseEntity<ApiResponse<Page<ProjectResponse>>> getProjectsBySeason(
            @PathVariable Long seasonId,
            @RequestParam(required = false) ProjectStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<ProjectResponse> projects;
        if (status != null) {
            projects = projectService.getProjectsBySeasonAndStatus(seasonId, status, pageable);
        } else {
            projects = projectService.getProjectsBySeason(seasonId, pageable);
        }
        return ResponseEntity.ok(ApiResponse.success(projects));
    }

    @Operation(summary = "우수작 목록 조회", description = "메인 화면과 우수작 페이지에서 사용하는 시즌별 우수작 목록을 조회합니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "우수작 목록 조회 성공",
            content = @Content(schema = @Schema(implementation = ProjectResponse.class))
    )
    @GetMapping("/projects/featured")
    public ResponseEntity<ApiResponse<List<ProjectResponse>>> getFeaturedProjects() {
        return ResponseEntity.ok(ApiResponse.success(projectService.getFeaturedProjects()));
    }

    // 프로젝트 수정
    @PutMapping("/projects/{projectId}")
    public ResponseEntity<ApiResponse<ProjectResponse>> updateProject(
            @CurrentUser CustomUserPrincipal principal,
            @PathVariable Long projectId,
            @Valid @RequestBody ProjectUpdateRequest request) {
        ProjectResponse project = projectService.updateProject(principal.getUserId(), projectId, request);
        return ResponseEntity.ok(ApiResponse.success(project, "프로젝트가 수정되었습니다."));
    }

    // 프로젝트 시작 (DRAFT -> IN_PROGRESS)
    @PostMapping("/projects/{projectId}/start")
    public ResponseEntity<ApiResponse<ProjectResponse>> startProject(
            @CurrentUser CustomUserPrincipal principal,
            @PathVariable Long projectId) {
        ProjectResponse project = projectService.startProject(principal.getUserId(), projectId);
        return ResponseEntity.ok(ApiResponse.success(project, "프로젝트가 시작되었습니다."));
    }

    // 프로젝트 제출
    @PostMapping("/projects/{projectId}/submit")
    public ResponseEntity<ApiResponse<ProjectResponse>> submitProject(
            @CurrentUser CustomUserPrincipal principal,
            @PathVariable Long projectId,
            @Valid @RequestBody ProjectSubmitRequest request) {
        ProjectResponse project = projectService.submitProject(principal.getUserId(), projectId, request);
        return ResponseEntity.ok(ApiResponse.success(project, "프로젝트가 제출되었습니다."));
    }

    // 체크포인트 생성
    @PostMapping("/projects/{projectId}/checkpoints")
    public ResponseEntity<ApiResponse<CheckpointResponse>> createCheckpoint(
            @CurrentUser CustomUserPrincipal principal,
            @PathVariable Long projectId,
            @Valid @RequestBody CheckpointCreateRequest request) {
        CheckpointResponse checkpoint = projectService.createCheckpoint(principal.getUserId(), projectId, request);
        return ResponseEntity.ok(ApiResponse.success(checkpoint, "체크포인트가 생성되었습니다."));
    }

    // 체크포인트 목록 조회 (팀원만 가능)
    @GetMapping("/projects/{projectId}/checkpoints")
    public ResponseEntity<ApiResponse<List<CheckpointResponse>>> getCheckpoints(
            @CurrentUser CustomUserPrincipal principal,
            @PathVariable Long projectId) {
        List<CheckpointResponse> checkpoints = projectService.getCheckpointsByProject(principal.getUserId(), projectId);
        return ResponseEntity.ok(ApiResponse.success(checkpoints));
    }

    // 체크포인트 상세 조회 (팀원만 가능)
    @GetMapping("/checkpoints/{checkpointId}")
    public ResponseEntity<ApiResponse<CheckpointResponse>> getCheckpoint(
            @CurrentUser CustomUserPrincipal principal,
            @PathVariable Long checkpointId) {
        CheckpointResponse checkpoint = projectService.getCheckpoint(principal.getUserId(), checkpointId);
        return ResponseEntity.ok(ApiResponse.success(checkpoint));
    }

    // 체크포인트 수정
    @PutMapping("/checkpoints/{checkpointId}")
    public ResponseEntity<ApiResponse<CheckpointResponse>> updateCheckpoint(
            @CurrentUser CustomUserPrincipal principal,
            @PathVariable Long checkpointId,
            @Valid @RequestBody CheckpointUpdateRequest request) {
        CheckpointResponse checkpoint = projectService.updateCheckpoint(principal.getUserId(), checkpointId, request);
        return ResponseEntity.ok(ApiResponse.success(checkpoint, "체크포인트가 수정되었습니다."));
    }

    // 체크포인트 삭제
    @DeleteMapping("/checkpoints/{checkpointId}")
    public ResponseEntity<ApiResponse<Void>> deleteCheckpoint(
            @CurrentUser CustomUserPrincipal principal,
            @PathVariable Long checkpointId) {
        projectService.deleteCheckpoint(principal.getUserId(), checkpointId);
        return ResponseEntity.ok(ApiResponse.success(null, "체크포인트가 삭제되었습니다."));
    }

    // AI 피드백 재생성
    @PostMapping("/checkpoints/{checkpointId}/ai-feedback")
    public ResponseEntity<ApiResponse<CheckpointResponse>> regenerateAiFeedback(
            @CurrentUser CustomUserPrincipal principal,
            @PathVariable Long checkpointId) {
        CheckpointResponse checkpoint = projectService.regenerateAiFeedback(principal.getUserId(), checkpointId);
        return ResponseEntity.ok(ApiResponse.success(checkpoint, "AI 피드백이 재생성되었습니다."));
    }

    @Operation(summary = "우수작 지정", description = "운영자가 프로젝트를 우수작으로 지정합니다. 시즌 내 우수작 순번은 자동 재정렬됩니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "우수작 지정 성공",
            content = @Content(schema = @Schema(implementation = ProjectResponse.class))
    )
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/projects/{projectId}/featured")
    public ResponseEntity<ApiResponse<ProjectResponse>> markProjectAsFeatured(@PathVariable Long projectId) {
        ProjectResponse project = projectService.markProjectAsFeatured(projectId);
        return ResponseEntity.ok(ApiResponse.success(project, "우수작으로 지정되었습니다."));
    }

    @Operation(summary = "우수작 지정 해제", description = "운영자가 프로젝트의 우수작 지정을 해제합니다. 해제 후 시즌 내 우수작 순번은 자동 재정렬됩니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "우수작 지정 해제 성공",
            content = @Content(schema = @Schema(implementation = ProjectResponse.class))
    )
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/admin/projects/{projectId}/featured")
    public ResponseEntity<ApiResponse<ProjectResponse>> unmarkProjectAsFeatured(@PathVariable Long projectId) {
        ProjectResponse project = projectService.unmarkProjectAsFeatured(projectId);
        return ResponseEntity.ok(ApiResponse.success(project, "우수작 지정이 해제되었습니다."));
    }
}
