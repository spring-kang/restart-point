package com.restartpoint.domain.admin.service;

import com.restartpoint.domain.admin.dto.TestDataSeedResponse;
import com.restartpoint.global.exception.BusinessException;
import com.restartpoint.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.test.util.ReflectionTestUtils;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AdminTestDataServiceTest {

    @Mock
    private DataSource dataSource;

    @Mock
    private Connection connection;

    @Mock
    private Statement statement;

    @Test
    @DisplayName("테스트 데이터 API가 비활성화되어 있으면 예외를 던진다")
    void resetReviewSeedFailsWhenDisabled() {
        AdminTestDataService service = new AdminTestDataService(dataSource);
        ReflectionTestUtils.setField(service, "testDataEnabled", false);

        assertThatThrownBy(service::resetReviewSeed)
                .isInstanceOf(BusinessException.class)
                .satisfies(exception ->
                        assertThat(((BusinessException) exception).getErrorCode()).isEqualTo(ErrorCode.TEST_DATA_DISABLED));
    }

    @Test
    @DisplayName("리뷰 테스트 데이터 재설정은 cleanup 후 seed를 순서대로 실행한다")
    void resetReviewSeedExecutesCleanupAndSeed() throws Exception {
        AdminTestDataService service = new AdminTestDataService(dataSource);
        ReflectionTestUtils.setField(service, "testDataEnabled", true);
        ReflectionTestUtils.setField(service, "reviewCleanupScript",
                new ByteArrayResource("DELETE FROM reviews;".getBytes(StandardCharsets.UTF_8)));
        ReflectionTestUtils.setField(service, "reviewSeedScript",
                new ByteArrayResource("INSERT INTO reviews VALUES (1);".getBytes(StandardCharsets.UTF_8)));

        given(dataSource.getConnection()).willReturn(connection, connection);
        given(connection.createStatement()).willReturn(statement, statement);

        TestDataSeedResponse response = service.resetReviewSeed();

        assertThat(response.getSeedType()).isEqualTo("review-e2e");
        assertThat(response.isCleanupExecuted()).isTrue();
        assertThat(response.isSeedExecuted()).isTrue();
        assertThat(response.getExecutedAt()).isNotNull();

        InOrder inOrder = inOrder(statement);
        inOrder.verify(statement).execute("DELETE FROM reviews;");
        inOrder.verify(statement).execute("INSERT INTO reviews VALUES (1);");

        verify(dataSource, times(2)).getConnection();
        verify(connection, times(2)).createStatement();
        verify(statement, times(2)).close();
        verify(connection, times(2)).close();
    }
}
