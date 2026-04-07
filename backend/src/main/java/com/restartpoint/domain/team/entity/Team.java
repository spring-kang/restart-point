package com.restartpoint.domain.team.entity;

import com.restartpoint.domain.season.entity.Season;
import com.restartpoint.domain.user.entity.User;
import com.restartpoint.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "teams")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Team extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(length = 2000)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "season_id", nullable = false)
    private Season season;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "leader_id", nullable = false)
    private User leader;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TeamStatus status;

    @OneToMany(mappedBy = "team", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TeamMember> members = new ArrayList<>();

    // 모집 중인 역할
    @Column(name = "recruiting_planner")
    private Boolean recruitingPlanner = false;

    @Column(name = "recruiting_uxui")
    private Boolean recruitingUxui = false;

    @Column(name = "recruiting_frontend")
    private Boolean recruitingFrontend = false;

    @Column(name = "recruiting_backend")
    private Boolean recruitingBackend = false;

    @Builder
    public Team(String name, String description, Season season, User leader,
                Boolean recruitingPlanner, Boolean recruitingUxui,
                Boolean recruitingFrontend, Boolean recruitingBackend) {
        this.name = name;
        this.description = description;
        this.season = season;
        this.leader = leader;
        this.status = TeamStatus.RECRUITING;
        this.recruitingPlanner = recruitingPlanner != null ? recruitingPlanner : false;
        this.recruitingUxui = recruitingUxui != null ? recruitingUxui : false;
        this.recruitingFrontend = recruitingFrontend != null ? recruitingFrontend : false;
        this.recruitingBackend = recruitingBackend != null ? recruitingBackend : false;
    }

    public void addMember(TeamMember member) {
        this.members.add(member);
    }

    public void removeMember(TeamMember member) {
        this.members.remove(member);
    }

    public void updateStatus(TeamStatus status) {
        this.status = status;
    }

    public void updateRecruitingRoles(Boolean recruitingPlanner, Boolean recruitingUxui,
                                      Boolean recruitingFrontend, Boolean recruitingBackend) {
        this.recruitingPlanner = recruitingPlanner;
        this.recruitingUxui = recruitingUxui;
        this.recruitingFrontend = recruitingFrontend;
        this.recruitingBackend = recruitingBackend;
    }

    public int getMemberCount() {
        return (int) members.stream()
                .filter(m -> m.getStatus() == TeamMemberStatus.ACCEPTED)
                .count();
    }

    public boolean isFull() {
        return getMemberCount() >= 4;
    }
}
