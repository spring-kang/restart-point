package com.restartpoint.domain.team.entity;

import com.restartpoint.domain.profile.entity.JobRole;
import com.restartpoint.domain.user.entity.User;
import com.restartpoint.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "team_members")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TeamMember extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JobRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TeamMemberStatus status;

    @Column(length = 500)
    private String applicationMessage;

    @Builder
    public TeamMember(Team team, User user, JobRole role, String applicationMessage) {
        this.team = team;
        this.user = user;
        this.role = role;
        this.status = TeamMemberStatus.PENDING;
        this.applicationMessage = applicationMessage;
    }

    public void accept() {
        this.status = TeamMemberStatus.ACCEPTED;
    }

    public void reject() {
        this.status = TeamMemberStatus.REJECTED;
    }

    public boolean isPending() {
        return this.status == TeamMemberStatus.PENDING;
    }

    public boolean isAccepted() {
        return this.status == TeamMemberStatus.ACCEPTED;
    }
}
