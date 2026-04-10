package com.restartpoint.domain.user.service;

import com.restartpoint.domain.notification.service.NotificationService;
import com.restartpoint.domain.user.dto.CertificationRequest;
import com.restartpoint.domain.user.dto.UserResponse;
import com.restartpoint.domain.user.entity.CertificationStatus;
import com.restartpoint.domain.user.entity.Role;
import com.restartpoint.domain.user.entity.User;
import com.restartpoint.domain.user.repository.UserRepository;
import com.restartpoint.global.exception.BusinessException;
import com.restartpoint.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public UserResponse getMe(Long userId) {
        User user = findUserById(userId);
        return UserResponse.from(user);
    }

    @Transactional
    public UserResponse requestCertification(Long userId, CertificationRequest request) {
        User user = findUserById(userId);

        // 이미 인증 승인된 경우
        if (user.getCertificationStatus() == CertificationStatus.APPROVED) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "이미 수료 인증이 완료되었습니다.");
        }

        // 인증 대기 중인 경우 재신청 방지
        if (user.getCertificationStatus() == CertificationStatus.PENDING) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "이미 수료 인증 요청이 진행 중입니다. 운영자 검토를 기다려주세요.");
        }

        user.requestCertification(
                request.getBootcampName(),
                request.getBootcampGeneration(),
                request.getGraduationDate(),
                request.getCertificateUrl()
        );

        return UserResponse.from(user);
    }

    // 관리자용: 인증 대기 목록 조회
    public List<UserResponse> getPendingCertifications() {
        return userRepository.findByCertificationStatus(CertificationStatus.PENDING)
                .stream()
                .map(UserResponse::from)
                .collect(Collectors.toList());
    }

    // 관리자용: 인증 승인
    @Transactional
    public UserResponse approveCertification(Long userId) {
        User user = findUserById(userId);

        if (user.getCertificationStatus() != CertificationStatus.PENDING) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "인증 대기 상태가 아닙니다.");
        }

        user.approveCertification();

        // 알림 발송
        notificationService.notifyCertificationApproved(userId);

        return UserResponse.from(user);
    }

    // 관리자용: 인증 거절
    @Transactional
    public UserResponse rejectCertification(Long userId) {
        User user = findUserById(userId);

        if (user.getCertificationStatus() != CertificationStatus.PENDING) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "인증 대기 상태가 아닙니다.");
        }

        user.rejectCertification();

        // 알림 발송
        notificationService.notifyCertificationRejected(userId, null);

        return UserResponse.from(user);
    }

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    // 관리자용: 회원 목록 조회 (검색, 필터링, 페이징)
    public Page<UserResponse> getUsers(String keyword, Role role, CertificationStatus certificationStatus, Pageable pageable) {
        return userRepository.findAllWithFilters(keyword, role, certificationStatus, pageable)
                .map(UserResponse::from);
    }

    // 관리자용: 회원 상세 조회
    public UserResponse getUser(Long userId) {
        User user = findUserById(userId);
        return UserResponse.from(user);
    }

    // 관리자용: 회원 역할 변경
    @Transactional
    public UserResponse updateUserRole(Long currentUserId, Long targetUserId, Role newRole) {
        // 자기 자신 강등 방지
        if (currentUserId.equals(targetUserId) && newRole == Role.USER) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "자기 자신의 관리자 권한을 해제할 수 없습니다.");
        }

        User targetUser = findUserById(targetUserId);

        // 마지막 관리자 강등 방지
        if (targetUser.getRole() == Role.ADMIN && newRole == Role.USER) {
            long adminCount = userRepository.countByRole(Role.ADMIN);
            if (adminCount <= 1) {
                throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "최소 1명의 관리자가 필요합니다. 마지막 관리자의 권한을 해제할 수 없습니다.");
            }
        }

        targetUser.updateRole(newRole);
        return UserResponse.from(targetUser);
    }

    // 관리자용: 회원 삭제
    @Transactional
    public void deleteUser(Long currentUserId, Long targetUserId) {
        // 자기 자신 삭제 방지
        if (currentUserId.equals(targetUserId)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "자기 자신을 삭제할 수 없습니다.");
        }

        User targetUser = findUserById(targetUserId);

        // 마지막 관리자 삭제 방지
        if (targetUser.getRole() == Role.ADMIN) {
            long adminCount = userRepository.countByRole(Role.ADMIN);
            if (adminCount <= 1) {
                throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "최소 1명의 관리자가 필요합니다. 마지막 관리자를 삭제할 수 없습니다.");
            }
        }

        try {
            userRepository.delete(targetUser);
            userRepository.flush(); // 즉시 삭제 실행하여 FK 오류 감지
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE,
                "회원을 삭제할 수 없습니다. 프로필이나 팀 정보 등 연관된 데이터가 있습니다.");
        }
    }
}
