package com.restartpoint.domain.project.repository;

import com.restartpoint.domain.project.entity.Checkpoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CheckpointRepository extends JpaRepository<Checkpoint, Long> {

    List<Checkpoint> findByProjectIdOrderByWeekNumberAsc(Long projectId);

    Optional<Checkpoint> findByProjectIdAndWeekNumber(Long projectId, Integer weekNumber);

    boolean existsByProjectIdAndWeekNumber(Long projectId, Integer weekNumber);

    @Query("SELECT c FROM Checkpoint c JOIN FETCH c.project WHERE c.id = :checkpointId")
    Optional<Checkpoint> findByIdWithProject(@Param("checkpointId") Long checkpointId);

    @Query("SELECT MAX(c.weekNumber) FROM Checkpoint c WHERE c.project.id = :projectId")
    Optional<Integer> findMaxWeekNumberByProjectId(@Param("projectId") Long projectId);

    long countByProjectId(Long projectId);
}
