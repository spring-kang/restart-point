package com.restartpoint.domain.project.repository;

import com.restartpoint.domain.project.entity.MemberProgress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MemberProgressRepository extends JpaRepository<MemberProgress, Long> {

    List<MemberProgress> findByCheckpointId(Long checkpointId);

    Optional<MemberProgress> findByCheckpointIdAndUserId(Long checkpointId, Long userId);

    void deleteByCheckpointId(Long checkpointId);
}
