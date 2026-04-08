package com.restartpoint.domain.season.service;

import com.restartpoint.domain.season.dto.SeasonRequest;
import com.restartpoint.domain.season.dto.SeasonResponse;
import com.restartpoint.domain.season.entity.Season;
import com.restartpoint.domain.season.entity.SeasonStatus;
import com.restartpoint.domain.season.repository.SeasonRepository;
import com.restartpoint.global.exception.BusinessException;
import com.restartpoint.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class SeasonServiceTest {

    @Mock
    private SeasonRepository seasonRepository;

    @InjectMocks
    private SeasonService seasonService;

    @Test
    @DisplayName("공개 시즌 상세 조회에서 DRAFT 시즌은 조회할 수 없다")
    void getSeasonFailsForDraftSeason() {
        Season draftSeason = createSeason(1L, SeasonStatus.DRAFT);
        given(seasonRepository.findById(1L)).willReturn(Optional.of(draftSeason));

        assertThatThrownBy(() -> seasonService.getSeason(1L))
                .isInstanceOf(BusinessException.class)
                .satisfies(exception -> {
                    BusinessException businessException = (BusinessException) exception;
                    assertThat(businessException.getErrorCode()).isEqualTo(ErrorCode.SEASON_NOT_FOUND);
                });
    }

    @Test
    @DisplayName("운영자 시즌 상세 조회에서는 DRAFT 시즌도 조회할 수 있다")
    void getSeasonForAdminReturnsDraftSeason() {
        Season draftSeason = createSeason(1L, SeasonStatus.DRAFT);
        given(seasonRepository.findById(1L)).willReturn(Optional.of(draftSeason));

        SeasonResponse response = seasonService.getSeasonForAdmin(1L);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getStatus()).isEqualTo(SeasonStatus.DRAFT);
    }

    @Test
    @DisplayName("현재 참여 가능한 시즌 조회는 모집 중과 팀빌딩 중 시즌을 모두 반환한다")
    void getActiveSeasonsReturnsRecruitingAndTeamBuilding() {
        Season recruitingSeason = createSeason(1L, SeasonStatus.RECRUITING);
        Season teamBuildingSeason = createSeason(2L, SeasonStatus.TEAM_BUILDING);
        given(seasonRepository.findActiveSeasons()).willReturn(List.of(recruitingSeason, teamBuildingSeason));

        List<SeasonResponse> responses = seasonService.getActiveSeasons();

        assertThat(responses).hasSize(2);
        assertThat(responses).extracting(SeasonResponse::getStatus)
                .containsExactly(SeasonStatus.RECRUITING, SeasonStatus.TEAM_BUILDING);
    }

    @Test
    @DisplayName("운영자 시즌 목록 조회는 DRAFT 시즌도 포함한다")
    void getAllSeasonsIncludesDraft() {
        Season draftSeason = createSeason(1L, SeasonStatus.DRAFT);
        Season publishedSeason = createSeason(2L, SeasonStatus.RECRUITING);
        given(seasonRepository.findAllByOrderByCreatedAtDesc(Pageable.ofSize(10)))
                .willReturn(new PageImpl<>(List.of(draftSeason, publishedSeason)));

        var responses = seasonService.getAllSeasons(Pageable.ofSize(10));

        assertThat(responses.getContent()).hasSize(2);
        assertThat(responses.getContent()).extracting(SeasonResponse::getStatus)
                .containsExactly(SeasonStatus.DRAFT, SeasonStatus.RECRUITING);
    }

    @Test
    @DisplayName("공개 시즌 목록 조회는 DRAFT 시즌을 제외한다")
    void getPublicSeasonsExcludesDraft() {
        Season recruitingSeason = createSeason(2L, SeasonStatus.RECRUITING);
        given(seasonRepository.findByStatusNotOrderByRecruitmentStartAtDesc(SeasonStatus.DRAFT))
                .willReturn(List.of(recruitingSeason));

        List<SeasonResponse> responses = seasonService.getPublicSeasons();

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getStatus()).isEqualTo(SeasonStatus.RECRUITING);
    }

    private Season createSeason(Long id, SeasonStatus status) {
        LocalDateTime base = LocalDateTime.of(2026, 4, 1, 0, 0);
        Season season = Season.builder()
                .title("테스트 시즌")
                .description("설명")
                .recruitmentStartAt(base)
                .recruitmentEndAt(base.plusDays(7))
                .teamBuildingStartAt(base.plusDays(8))
                .teamBuildingEndAt(base.plusDays(14))
                .projectStartAt(base.plusDays(15))
                .projectEndAt(base.plusDays(30))
                .reviewStartAt(base.plusDays(31))
                .reviewEndAt(base.plusDays(37))
                .expertReviewWeight(70)
                .candidateReviewWeight(30)
                .build();
        setField(season, "id", id);
        setField(season, "status", status);
        return season;
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("테스트 필드 설정에 실패했습니다: " + fieldName, exception);
        }
    }
}
