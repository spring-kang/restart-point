package com.restartpoint.domain.profile.repository;

import com.restartpoint.domain.profile.entity.JobRole;
import com.restartpoint.domain.profile.entity.Profile;
import com.restartpoint.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProfileRepository extends JpaRepository<Profile, Long> {

    Optional<Profile> findByUser(User user);

    Optional<Profile> findByUserId(Long userId);

    boolean existsByUser(User user);

    List<Profile> findByJobRole(JobRole jobRole);

    @Query("SELECT p FROM Profile p WHERE p.jobRole = :role AND p.user.certificationStatus = 'APPROVED'")
    List<Profile> findCertifiedProfilesByRole(@Param("role") JobRole role);
}
