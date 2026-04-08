package com.restartpoint.domain.team.dto;

import com.restartpoint.domain.profile.entity.JobRole;
import com.restartpoint.domain.team.entity.TeamMember;
import com.restartpoint.domain.team.entity.TeamMemberStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class TeamMemberResponse {

    private Long id;
    private Long userId;
    private String userName;
    private String userEmail;
    private JobRole role;
    private TeamMemberStatus status;
    private String applicationMessage;
    private LocalDateTime createdAt;

    public static TeamMemberResponse from(TeamMember teamMember) {
        return TeamMemberResponse.builder()
                .id(teamMember.getId())
                .userId(teamMember.getUser().getId())
                .userName(teamMember.getUser().getName())
                .userEmail(teamMember.getUser().getEmail())
                .role(teamMember.getRole())
                .status(teamMember.getStatus())
                .applicationMessage(teamMember.getApplicationMessage())
                .createdAt(teamMember.getCreatedAt())
                .build();
    }
}
