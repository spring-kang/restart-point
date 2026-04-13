package com.restartpoint.domain.team.entity;

import com.restartpoint.domain.profile.entity.JobRole;
import com.restartpoint.domain.user.entity.User;
import com.restartpoint.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "team_invitations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TeamInvitation extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invited_user_id", nullable = false)
    private User invitedUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invited_by_id", nullable = false)
    private User invitedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JobRole suggestedRole;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InvitationStatus status;

    @Column(length = 500)
    private String message;

    @Column
    private Integer matchScore;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Builder
    public TeamInvitation(Team team, User invitedUser, User invitedBy,
                          JobRole suggestedRole, String message, Integer matchScore) {
        this.team = team;
        this.invitedUser = invitedUser;
        this.invitedBy = invitedBy;
        this.suggestedRole = suggestedRole;
        this.message = message;
        this.matchScore = matchScore;
        this.status = InvitationStatus.PENDING;
        this.expiresAt = LocalDateTime.now().plusDays(7); // 7일 후 만료
    }

    public void accept() {
        this.status = InvitationStatus.ACCEPTED;
    }

    public void reject() {
        this.status = InvitationStatus.REJECTED;
    }

    public void expire() {
        this.status = InvitationStatus.EXPIRED;
    }

    public boolean isPending() {
        return this.status == InvitationStatus.PENDING;
    }

    public boolean isExpired() {
        return this.status == InvitationStatus.EXPIRED ||
               LocalDateTime.now().isAfter(this.expiresAt);
    }
}
