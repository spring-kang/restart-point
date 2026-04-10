package com.restartpoint.domain.review.repository;

import com.restartpoint.domain.review.entity.ReviewScore;
import com.restartpoint.domain.review.entity.RubricItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ReviewScoreRepository extends JpaRepository<ReviewScore, Long> {

    /**
     * 심사별 점수 목록 조회
     */
    List<ReviewScore> findByReviewId(Long reviewId);

    /**
     * 프로젝트별, 루브릭 항목별 평균 점수 조회
     */
    @Query("SELECT AVG(rs.score) FROM ReviewScore rs " +
           "WHERE rs.review.project.id = :projectId AND rs.rubricItem = :rubricItem")
    Double findAverageScoreByProjectIdAndRubricItem(@Param("projectId") Long projectId,
                                                     @Param("rubricItem") RubricItem rubricItem);

    /**
     * 프로젝트별 전체 평균 점수 조회
     */
    @Query("SELECT AVG(rs.score) FROM ReviewScore rs WHERE rs.review.project.id = :projectId")
    Double findAverageScoreByProjectId(@Param("projectId") Long projectId);
}
