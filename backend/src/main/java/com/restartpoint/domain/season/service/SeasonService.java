package com.restartpoint.domain.season.service;

import com.restartpoint.domain.season.dto.SeasonRequest;
import com.restartpoint.domain.season.dto.SeasonResponse;
import com.restartpoint.domain.season.entity.Season;
import com.restartpoint.domain.season.entity.SeasonStatus;
import com.restartpoint.domain.season.repository.SeasonRepository;
import com.restartpoint.global.exception.BusinessException;
import com.restartpoint.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SeasonService {

    private final SeasonRepository seasonRepository;

    // 시즌 생성 (운영자 전용)
    @Transactional
    public SeasonResponse createSeason(SeasonRequest request) {
        validateDateOrder(request);
        validateReviewWeights(request);

        Season season = Season.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .recruitmentStartAt(request.getRecruitmentStartAt())
                .recruitmentEndAt(request.getRecruitmentEndAt())
                .teamBuildingStartAt(request.getTeamBuildingStartAt())
                .teamBuildingEndAt(request.getTeamBuildingEndAt())
                .projectStartAt(request.getProjectStartAt())
                .projectEndAt(request.getProjectEndAt())
                .reviewStartAt(request.getReviewStartAt())
                .reviewEndAt(request.getReviewEndAt())
                .expertReviewWeight(request.getExpertReviewWeight())
                .candidateReviewWeight(request.getCandidateReviewWeight())
                .build();

        Season savedSeason = seasonRepository.save(season);
        return SeasonResponse.from(savedSeason);
    }

    // 시즌 수정 (운영자 전용)
    @Transactional
    public SeasonResponse updateSeason(Long seasonId, SeasonRequest request) {
        Season season = findSeasonById(seasonId);

        // DRAFT 상태에서만 수정 가능
        if (season.getStatus() != SeasonStatus.DRAFT) {
            throw new BusinessException(ErrorCode.INVALID_SEASON_STATUS);
        }

        validateDateOrder(request);
        validateReviewWeights(request);

        season.update(
                request.getTitle(),
                request.getDescription(),
                request.getRecruitmentStartAt(),
                request.getRecruitmentEndAt(),
                request.getTeamBuildingStartAt(),
                request.getTeamBuildingEndAt(),
                request.getProjectStartAt(),
                request.getProjectEndAt(),
                request.getReviewStartAt(),
                request.getReviewEndAt(),
                request.getExpertReviewWeight(),
                request.getCandidateReviewWeight()
        );

        return SeasonResponse.from(season);
    }

    // 시즌 상태 변경 (운영자 전용)
    @Transactional
    public SeasonResponse updateSeasonStatus(Long seasonId, SeasonStatus newStatus) {
        Season season = findSeasonById(seasonId);
        validateStatusTransition(season.getStatus(), newStatus);
        season.updateStatus(newStatus);
        return SeasonResponse.from(season);
    }

    // 시즌 삭제 (운영자 전용, DRAFT 상태만)
    @Transactional
    public void deleteSeason(Long seasonId) {
        Season season = findSeasonById(seasonId);

        if (season.getStatus() != SeasonStatus.DRAFT) {
            throw new BusinessException(ErrorCode.INVALID_SEASON_STATUS);
        }

        seasonRepository.delete(season);
    }

    // 시즌 상세 조회 (공개 API - DRAFT 제외)
    public SeasonResponse getSeason(Long seasonId) {
        Season season = findSeasonById(seasonId);

        // DRAFT 상태의 시즌은 공개 API에서 접근 불가
        if (season.getStatus() == SeasonStatus.DRAFT) {
            throw new BusinessException(ErrorCode.SEASON_NOT_FOUND);
        }

        return SeasonResponse.from(season);
    }

    // 시즌 상세 조회 (운영자용 - DRAFT 포함)
    public SeasonResponse getSeasonForAdmin(Long seasonId) {
        Season season = findSeasonById(seasonId);
        return SeasonResponse.from(season);
    }

    // 모든 시즌 목록 조회 (운영자용 - DRAFT 포함)
    public Page<SeasonResponse> getAllSeasons(Pageable pageable) {
        return seasonRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(SeasonResponse::from);
    }

    // 공개 시즌 목록 조회 (사용자용 - DRAFT 제외)
    public List<SeasonResponse> getPublicSeasons() {
        return seasonRepository.findByStatusNotOrderByRecruitmentStartAtDesc(SeasonStatus.DRAFT)
                .stream()
                .map(SeasonResponse::from)
                .toList();
    }

    // 현재 참여 가능한 시즌 조회 (모집 중 또는 팀빌딩 중)
    public List<SeasonResponse> getActiveSeasons() {
        return seasonRepository.findActiveSeasons()
                .stream()
                .map(SeasonResponse::from)
                .toList();
    }

    private Season findSeasonById(Long seasonId) {
        return seasonRepository.findById(seasonId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SEASON_NOT_FOUND));
    }

    private void validateDateOrder(SeasonRequest request) {
        // 날짜 순서 검증: 모집 -> 팀빌딩 -> 프로젝트 -> 심사
        if (request.getRecruitmentStartAt().isAfter(request.getRecruitmentEndAt())) {
            throw new BusinessException(ErrorCode.INVALID_DATE_RANGE);
        }
        if (request.getRecruitmentEndAt().isAfter(request.getTeamBuildingStartAt())) {
            throw new BusinessException(ErrorCode.INVALID_DATE_RANGE);
        }
        if (request.getTeamBuildingStartAt().isAfter(request.getTeamBuildingEndAt())) {
            throw new BusinessException(ErrorCode.INVALID_DATE_RANGE);
        }
        if (request.getTeamBuildingEndAt().isAfter(request.getProjectStartAt())) {
            throw new BusinessException(ErrorCode.INVALID_DATE_RANGE);
        }
        if (request.getProjectStartAt().isAfter(request.getProjectEndAt())) {
            throw new BusinessException(ErrorCode.INVALID_DATE_RANGE);
        }
        if (request.getProjectEndAt().isAfter(request.getReviewStartAt())) {
            throw new BusinessException(ErrorCode.INVALID_DATE_RANGE);
        }
        if (request.getReviewStartAt().isAfter(request.getReviewEndAt())) {
            throw new BusinessException(ErrorCode.INVALID_DATE_RANGE);
        }
    }

    private void validateReviewWeights(SeasonRequest request) {
        int total = request.getExpertReviewWeight() + request.getCandidateReviewWeight();
        if (total != 100) {
            throw new BusinessException(ErrorCode.INVALID_REVIEW_WEIGHT);
        }
    }

    private void validateStatusTransition(SeasonStatus current, SeasonStatus next) {
        // 상태 전환 규칙 검증
        boolean valid = switch (current) {
            case DRAFT -> next == SeasonStatus.RECRUITING;
            case RECRUITING -> next == SeasonStatus.TEAM_BUILDING;
            case TEAM_BUILDING -> next == SeasonStatus.IN_PROGRESS;
            case IN_PROGRESS -> next == SeasonStatus.REVIEWING;
            case REVIEWING -> next == SeasonStatus.COMPLETED;
            case COMPLETED -> false;
        };

        if (!valid) {
            throw new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION);
        }
    }
}
