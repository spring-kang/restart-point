package com.restartpoint.domain.season.controller;

import com.restartpoint.domain.season.dto.SeasonRequest;
import com.restartpoint.domain.season.dto.SeasonResponse;
import com.restartpoint.domain.season.dto.SeasonStatusRequest;
import com.restartpoint.domain.season.service.SeasonService;
import com.restartpoint.global.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class SeasonController {

    private final SeasonService seasonService;

    // 공개 시즌 목록 조회 (모든 사용자)
    @GetMapping("/seasons")
    public ResponseEntity<ApiResponse<List<SeasonResponse>>> getPublicSeasons() {
        List<SeasonResponse> seasons = seasonService.getPublicSeasons();
        return ResponseEntity.ok(ApiResponse.success(seasons));
    }

    // 현재 참여 가능한 시즌 조회
    @GetMapping("/seasons/active")
    public ResponseEntity<ApiResponse<List<SeasonResponse>>> getActiveSeasons() {
        List<SeasonResponse> seasons = seasonService.getActiveSeasons();
        return ResponseEntity.ok(ApiResponse.success(seasons));
    }

    // 시즌 상세 조회
    @GetMapping("/seasons/{seasonId}")
    public ResponseEntity<ApiResponse<SeasonResponse>> getSeason(@PathVariable Long seasonId) {
        SeasonResponse season = seasonService.getSeason(seasonId);
        return ResponseEntity.ok(ApiResponse.success(season));
    }

    // === 운영자 전용 API ===

    // 모든 시즌 목록 조회 (DRAFT 포함)
    @GetMapping("/admin/seasons")
    public ResponseEntity<ApiResponse<Page<SeasonResponse>>> getAllSeasons(
            @PageableDefault(size = 10) Pageable pageable) {
        Page<SeasonResponse> seasons = seasonService.getAllSeasons(pageable);
        return ResponseEntity.ok(ApiResponse.success(seasons));
    }

    // 시즌 상세 조회 (DRAFT 포함)
    @GetMapping("/admin/seasons/{seasonId}")
    public ResponseEntity<ApiResponse<SeasonResponse>> getSeasonForAdmin(@PathVariable Long seasonId) {
        SeasonResponse season = seasonService.getSeasonForAdmin(seasonId);
        return ResponseEntity.ok(ApiResponse.success(season));
    }

    // 시즌 생성
    @PostMapping("/admin/seasons")
    public ResponseEntity<ApiResponse<SeasonResponse>> createSeason(
            @Valid @RequestBody SeasonRequest request) {
        SeasonResponse season = seasonService.createSeason(request);
        return ResponseEntity.ok(ApiResponse.success(season, "시즌이 생성되었습니다."));
    }

    // 시즌 수정
    @PutMapping("/admin/seasons/{seasonId}")
    public ResponseEntity<ApiResponse<SeasonResponse>> updateSeason(
            @PathVariable Long seasonId,
            @Valid @RequestBody SeasonRequest request) {
        SeasonResponse season = seasonService.updateSeason(seasonId, request);
        return ResponseEntity.ok(ApiResponse.success(season, "시즌이 수정되었습니다."));
    }

    // 시즌 상태 변경
    @PatchMapping("/admin/seasons/{seasonId}/status")
    public ResponseEntity<ApiResponse<SeasonResponse>> updateSeasonStatus(
            @PathVariable Long seasonId,
            @Valid @RequestBody SeasonStatusRequest request) {
        SeasonResponse season = seasonService.updateSeasonStatus(seasonId, request.getStatus());
        return ResponseEntity.ok(ApiResponse.success(season, "시즌 상태가 변경되었습니다."));
    }

    // 시즌 삭제
    @DeleteMapping("/admin/seasons/{seasonId}")
    public ResponseEntity<ApiResponse<Void>> deleteSeason(@PathVariable Long seasonId) {
        seasonService.deleteSeason(seasonId);
        return ResponseEntity.ok(ApiResponse.success(null, "시즌이 삭제되었습니다."));
    }
}
