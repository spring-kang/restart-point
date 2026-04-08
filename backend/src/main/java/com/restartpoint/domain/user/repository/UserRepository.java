package com.restartpoint.domain.user.repository;

import com.restartpoint.domain.user.entity.CertificationStatus;
import com.restartpoint.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    List<User> findByCertificationStatus(CertificationStatus status);

    @Modifying
    @Query("UPDATE User u SET u.emailVerified = true WHERE u.emailVerified = false")
    int markAllUsersAsEmailVerified();
}
