package com.restartpoint.domain.project.repository;

import com.restartpoint.domain.project.entity.Project;
import com.restartpoint.domain.project.entity.ProjectStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    Optional<Project> findByTeamId(Long teamId);

    boolean existsByTeamId(Long teamId);

    @Query("SELECT p FROM Project p JOIN p.team t WHERE t.season.id = :seasonId")
    Page<Project> findBySeasonId(@Param("seasonId") Long seasonId, Pageable pageable);

    @Query("SELECT p FROM Project p JOIN p.team t WHERE t.season.id = :seasonId AND p.status = :status")
    Page<Project> findBySeasonIdAndStatus(@Param("seasonId") Long seasonId,
                                           @Param("status") ProjectStatus status,
                                           Pageable pageable);

    @Query("SELECT p FROM Project p JOIN FETCH p.team t JOIN FETCH t.leader WHERE p.id = :projectId")
    Optional<Project> findByIdWithTeam(@Param("projectId") Long projectId);

    @Query("SELECT COUNT(p) FROM Project p JOIN p.team t WHERE t.season.id = :seasonId AND p.status = :status")
    long countBySeasonIdAndStatus(@Param("seasonId") Long seasonId, @Param("status") ProjectStatus status);
}
