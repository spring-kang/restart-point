package com.restartpoint.domain.season.repository;

import com.restartpoint.domain.season.entity.Season;
import com.restartpoint.domain.season.entity.SeasonStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface SeasonRepository extends JpaRepository<Season, Long> {

    List<Season> findByStatus(SeasonStatus status);

    List<Season> findByStatusNot(SeasonStatus status);

    @Query("SELECT s FROM Season s WHERE s.status != 'DRAFT' ORDER BY s.recruitmentStartAt DESC")
    List<Season> findAllPublicSeasons();

    @Query("SELECT s FROM Season s WHERE s.status IN ('RECRUITING', 'TEAM_BUILDING') ORDER BY s.recruitmentStartAt DESC")
    List<Season> findActiveSeasons();

    Optional<Season> findFirstByStatusOrderByRecruitmentStartAtDesc(SeasonStatus status);
}
