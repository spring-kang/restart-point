package com.restartpoint.domain.team.controller;

import com.restartpoint.domain.team.dto.TeamInvitationRequest;
import com.restartpoint.domain.team.dto.TeamInvitationResponse;
import com.restartpoint.domain.team.service.TeamInvitationService;
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
public class TeamInvitationController {

    private final TeamInvitationService invitationService;

    /**
     * 영입 요청 발송 (팀 리더만 가능)
     */
    @PostMapping("/teams/{teamId}/invitations")
    public ResponseEntity<ApiResponse<TeamInvitationResponse>> sendInvitation(
            @CurrentUser CustomUserPrincipal principal,
            @PathVariable Long teamId,
            @Valid @RequestBody TeamInvitationRequest request) {
        TeamInvitationResponse invitation = invitationService.sendInvitation(
                principal.getUserId(), teamId, request);
        return ResponseEntity.ok(ApiResponse.success(invitation, "영입 요청을 보냈습니다."));
    }

    /**
     * 팀에서 보낸 영입 요청 목록 조회 (팀 리더만 가능)
     */
    @GetMapping("/teams/{teamId}/invitations")
    public ResponseEntity<ApiResponse<List<TeamInvitationResponse>>> getTeamInvitations(
            @CurrentUser CustomUserPrincipal principal,
            @PathVariable Long teamId) {
        List<TeamInvitationResponse> invitations = invitationService.getTeamInvitations(
                principal.getUserId(), teamId);
        return ResponseEntity.ok(ApiResponse.success(invitations));
    }

    /**
     * 영입 요청 취소 (팀 리더만 가능)
     */
    @DeleteMapping("/teams/{teamId}/invitations/{invitationId}")
    public ResponseEntity<ApiResponse<Void>> cancelInvitation(
            @CurrentUser CustomUserPrincipal principal,
            @PathVariable Long teamId,
            @PathVariable Long invitationId) {
        invitationService.cancelInvitation(principal.getUserId(), invitationId);
        return ResponseEntity.ok(ApiResponse.success(null, "영입 요청을 취소했습니다."));
    }

    /**
     * 내가 받은 영입 요청 목록 조회
     */
    @GetMapping("/users/me/invitations")
    public ResponseEntity<ApiResponse<List<TeamInvitationResponse>>> getMyInvitations(
            @CurrentUser CustomUserPrincipal principal) {
        List<TeamInvitationResponse> invitations = invitationService.getMyInvitations(
                principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success(invitations));
    }

    /**
     * 내가 받은 대기 중인 영입 요청 목록 조회
     */
    @GetMapping("/users/me/invitations/pending")
    public ResponseEntity<ApiResponse<List<TeamInvitationResponse>>> getMyPendingInvitations(
            @CurrentUser CustomUserPrincipal principal) {
        List<TeamInvitationResponse> invitations = invitationService.getMyPendingInvitations(
                principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success(invitations));
    }

    /**
     * 대기 중인 영입 요청 수 조회
     */
    @GetMapping("/users/me/invitations/pending/count")
    public ResponseEntity<ApiResponse<Long>> getPendingInvitationCount(
            @CurrentUser CustomUserPrincipal principal) {
        long count = invitationService.getPendingInvitationCount(principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success(count));
    }

    /**
     * 영입 요청 수락
     */
    @PostMapping("/invitations/{invitationId}/accept")
    public ResponseEntity<ApiResponse<TeamInvitationResponse>> acceptInvitation(
            @CurrentUser CustomUserPrincipal principal,
            @PathVariable Long invitationId) {
        TeamInvitationResponse invitation = invitationService.acceptInvitation(
                principal.getUserId(), invitationId);
        return ResponseEntity.ok(ApiResponse.success(invitation, "영입 요청을 수락했습니다."));
    }

    /**
     * 영입 요청 거절
     */
    @PostMapping("/invitations/{invitationId}/reject")
    public ResponseEntity<ApiResponse<TeamInvitationResponse>> rejectInvitation(
            @CurrentUser CustomUserPrincipal principal,
            @PathVariable Long invitationId) {
        TeamInvitationResponse invitation = invitationService.rejectInvitation(
                principal.getUserId(), invitationId);
        return ResponseEntity.ok(ApiResponse.success(invitation, "영입 요청을 거절했습니다."));
    }
}
