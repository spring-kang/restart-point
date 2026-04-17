package com.restartpoint.domain.guide.repository;

import com.restartpoint.domain.guide.entity.ProjectTemplate;
import com.restartpoint.domain.season.entity.Season;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProjectTemplateRepository extends JpaRepository<ProjectTemplate, Long> {

    List<ProjectTemplate> findBySeasonId(Long seasonId);

    List<ProjectTemplate> findBySeasonIdAndActiveTrue(Long seasonId);

    Optional<ProjectTemplate> findFirstBySeasonIdAndActiveTrue(Long seasonId);

    @Query("SELECT pt FROM ProjectTemplate pt " +
           "LEFT JOIN FETCH pt.guidelines " +
           "WHERE pt.id = :id")
    Optional<ProjectTemplate> findByIdWithGuidelines(@Param("id") Long id);

    @Query("SELECT pt FROM ProjectTemplate pt " +
           "LEFT JOIN FETCH pt.guidelines " +
           "WHERE pt.season.id = :seasonId AND pt.active = true")
    Optional<ProjectTemplate> findActiveBySeasonIdWithGuidelines(@Param("seasonId") Long seasonId);

    boolean existsBySeasonIdAndActiveTrue(Long seasonId);

    @Query("SELECT COUNT(pt) FROM ProjectTemplate pt WHERE pt.season = :season")
    long countBySeason(@Param("season") Season season);
}
