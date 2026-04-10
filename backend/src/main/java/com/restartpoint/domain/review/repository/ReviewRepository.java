package com.restartpoint.domain.review.repository;

import com.restartpoint.domain.review.entity.Review;
import com.restartpoint.domain.review.entity.ReviewType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    /**
     * 프로젝트별 심사 목록 조회
     */
    @Query("SELECT r FROM Review r JOIN FETCH r.reviewer WHERE r.project.id = :projectId ORDER BY r.submittedAt DESC")
    List<Review> findByProjectIdWithReviewer(@Param("projectId") Long projectId);

    /**
     * 프로젝트별, 심사자 유형별 심사 목록 조회
     */
    @Query("SELECT r FROM Review r JOIN FETCH r.reviewer WHERE r.project.id = :projectId AND r.reviewType = :reviewType")
    List<Review> findByProjectIdAndReviewType(@Param("projectId") Long projectId, @Param("reviewType") ReviewType reviewType);

    /**
     * 특정 프로젝트에 대한 특정 사용자의 심사 조회
     */
    Optional<Review> findByProjectIdAndReviewerId(Long projectId, Long reviewerId);

    /**
     * 특정 프로젝트에 대해 사용자가 이미 심사했는지 확인
     */
    boolean existsByProjectIdAndReviewerId(Long projectId, Long reviewerId);

    /**
     * 특정 사용자가 심사한 목록 조회
     */
    @Query("SELECT r FROM Review r JOIN FETCH r.project WHERE r.reviewer.id = :reviewerId ORDER BY r.submittedAt DESC")
    List<Review> findByReviewerIdWithProject(@Param("reviewerId") Long reviewerId);

    /**
     * 프로젝트별 심사 개수 조회
     */
    long countByProjectId(Long projectId);

    /**
     * 프로젝트별, 심사자 유형별 심사 개수 조회
     */
    long countByProjectIdAndReviewType(Long projectId, ReviewType reviewType);

    /**
     * 심사자 유형별 심사 목록 조회
     */
    @Query("SELECT r FROM Review r JOIN FETCH r.scores WHERE r.reviewType = :reviewType")
    List<Review> findByReviewType(@Param("reviewType") ReviewType reviewType);

    /**
     * 시즌별 모든 심사 조회
     */
    @Query("SELECT r FROM Review r " +
           "JOIN FETCH r.project p " +
           "JOIN p.team t " +
           "WHERE t.season.id = :seasonId")
    List<Review> findAllBySeasonId(@Param("seasonId") Long seasonId);
}
