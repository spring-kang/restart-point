package com.restartpoint.domain.team.controller;

import com.restartpoint.domain.team.dto.TeamApplyRequest;
import com.restartpoint.domain.team.dto.TeamMemberResponse;
import com.restartpoint.domain.team.dto.TeamRequest;
import com.restartpoint.domain.team.dto.TeamResponse;
import com.restartpoint.domain.team.dto.TeamStatusRequest;
import com.restartpoint.domain.team.service.TeamService;
import com.restartpoint.global.common.ApiResponse;
import com.restartpoint.global.security.CurrentUser;
import com.restartpoint.global.security.CustomUserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class TeamController {

    private final TeamService teamService;

    // 팀 생성
    @PostMapping("/teams")
    public ResponseEntity<ApiResponse<TeamResponse>> createTeam(
            @CurrentUser CustomUserPrincipal principal,
            @Valid @RequestBody TeamRequest request) {
        TeamResponse team = teamService.createTeam(principal.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.success(team, "팀이 생성되었습니다."));
    }

    // 팀 상세 조회
    @GetMapping("/teams/{teamId}")
    public ResponseEntity<ApiResponse<TeamResponse>> getTeam(@PathVariable Long teamId) {
        TeamResponse team = teamService.getTeam(teamId);
        return ResponseEntity.ok(ApiResponse.success(team));
    }

    // 시즌별 팀 목록 조회
    @GetMapping("/seasons/{seasonId}/teams")
    public ResponseEntity<ApiResponse<List<TeamResponse>>> getTeamsBySeason(@PathVariable Long seasonId) {
        List<TeamResponse> teams = teamService.getTeamsBySeason(seasonId);
        return ResponseEntity.ok(ApiResponse.success(teams));
    }

    // 시즌별 모집 중인 팀 목록 조회
    @GetMapping("/seasons/{seasonId}/teams/recruiting")
    public ResponseEntity<ApiResponse<List<TeamResponse>>> getRecruitingTeams(@PathVariable Long seasonId) {
        List<TeamResponse> teams = teamService.getRecruitingTeamsBySeason(seasonId);
        return ResponseEntity.ok(ApiResponse.success(teams));
    }

    // 내가 리더인 팀 목록 조회
    @GetMapping("/users/me/teams")
    public ResponseEntity<ApiResponse<List<TeamResponse>>> getMyTeams(
            @CurrentUser CustomUserPrincipal principal) {
        List<TeamResponse> teams = teamService.getMyTeams(principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success(teams));
    }

    // 내가 멤버로 속한 팀 목록 조회
    @GetMapping("/users/me/teams/member")
    public ResponseEntity<ApiResponse<List<TeamResponse>>> getTeamsAsMember(
            @CurrentUser CustomUserPrincipal principal) {
        List<TeamResponse> teams = teamService.getTeamsAsMember(principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success(teams));
    }

    // 팀 정보 수정 (리더만 가능)
    @PutMapping("/teams/{teamId}")
    public ResponseEntity<ApiResponse<TeamResponse>> updateTeam(
            @CurrentUser CustomUserPrincipal principal,
            @PathVariable Long teamId,
            @Valid @RequestBody TeamRequest request) {
        TeamResponse team = teamService.updateTeam(principal.getUserId(), teamId, request);
        return ResponseEntity.ok(ApiResponse.success(team, "팀 정보가 수정되었습니다."));
    }

    // 팀 상태 변경 (리더만 가능)
    @PatchMapping("/teams/{teamId}/status")
    public ResponseEntity<ApiResponse<TeamResponse>> updateTeamStatus(
            @CurrentUser CustomUserPrincipal principal,
            @PathVariable Long teamId,
            @Valid @RequestBody TeamStatusRequest request) {
        TeamResponse team = teamService.updateTeamStatus(principal.getUserId(), teamId, request.getStatus());
        return ResponseEntity.ok(ApiResponse.success(team, "팀 상태가 변경되었습니다."));
    }

    // 팀 지원
    @PostMapping("/teams/{teamId}/applications")
    public ResponseEntity<ApiResponse<TeamMemberResponse>> applyToTeam(
            @CurrentUser CustomUserPrincipal principal,
            @PathVariable Long teamId,
            @Valid @RequestBody TeamApplyRequest request) {
        TeamMemberResponse member = teamService.applyToTeam(principal.getUserId(), teamId, request);
        return ResponseEntity.ok(ApiResponse.success(member, "팀에 지원했습니다."));
    }

    // 팀 지원 목록 조회 (리더만 가능)
    @GetMapping("/teams/{teamId}/applications")
    public ResponseEntity<ApiResponse<List<TeamMemberResponse>>> getTeamApplications(
            @CurrentUser CustomUserPrincipal principal,
            @PathVariable Long teamId) {
        List<TeamMemberResponse> applications = teamService.getTeamApplications(principal.getUserId(), teamId);
        return ResponseEntity.ok(ApiResponse.success(applications));
    }

    // 팀 멤버 목록 조회
    @GetMapping("/teams/{teamId}/members")
    public ResponseEntity<ApiResponse<List<TeamMemberResponse>>> getTeamMembers(@PathVariable Long teamId) {
        List<TeamMemberResponse> members = teamService.getTeamMembers(teamId);
        return ResponseEntity.ok(ApiResponse.success(members));
    }

    // 팀 지원 수락 (리더만 가능)
    @PostMapping("/teams/{teamId}/applications/{memberId}/accept")
    public ResponseEntity<ApiResponse<TeamMemberResponse>> acceptApplication(
            @CurrentUser CustomUserPrincipal principal,
            @PathVariable Long teamId,
            @PathVariable Long memberId) {
        TeamMemberResponse member = teamService.acceptApplication(principal.getUserId(), teamId, memberId);
        return ResponseEntity.ok(ApiResponse.success(member, "지원을 수락했습니다."));
    }

    // 팀 지원 거절 (리더만 가능)
    @PostMapping("/teams/{teamId}/applications/{memberId}/reject")
    public ResponseEntity<ApiResponse<TeamMemberResponse>> rejectApplication(
            @CurrentUser CustomUserPrincipal principal,
            @PathVariable Long teamId,
            @PathVariable Long memberId) {
        TeamMemberResponse member = teamService.rejectApplication(principal.getUserId(), teamId, memberId);
        return ResponseEntity.ok(ApiResponse.success(member, "지원을 거절했습니다."));
    }

    // 팀 탈퇴
    @DeleteMapping("/teams/{teamId}/members/me")
    public ResponseEntity<ApiResponse<Void>> leaveTeam(
            @CurrentUser CustomUserPrincipal principal,
            @PathVariable Long teamId) {
        teamService.leaveTeam(principal.getUserId(), teamId);
        return ResponseEntity.ok(ApiResponse.success(null, "팀에서 탈퇴했습니다."));
    }

    // 내 지원 현황 조회
    @GetMapping("/users/me/applications")
    public ResponseEntity<ApiResponse<List<TeamMemberResponse>>> getMyApplications(
            @CurrentUser CustomUserPrincipal principal) {
        List<TeamMemberResponse> applications = teamService.getMyApplications(principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success(applications));
    }
}
