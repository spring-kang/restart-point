package com.restartpoint.domain.report.repository;

import com.restartpoint.domain.report.entity.GrowthReport;
import com.restartpoint.domain.report.entity.ReportType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GrowthReportRepository extends JpaRepository<GrowthReport, Long> {

    // 프로젝트의 팀 리포트 조회
    @Query("SELECT gr FROM GrowthReport gr " +
           "JOIN FETCH gr.project p " +
           "WHERE gr.project.id = :projectId AND gr.reportType = 'TEAM'")
    Optional<GrowthReport> findTeamReportByProjectId(@Param("projectId") Long projectId);

    // 프로젝트의 개인 리포트 조회
    @Query("SELECT gr FROM GrowthReport gr " +
           "JOIN FETCH gr.project p " +
           "JOIN FETCH gr.user u " +
           "WHERE gr.project.id = :projectId AND gr.user.id = :userId AND gr.reportType = 'INDIVIDUAL'")
    Optional<GrowthReport> findIndividualReport(
            @Param("projectId") Long projectId,
            @Param("userId") Long userId);

    // 사용자의 모든 개인 리포트 조회
    @Query("SELECT gr FROM GrowthReport gr " +
           "JOIN FETCH gr.project p " +
           "JOIN FETCH p.team t " +
           "WHERE gr.user.id = :userId AND gr.reportType = 'INDIVIDUAL' " +
           "ORDER BY gr.createdAt DESC")
    List<GrowthReport> findAllByUserId(@Param("userId") Long userId);

    // 프로젝트의 모든 리포트 조회
    @Query("SELECT gr FROM GrowthReport gr " +
           "LEFT JOIN FETCH gr.user u " +
           "WHERE gr.project.id = :projectId " +
           "ORDER BY gr.reportType, gr.createdAt")
    List<GrowthReport> findAllByProjectId(@Param("projectId") Long projectId);

    // 리포트 존재 여부 확인
    boolean existsByProjectIdAndReportType(Long projectId, ReportType reportType);

    boolean existsByProjectIdAndUserIdAndReportType(Long projectId, Long userId, ReportType reportType);

    // 시즌의 모든 리포트 조회 (운영자용)
    @Query("SELECT gr FROM GrowthReport gr " +
           "JOIN FETCH gr.project p " +
           "JOIN FETCH p.team t " +
           "WHERE t.season.id = :seasonId " +
           "ORDER BY p.id, gr.reportType")
    List<GrowthReport> findAllBySeasonId(@Param("seasonId") Long seasonId);

    // 미생성 리포트 조회 (재시도용)
    @Query("SELECT gr FROM GrowthReport gr " +
           "WHERE gr.generated = false")
    List<GrowthReport> findAllNotGenerated();
}
