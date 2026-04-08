package com.restartpoint.domain.matching.controller;

import com.restartpoint.domain.matching.dto.MemberRecommendationResponse;
import com.restartpoint.domain.matching.dto.TeamRecommendationResponse;
import com.restartpoint.domain.matching.service.TeamMatchingService;
import com.restartpoint.global.common.ApiResponse;
import com.restartpoint.global.security.CurrentUser;
import com.restartpoint.global.security.CustomUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/matching")
@RequiredArgsConstructor
public class TeamMatchingController {

    private final TeamMatchingService teamMatchingService;

    /**
     * 사용자에게 맞는 팀 추천 (팀에 참가하려는 사용자용)
     */
    @GetMapping("/teams")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<TeamRecommendationResponse>>> recommendTeams(
            @CurrentUser CustomUserPrincipal principal,
            @RequestParam Long seasonId,
            @RequestParam(defaultValue = "5") int limit) {
        List<TeamRecommendationResponse> recommendations =
                teamMatchingService.recommendTeamsForUser(principal.getUserId(), seasonId, limit);
        return ResponseEntity.ok(ApiResponse.success(recommendations));
    }

    /**
     * 팀에 맞는 멤버 추천 (팀 리더용)
     */
    @GetMapping("/teams/{teamId}/members")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<MemberRecommendationResponse>>> recommendMembers(
            @CurrentUser CustomUserPrincipal principal,
            @PathVariable Long teamId,
            @RequestParam(defaultValue = "5") int limit) {
        List<MemberRecommendationResponse> recommendations =
                teamMatchingService.recommendMembersForTeam(principal.getUserId(), teamId, limit);
        return ResponseEntity.ok(ApiResponse.success(recommendations));
    }
}
