package com.restartpoint.global.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;

@Slf4j
@Configuration
@Profile("prod")
public class DatabaseMigrationConfig {

    private final JdbcTemplate jdbcTemplate;

    public DatabaseMigrationConfig(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void runMigrations() {
        log.info("데이터베이스 마이그레이션 시작...");

        try {
            // email_verified 컬럼이 없으면 추가
            jdbcTemplate.execute("""
                DO $$
                BEGIN
                    IF NOT EXISTS (
                        SELECT 1 FROM information_schema.columns
                        WHERE table_name = 'users' AND column_name = 'email_verified'
                    ) THEN
                        ALTER TABLE users ADD COLUMN email_verified boolean DEFAULT false;
                    END IF;
                END $$;
            """);

            // 기존 NULL 값을 false로 업데이트
            jdbcTemplate.execute("""
                UPDATE users SET email_verified = false WHERE email_verified IS NULL
            """);

            log.info("데이터베이스 마이그레이션 완료");
        } catch (Exception e) {
            log.warn("마이그레이션 중 오류 (무시 가능): {}", e.getMessage());
        }
    }
}
