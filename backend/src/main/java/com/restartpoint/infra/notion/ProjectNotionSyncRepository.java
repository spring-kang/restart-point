package com.restartpoint.infra.notion;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProjectNotionSyncRepository extends JpaRepository<ProjectNotionSync, Long> {

    Optional<ProjectNotionSync> findByProjectId(Long projectId);

    boolean existsByProjectId(Long projectId);

    @Query("SELECT pns FROM ProjectNotionSync pns " +
           "WHERE pns.autoSync = true AND pns.status != 'SYNCING'")
    List<ProjectNotionSync> findAllForAutoSync();

    @Query("SELECT pns FROM ProjectNotionSync pns " +
           "JOIN pns.project p " +
           "JOIN p.team t " +
           "WHERE t.season.id = :seasonId")
    List<ProjectNotionSync> findBySeasonId(@Param("seasonId") Long seasonId);
}
