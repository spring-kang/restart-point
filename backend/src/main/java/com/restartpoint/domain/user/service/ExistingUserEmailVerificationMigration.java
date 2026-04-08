package com.restartpoint.domain.user.service;

import com.restartpoint.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExistingUserEmailVerificationMigration {

    private final UserRepository userRepository;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void markExistingUsersAsVerified() {
        int updatedCount = userRepository.markAllUsersAsEmailVerified();
        if (updatedCount > 0) {
            log.info("기존 사용자 이메일 인증 상태 백필 완료: {}건", updatedCount);
        }
    }
}
