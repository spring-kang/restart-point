package com.restartpoint.domain.season.controller;

import com.restartpoint.domain.season.dto.SeasonResponse;
import com.restartpoint.domain.season.entity.SeasonStatus;
import com.restartpoint.domain.season.service.SeasonService;
import com.restartpoint.global.exception.BusinessException;
import com.restartpoint.global.exception.ErrorCode;
import com.restartpoint.global.security.CustomUserPrincipal;
import com.restartpoint.global.security.JwtAuthenticationFilter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
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
                .expertReviewWeight(100)
                .candidateReviewWeight(0)
                .currentPhase("테스트")
                .canJoin(status == SeasonStatus.RECRUITING)
                .build();
    }

    private SeasonResponse createSeasonResponseWithParticipation(Long id, SeasonStatus status, Long myTeamId, String myTeamName) {
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
                .expertReviewWeight(100)
                .candidateReviewWeight(0)
                .currentPhase("테스트")
                .canJoin(status == SeasonStatus.RECRUITING)
                .myTeamId(myTeamId)
                .myTeamName(myTeamName)
                .build();
    }

    @Nested
    @DisplayName("선택적 인증 테스트 - 시즌 목록 조회")
    class GetSeasonsWithOptionalAuthTest {

        @Test
        @DisplayName("비로그인 사용자가 시즌 목록을 조회하면 참여 정보 없이 반환된다")
        void getPublicSeasons_withoutAuth_returnsListWithoutParticipation() throws Exception {
            // given - 인증 없음 (SecurityContext 비어있음)
            SecurityContextHolder.clearContext();
            given(seasonService.getPublicSeasons()).willReturn(List.of(
                    createSeasonResponse(1L, SeasonStatus.RECRUITING)
            ));

            // when & then
            mockMvc.perform(get("/api/v1/seasons"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data[0].id").value(1))
                    .andExpect(jsonPath("$.data[0].myTeamId").doesNotExist());

            verify(seasonService).getPublicSeasons();
            verify(seasonService, never()).getPublicSeasonsForUser(1L);
        }

        @Test
        @DisplayName("로그인 사용자가 시즌 목록을 조회하면 참여 정보가 포함된다")
        void getPublicSeasons_withAuth_returnsListWithParticipation() throws Exception {
            // given - 인증 설정
            CustomUserPrincipal principal = new CustomUserPrincipal(1L, "user@test.com", "USER");
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    principal, null, List.of(new SimpleGrantedAuthority("ROLE_USER"))
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);

            given(seasonService.getPublicSeasonsForUser(1L)).willReturn(List.of(
                    createSeasonResponseWithParticipation(1L, SeasonStatus.RECRUITING, 10L, "테스트 팀")
            ));

            // when & then
            mockMvc.perform(get("/api/v1/seasons"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data[0].id").value(1))
                    .andExpect(jsonPath("$.data[0].myTeamId").value(10))
                    .andExpect(jsonPath("$.data[0].myTeamName").value("테스트 팀"));

            verify(seasonService).getPublicSeasonsForUser(1L);
            verify(seasonService, never()).getPublicSeasons();

            // cleanup
            SecurityContextHolder.clearContext();
        }
    }

    @Nested
    @DisplayName("선택적 인증 테스트 - 시즌 상세 조회")
    class GetSeasonDetailWithOptionalAuthTest {

        @Test
        @DisplayName("비로그인 사용자가 시즌 상세를 조회하면 참여 정보 없이 반환된다")
        void getSeason_withoutAuth_returnsDetailWithoutParticipation() throws Exception {
            // given - 인증 없음
            SecurityContextHolder.clearContext();
            given(seasonService.getSeason(1L)).willReturn(createSeasonResponse(1L, SeasonStatus.RECRUITING));

            // when & then
            mockMvc.perform(get("/api/v1/seasons/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(1))
                    .andExpect(jsonPath("$.data.myTeamId").doesNotExist());

            verify(seasonService).getSeason(1L);
            verify(seasonService, never()).getSeasonForUser(1L, 1L);
        }

        @Test
        @DisplayName("로그인 사용자가 시즌 상세를 조회하면 참여 정보가 포함된다")
        void getSeason_withAuth_returnsDetailWithParticipation() throws Exception {
            // given - 인증 설정
            CustomUserPrincipal principal = new CustomUserPrincipal(1L, "user@test.com", "USER");
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    principal, null, List.of(new SimpleGrantedAuthority("ROLE_USER"))
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);

            given(seasonService.getSeasonForUser(1L, 1L)).willReturn(
                    createSeasonResponseWithParticipation(1L, SeasonStatus.RECRUITING, 10L, "테스트 팀")
            );

            // when & then
            mockMvc.perform(get("/api/v1/seasons/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(1))
                    .andExpect(jsonPath("$.data.myTeamId").value(10))
                    .andExpect(jsonPath("$.data.myTeamName").value("테스트 팀"));

            verify(seasonService).getSeasonForUser(1L, 1L);
            verify(seasonService, never()).getSeason(1L);

            // cleanup
            SecurityContextHolder.clearContext();
        }

        @Test
        @DisplayName("로그인 사용자가 참여하지 않은 시즌을 조회하면 참여 정보가 null이다")
        void getSeason_withAuth_noParticipation_returnsNullParticipation() throws Exception {
            // given - 인증 설정
            CustomUserPrincipal principal = new CustomUserPrincipal(1L, "user@test.com", "USER");
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    principal, null, List.of(new SimpleGrantedAuthority("ROLE_USER"))
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);

            given(seasonService.getSeasonForUser(1L, 1L)).willReturn(
                    createSeasonResponseWithParticipation(1L, SeasonStatus.RECRUITING, null, null)
            );

            // when & then
            mockMvc.perform(get("/api/v1/seasons/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(1))
                    .andExpect(jsonPath("$.data.myTeamId").isEmpty())
                    .andExpect(jsonPath("$.data.myTeamName").isEmpty());

            verify(seasonService).getSeasonForUser(1L, 1L);

            // cleanup
            SecurityContextHolder.clearContext();
        }
    }
}
