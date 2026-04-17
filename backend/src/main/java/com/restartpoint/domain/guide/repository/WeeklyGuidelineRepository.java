package com.restartpoint.domain.guide.repository;

import com.restartpoint.domain.guide.entity.WeeklyGuideline;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface WeeklyGuidelineRepository extends JpaRepository<WeeklyGuideline, Long> {

    List<WeeklyGuideline> findByTemplateIdOrderByWeekNumberAsc(Long templateId);

    Optional<WeeklyGuideline> findByTemplateIdAndWeekNumber(Long templateId, Integer weekNumber);

    @Query("SELECT wg FROM WeeklyGuideline wg " +
           "JOIN wg.template pt " +
           "WHERE pt.season.id = :seasonId AND pt.active = true " +
           "ORDER BY wg.weekNumber ASC")
    List<WeeklyGuideline> findBySeasonId(@Param("seasonId") Long seasonId);

    @Query("SELECT wg FROM WeeklyGuideline wg " +
           "JOIN wg.template pt " +
           "WHERE pt.season.id = :seasonId AND pt.active = true AND wg.weekNumber = :weekNumber")
    Optional<WeeklyGuideline> findBySeasonIdAndWeekNumber(
            @Param("seasonId") Long seasonId,
            @Param("weekNumber") Integer weekNumber);

    boolean existsByTemplateIdAndWeekNumber(Long templateId, Integer weekNumber);

    @Query("SELECT MAX(wg.weekNumber) FROM WeeklyGuideline wg WHERE wg.template.id = :templateId")
    Optional<Integer> findMaxWeekNumberByTemplateId(@Param("templateId") Long templateId);

    void deleteByTemplateId(Long templateId);
}
