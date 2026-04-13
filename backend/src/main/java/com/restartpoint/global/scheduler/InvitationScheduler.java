package com.restartpoint.global.scheduler;

import com.restartpoint.domain.team.service.TeamInvitationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 영입 요청 관련 스케줄러
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class InvitationScheduler {

    private final TeamInvitationService invitationService;

    /**
     * 매일 자정에 만료된 영입 요청을 EXPIRED 상태로 변경
     */
    @Scheduled(cron = "0 0 0 * * *")
    public void expireOldInvitations() {
        log.info("만료된 영입 요청 처리 시작");
        int expiredCount = invitationService.expireOldInvitations();
        log.info("만료된 영입 요청 처리 완료: {}건", expiredCount);
    }
}
