package com.restartpoint.domain.team.dto;

import com.restartpoint.domain.profile.entity.JobRole;
import com.restartpoint.domain.team.entity.InvitationStatus;
import com.restartpoint.domain.team.entity.TeamInvitation;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class TeamInvitationResponse {

    private Long id;

    // 팀 정보
    private Long teamId;
    private String teamName;
    private String teamDescription;
    private Long seasonId;
    private String seasonName;

    // 초대받은 사용자 정보
    private Long invitedUserId;
    private String invitedUserName;
    private String invitedUserEmail;

    // 초대한 사용자 정보 (팀 리더)
    private Long invitedById;
    private String invitedByName;

    // 초대 정보
    private JobRole suggestedRole;
    private InvitationStatus status;
    private String message;
    private Integer matchScore;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;

    public static TeamInvitationResponse from(TeamInvitation invitation) {
        return TeamInvitationResponse.builder()
                .id(invitation.getId())
                .teamId(invitation.getTeam().getId())
                .teamName(invitation.getTeam().getName())
                .teamDescription(invitation.getTeam().getDescription())
                .seasonId(invitation.getTeam().getSeason().getId())
                .seasonName(invitation.getTeam().getSeason().getTitle())
                .invitedUserId(invitation.getInvitedUser().getId())
                .invitedUserName(invitation.getInvitedUser().getName())
                .invitedUserEmail(invitation.getInvitedUser().getEmail())
                .invitedById(invitation.getInvitedBy().getId())
                .invitedByName(invitation.getInvitedBy().getName())
                .suggestedRole(invitation.getSuggestedRole())
                .status(invitation.getStatus())
                .message(invitation.getMessage())
                .matchScore(invitation.getMatchScore())
                .createdAt(invitation.getCreatedAt())
                .expiresAt(invitation.getExpiresAt())
                .build();
    }
}
