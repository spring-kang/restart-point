package com.restartpoint.domain.user.service;

import com.restartpoint.domain.user.dto.CertificationRequest;
import com.restartpoint.domain.user.dto.UserResponse;
import com.restartpoint.domain.user.entity.CertificationStatus;
import com.restartpoint.domain.user.entity.User;
import com.restartpoint.domain.user.repository.UserRepository;
import com.restartpoint.global.exception.BusinessException;
import com.restartpoint.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;

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
        return UserResponse.from(user);
    }

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }
}
