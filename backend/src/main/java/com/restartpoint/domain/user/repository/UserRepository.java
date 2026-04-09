package com.restartpoint.domain.user.repository;

import com.restartpoint.domain.user.entity.CertificationStatus;
import com.restartpoint.domain.user.entity.Role;
import com.restartpoint.domain.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    List<User> findByCertificationStatus(CertificationStatus status);

    // 회원 목록 조회 (검색, 필터링)
    @Query("SELECT u FROM User u WHERE " +
           "(:keyword IS NULL OR u.name LIKE %:keyword% OR u.email LIKE %:keyword%) AND " +
           "(:role IS NULL OR u.role = :role) AND " +
           "(:certificationStatus IS NULL OR u.certificationStatus = :certificationStatus)")
    Page<User> findAllWithFilters(
            @Param("keyword") String keyword,
            @Param("role") Role role,
            @Param("certificationStatus") CertificationStatus certificationStatus,
            Pageable pageable
    );

    @Modifying
    @Query("UPDATE User u SET u.emailVerified = true WHERE u.emailVerified = false")
    int markAllUsersAsEmailVerified();

    // 관리자 수 조회
    long countByRole(Role role);
}
