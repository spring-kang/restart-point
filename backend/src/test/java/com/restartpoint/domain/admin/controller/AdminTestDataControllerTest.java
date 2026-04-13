package com.restartpoint.domain.admin.controller;

import com.restartpoint.domain.admin.dto.TestDataSeedResponse;
import com.restartpoint.domain.admin.service.AdminTestDataService;
import com.restartpoint.global.security.JwtAuthenticationFilter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminTestDataController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminTestDataControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AdminTestDataService adminTestDataService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    @DisplayName("리뷰 테스트 데이터 재설정 API는 성공 응답을 반환한다")
    void resetReviewSeedReturnsSuccess() throws Exception {
        given(adminTestDataService.resetReviewSeed()).willReturn(TestDataSeedResponse.builder()
                .seedType("review-e2e")
                .cleanupExecuted(true)
                .seedExecuted(true)
                .executedAt(OffsetDateTime.parse("2026-04-13T13:00:00Z"))
                .build());

        mockMvc.perform(post("/api/v1/admin/test-data/review-seed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("리뷰 테스트 데이터가 재설정되었습니다."))
                .andExpect(jsonPath("$.data.seedType").value("review-e2e"))
                .andExpect(jsonPath("$.data.cleanupExecuted").value(true))
                .andExpect(jsonPath("$.data.seedExecuted").value(true));
    }
}
