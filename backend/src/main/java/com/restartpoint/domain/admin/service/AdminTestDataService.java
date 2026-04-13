package com.restartpoint.domain.admin.service;

import com.restartpoint.domain.admin.dto.TestDataSeedResponse;
import com.restartpoint.global.exception.BusinessException;
import com.restartpoint.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminTestDataService {

    private static final String REVIEW_SEED_TYPE = "review-e2e";

    private final DataSource dataSource;

    @Value("${app.test-data.enabled:false}")
    private boolean testDataEnabled;

    @Value("classpath:sql/test-data/e2e-review-cleanup.sql")
    private Resource reviewCleanupScript;

    @Value("classpath:sql/test-data/e2e-review-seed.sql")
    private Resource reviewSeedScript;

    public TestDataSeedResponse resetReviewSeed() {
        validateEnabled();

        executeSqlScript(reviewCleanupScript, "review cleanup");
        executeSqlScript(reviewSeedScript, "review seed");

        return TestDataSeedResponse.builder()
                .seedType(REVIEW_SEED_TYPE)
                .cleanupExecuted(true)
                .seedExecuted(true)
                .executedAt(OffsetDateTime.now())
                .build();
    }

    private void validateEnabled() {
        if (!testDataEnabled) {
            throw new BusinessException(ErrorCode.TEST_DATA_DISABLED);
        }
    }

    private void executeSqlScript(Resource resource, String scriptName) {
        try (var connection = dataSource.getConnection();
             var statement = connection.createStatement()) {
            String sql = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
            statement.execute(sql);
            log.info("관리자 테스트 데이터 스크립트 실행 완료 - {}", scriptName);
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR,
                    "테스트 데이터 스크립트를 읽는 중 오류가 발생했습니다.");
        } catch (Exception exception) {
            log.error("관리자 테스트 데이터 스크립트 실행 실패 - {}", scriptName, exception);
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR,
                    "테스트 데이터 시드 실행에 실패했습니다.");
        }
    }
}
