package com.restartpoint.domain.team.dto;

import com.restartpoint.domain.team.entity.Team;
import com.restartpoint.domain.team.entity.TeamMemberStatus;
import com.restartpoint.domain.team.entity.TeamStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class TeamResponse {

    private Long id;
    private String name;
    private String description;
    private Long seasonId;
    private String seasonTitle;
    private TeamStatus status;

    // 리더 정보
    private Long leaderId;
    private String leaderName;
    private String leaderEmail;

    // 모집 현황
    private Boolean recruitingPlanner;
    private Boolean recruitingUxui;
    private Boolean recruitingFrontend;
    private Boolean recruitingBackend;

    // 팀원 정보
    private int memberCount;
    private int maxMemberCount;
    private List<TeamMemberResponse> members;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static TeamResponse from(Team team) {
        return TeamResponse.builder()
                .id(team.getId())
                .name(team.getName())
                .description(team.getDescription())
                .seasonId(team.getSeason().getId())
                .seasonTitle(team.getSeason().getTitle())
                .status(team.getStatus())
                .leaderId(team.getLeader().getId())
                .leaderName(team.getLeader().getName())
                .leaderEmail(team.getLeader().getEmail())
                .recruitingPlanner(team.getRecruitingPlanner())
                .recruitingUxui(team.getRecruitingUxui())
                .recruitingFrontend(team.getRecruitingFrontend())
                .recruitingBackend(team.getRecruitingBackend())
                .memberCount(team.getMemberCount())
                .maxMemberCount(4)
                .members(team.getMembers().stream()
                        .filter(m -> m.getStatus() == TeamMemberStatus.ACCEPTED)
                        .map(TeamMemberResponse::from)
                        .toList())
                .createdAt(team.getCreatedAt())
                .updatedAt(team.getUpdatedAt())
                .build();
    }

    public static TeamResponse simpleFrom(Team team) {
        return TeamResponse.builder()
                .id(team.getId())
                .name(team.getName())
                .description(team.getDescription())
                .seasonId(team.getSeason().getId())
                .seasonTitle(team.getSeason().getTitle())
                .status(team.getStatus())
                .leaderId(team.getLeader().getId())
                .leaderName(team.getLeader().getName())
                .recruitingPlanner(team.getRecruitingPlanner())
                .recruitingUxui(team.getRecruitingUxui())
                .recruitingFrontend(team.getRecruitingFrontend())
                .recruitingBackend(team.getRecruitingBackend())
                .memberCount(team.getMemberCount())
                .maxMemberCount(4)
                .createdAt(team.getCreatedAt())
                .build();
    }
}
