package com.restartpoint.domain.season.controller;

import com.restartpoint.domain.season.dto.SeasonResponse;
import com.restartpoint.domain.season.entity.SeasonStatus;
import com.restartpoint.domain.season.service.SeasonService;
import com.restartpoint.global.exception.BusinessException;
import com.restartpoint.global.exception.ErrorCode;
import com.restartpoint.global.security.JwtAuthenticationFilter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SeasonController.class)
@AutoConfigureMockMvc(addFilters = false)
class SeasonControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SeasonService seasonService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    @DisplayName("공개 시즌 상세 조회에서 DRAFT 시즌이면 404를 반환한다")
    void getSeasonReturnsNotFoundForDraftSeason() throws Exception {
        given(seasonService.getSeason(1L))
                .willThrow(new BusinessException(ErrorCode.SEASON_NOT_FOUND));

        mockMvc.perform(get("/api/v1/seasons/1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("SEASON_001"))
                .andExpect(jsonPath("$.message").value("시즌을 찾을 수 없습니다."));
    }

    @Test
    @DisplayName("운영자 시즌 상세 조회는 DRAFT 시즌도 반환한다")
    void getSeasonForAdminReturnsDraftSeason() throws Exception {
        given(seasonService.getSeasonForAdmin(1L)).willReturn(createSeasonResponse(1L, SeasonStatus.DRAFT));

        mockMvc.perform(get("/api/v1/admin/seasons/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.status").value("DRAFT"));
    }

    @Test
    @DisplayName("현재 참여 가능한 시즌 조회는 모집 중과 팀빌딩 중 시즌을 모두 반환한다")
    void getActiveSeasonsReturnsRecruitingAndTeamBuilding() throws Exception {
        given(seasonService.getActiveSeasons()).willReturn(List.of(
                createSeasonResponse(1L, SeasonStatus.RECRUITING),
                createSeasonResponse(2L, SeasonStatus.TEAM_BUILDING)
        ));

        mockMvc.perform(get("/api/v1/seasons/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].status").value("RECRUITING"))
                .andExpect(jsonPath("$.data[1].status").value("TEAM_BUILDING"));
    }

    private SeasonResponse createSeasonResponse(Long id, SeasonStatus status) {
        LocalDateTime now = LocalDateTime.of(2026, 4, 1, 0, 0);
        return SeasonResponse.builder()
                .id(id)
                .title("테스트 시즌")
                .description("설명")
                .status(status)
                .recruitmentStartAt(now)
                .recruitmentEndAt(now.plusDays(7))
                .teamBuildingStartAt(now.plusDays(8))
                .teamBuildingEndAt(now.plusDays(14))
                .projectStartAt(now.plusDays(15))
                .projectEndAt(now.plusDays(30))
                .reviewStartAt(now.plusDays(31))
                .reviewEndAt(now.plusDays(37))
                .expertReviewWeight(70)
                .candidateReviewWeight(30)
                .currentPhase("테스트")
                .canJoin(status == SeasonStatus.RECRUITING)
                .build();
    }
}
