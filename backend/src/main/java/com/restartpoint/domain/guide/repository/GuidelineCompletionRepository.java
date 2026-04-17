package com.restartpoint.domain.guide.repository;

import com.restartpoint.domain.guide.entity.GuidelineCompletion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GuidelineCompletionRepository extends JpaRepository<GuidelineCompletion, Long> {

    Optional<GuidelineCompletion> findByUserIdAndGuidelineId(Long userId, Long guidelineId);

    List<GuidelineCompletion> findByUserId(Long userId);

    List<GuidelineCompletion> findByGuidelineId(Long guidelineId);

    @Query("SELECT gc FROM GuidelineCompletion gc " +
           "JOIN gc.guideline wg " +
           "JOIN wg.template pt " +
           "WHERE gc.user.id = :userId AND pt.season.id = :seasonId " +
           "ORDER BY wg.weekNumber ASC")
    List<GuidelineCompletion> findByUserIdAndSeasonId(
            @Param("userId") Long userId,
            @Param("seasonId") Long seasonId);

    @Query("SELECT gc FROM GuidelineCompletion gc " +
           "JOIN gc.guideline wg " +
           "WHERE gc.user.id = :userId AND wg.template.id = :templateId " +
           "ORDER BY wg.weekNumber ASC")
    List<GuidelineCompletion> findByUserIdAndTemplateId(
            @Param("userId") Long userId,
            @Param("templateId") Long templateId);

    @Query("SELECT COUNT(gc) FROM GuidelineCompletion gc " +
           "JOIN gc.guideline wg " +
           "WHERE gc.user.id = :userId AND wg.template.id = :templateId AND gc.completed = true")
    long countCompletedByUserIdAndTemplateId(
            @Param("userId") Long userId,
            @Param("templateId") Long templateId);

    @Query("SELECT COUNT(wg) FROM WeeklyGuideline wg WHERE wg.template.id = :templateId")
    long countTotalGuidelinesByTemplateId(@Param("templateId") Long templateId);

    boolean existsByUserIdAndGuidelineId(Long userId, Long guidelineId);

    Optional<GuidelineCompletion> findByCheckpointId(Long checkpointId);

    List<GuidelineCompletion> findByUserIdAndCompletedTrue(Long userId);
}
