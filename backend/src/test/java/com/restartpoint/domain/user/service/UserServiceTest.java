package com.restartpoint.domain.user.service;

import com.restartpoint.domain.user.dto.CertificationRequest;
import com.restartpoint.domain.user.dto.UserResponse;
import com.restartpoint.domain.user.entity.Role;
import com.restartpoint.domain.user.entity.User;
import com.restartpoint.domain.user.repository.UserRepository;
import com.restartpoint.global.exception.BusinessException;
import com.restartpoint.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @Test
    @DisplayName("수료 인증 요청 시 사용자 상태를 PENDING으로 변경하고 입력값을 저장한다")
    void requestCertificationUpdatesUser() {
        User user = createUser(1L, "test@example.com", "테스터");
        CertificationRequest request = createCertificationRequest(
                "코드잇 스프린트",
                "1기",
                "2026-04-01",
                "https://example.com/certificate"
        );
        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        UserResponse response = userService.requestCertification(1L, request);

        assertThat(response.getCertificationStatus().name()).isEqualTo("PENDING");
        assertThat(response.getBootcampName()).isEqualTo("코드잇 스프린트");
        assertThat(response.getBootcampGeneration()).isEqualTo("1기");
        assertThat(response.getGraduationDate()).isEqualTo("2026-04-01");
    }

    @Test
    @DisplayName("이미 승인된 사용자는 수료 인증을 다시 요청할 수 없다")
    void requestCertificationFailsWhenAlreadyApproved() {
        User user = createUser(1L, "approved@example.com", "승인유저");
        user.requestCertification("부트캠프", "2기", "2026-03-01", "https://example.com/cert");
        user.approveCertification();
        CertificationRequest request = createCertificationRequest(
                "다른 부트캠프",
                "3기",
                "2026-04-01",
                "https://example.com/other-cert"
        );
        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.requestCertification(1L, request))
                .isInstanceOf(BusinessException.class)
                .satisfies(exception -> {
                    BusinessException businessException = (BusinessException) exception;
                    assertThat(businessException.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
                    assertThat(businessException.getMessage()).isEqualTo("이미 수료 인증이 완료되었습니다.");
                });
    }

    @Test
    @DisplayName("이미 인증 대기 중인 사용자는 수료 인증을 다시 요청할 수 없다")
    void requestCertificationFailsWhenAlreadyPending() {
        User user = createUser(1L, "pending@example.com", "대기유저");
        user.requestCertification("부트캠프", "2기", "2026-03-01", "https://example.com/cert");
        CertificationRequest request = createCertificationRequest(
                "다른 부트캠프",
                "3기",
                "2026-04-01",
                "https://example.com/other-cert"
        );
        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.requestCertification(1L, request))
                .isInstanceOf(BusinessException.class)
                .satisfies(exception -> {
                    BusinessException businessException = (BusinessException) exception;
                    assertThat(businessException.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
                    assertThat(businessException.getMessage())
                            .isEqualTo("이미 수료 인증 요청이 진행 중입니다. 운영자 검토를 기다려주세요.");
                });
    }

    @Test
    @DisplayName("관리자는 대기 중인 수료 인증을 승인할 수 있다")
    void approveCertificationSucceedsForPendingUser() {
        User user = createUser(1L, "pending@example.com", "대기유저");
        user.requestCertification("부트캠프", "2기", "2026-03-01", "https://example.com/cert");
        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        UserResponse response = userService.approveCertification(1L);

        assertThat(response.getCertificationStatus().name()).isEqualTo("APPROVED");
    }

    @Test
    @DisplayName("대기 상태가 아닌 수료 인증은 승인할 수 없다")
    void approveCertificationFailsWhenNotPending() {
        User user = createUser(1L, "user@example.com", "일반유저");
        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.approveCertification(1L))
                .isInstanceOf(BusinessException.class)
                .satisfies(exception -> {
                    BusinessException businessException = (BusinessException) exception;
                    assertThat(businessException.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
                    assertThat(businessException.getMessage()).isEqualTo("인증 대기 상태가 아닙니다.");
                });
    }

    @Test
    @DisplayName("관리자는 대기 중인 수료 인증을 거절할 수 있다")
    void rejectCertificationSucceedsForPendingUser() {
        User user = createUser(1L, "pending@example.com", "대기유저");
        user.requestCertification("부트캠프", "2기", "2026-03-01", "https://example.com/cert");
        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        UserResponse response = userService.rejectCertification(1L);

        assertThat(response.getCertificationStatus().name()).isEqualTo("REJECTED");
    }

    @Test
    @DisplayName("인증 대기 목록 조회 시 대기 중인 사용자만 반환한다")
    void getPendingCertificationsReturnsPendingUsers() {
        User pendingUser = createUser(1L, "pending@example.com", "대기유저");
        pendingUser.requestCertification("부트캠프", "2기", "2026-03-01", "https://example.com/cert");
        given(userRepository.findByCertificationStatus(com.restartpoint.domain.user.entity.CertificationStatus.PENDING))
                .willReturn(List.of(pendingUser));

        List<UserResponse> responses = userService.getPendingCertifications();

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getId()).isEqualTo(1L);
        assertThat(responses.get(0).getCertificationStatus().name()).isEqualTo("PENDING");
    }

    private User createUser(Long id, String email, String name) {
        User user = User.builder()
                .email(email)
                .password("encoded-password")
                .name(name)
                .role(Role.USER)
                .build();
        setField(user, "id", id);
        return user;
    }

    private CertificationRequest createCertificationRequest(
            String bootcampName,
            String bootcampGeneration,
            String graduationDate,
            String certificateUrl
    ) {
        CertificationRequest request = new CertificationRequest();
        setField(request, "bootcampName", bootcampName);
        setField(request, "bootcampGeneration", bootcampGeneration);
        setField(request, "graduationDate", graduationDate);
        setField(request, "certificateUrl", certificateUrl);
        return request;
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("테스트 필드 설정에 실패했습니다: " + fieldName, exception);
        }
    }
}
