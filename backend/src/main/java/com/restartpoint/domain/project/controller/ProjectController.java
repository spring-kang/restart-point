package com.restartpoint.domain.project.controller;

import com.restartpoint.domain.project.dto.*;
import com.restartpoint.domain.project.entity.ProjectStatus;
import com.restartpoint.domain.project.service.ProjectService;
import com.restartpoint.global.common.ApiResponse;
import com.restartpoint.global.security.CurrentUser;
import com.restartpoint.global.security.CustomUserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
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

    // 프로젝트 상세 조회
    @GetMapping("/projects/{projectId}")
    public ResponseEntity<ApiResponse<ProjectResponse>> getProject(@PathVariable Long projectId) {
        ProjectResponse project = projectService.getProject(projectId);
        return ResponseEntity.ok(ApiResponse.success(project));
    }

    // 팀의 프로젝트 조회
    @GetMapping("/teams/{teamId}/project")
    public ResponseEntity<ApiResponse<ProjectResponse>> getProjectByTeam(@PathVariable Long teamId) {
        ProjectResponse project = projectService.getProjectByTeam(teamId);
        return ResponseEntity.ok(ApiResponse.success(project));
    }

    // 시즌별 프로젝트 목록 조회
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

    // 체크포인트 목록 조회
    @GetMapping("/projects/{projectId}/checkpoints")
    public ResponseEntity<ApiResponse<List<CheckpointResponse>>> getCheckpoints(@PathVariable Long projectId) {
        List<CheckpointResponse> checkpoints = projectService.getCheckpointsByProject(projectId);
        return ResponseEntity.ok(ApiResponse.success(checkpoints));
    }

    // 체크포인트 상세 조회
    @GetMapping("/checkpoints/{checkpointId}")
    public ResponseEntity<ApiResponse<CheckpointResponse>> getCheckpoint(@PathVariable Long checkpointId) {
        CheckpointResponse checkpoint = projectService.getCheckpoint(checkpointId);
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
}
