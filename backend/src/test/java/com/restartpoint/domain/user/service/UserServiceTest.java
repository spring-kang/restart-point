package com.restartpoint.domain.user.service;

import com.restartpoint.domain.notification.service.NotificationService;
import com.restartpoint.domain.user.dto.CertificationRequest;
import com.restartpoint.domain.user.dto.UserResponse;
import com.restartpoint.domain.user.entity.Role;
import com.restartpoint.domain.user.entity.User;
import com.restartpoint.domain.user.repository.UserRepository;
import com.restartpoint.global.exception.BusinessException;
import com.restartpoint.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationService notificationService;

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

    @Nested
    @DisplayName("회원 역할 변경")
    class UpdateUserRole {

        @Test
        @DisplayName("관리자가 다른 사용자의 역할을 USER에서 ADMIN으로 변경할 수 있다")
        void updateUserRoleToAdmin() {
            Long currentUserId = 1L;
            Long targetUserId = 2L;
            User targetUser = createUser(targetUserId, "user@example.com", "대상유저");
            given(userRepository.findById(targetUserId)).willReturn(Optional.of(targetUser));

            UserResponse response = userService.updateUserRole(currentUserId, targetUserId, Role.ADMIN);

            assertThat(response.getRole()).isEqualTo(Role.ADMIN);
        }

        @Test
        @DisplayName("관리자가 다른 관리자의 역할을 ADMIN에서 USER로 변경할 수 있다 (관리자가 2명 이상일 때)")
        void updateUserRoleToUser() {
            Long currentUserId = 1L;
            Long targetUserId = 2L;
            User targetUser = createAdminUser(targetUserId, "admin2@example.com", "관리자2");
            given(userRepository.findById(targetUserId)).willReturn(Optional.of(targetUser));
            given(userRepository.countByRole(Role.ADMIN)).willReturn(2L);

            UserResponse response = userService.updateUserRole(currentUserId, targetUserId, Role.USER);

            assertThat(response.getRole()).isEqualTo(Role.USER);
        }

        @Test
        @DisplayName("자기 자신의 관리자 권한을 해제하려고 하면 실패한다")
        void updateUserRoleFailsWhenSelfDemotion() {
            Long currentUserId = 1L;
            Long targetUserId = 1L; // 자기 자신

            assertThatThrownBy(() -> userService.updateUserRole(currentUserId, targetUserId, Role.USER))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(exception -> {
                        BusinessException businessException = (BusinessException) exception;
                        assertThat(businessException.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
                        assertThat(businessException.getMessage()).isEqualTo("자기 자신의 관리자 권한을 해제할 수 없습니다.");
                    });

            verify(userRepository, never()).findById(targetUserId);
        }

        @Test
        @DisplayName("마지막 관리자의 역할을 USER로 변경하려고 하면 실패한다")
        void updateUserRoleFailsWhenLastAdmin() {
            Long currentUserId = 1L;
            Long targetUserId = 2L;
            User targetUser = createAdminUser(targetUserId, "lastadmin@example.com", "마지막관리자");
            given(userRepository.findById(targetUserId)).willReturn(Optional.of(targetUser));
            given(userRepository.countByRole(Role.ADMIN)).willReturn(1L);

            assertThatThrownBy(() -> userService.updateUserRole(currentUserId, targetUserId, Role.USER))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(exception -> {
                        BusinessException businessException = (BusinessException) exception;
                        assertThat(businessException.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
                        assertThat(businessException.getMessage()).isEqualTo("최소 1명의 관리자가 필요합니다. 마지막 관리자의 권한을 해제할 수 없습니다.");
                    });
        }

        @Test
        @DisplayName("자기 자신에게 관리자 권한을 부여하는 것은 허용된다 (이미 ADMIN이면 변경 없음)")
        void updateUserRoleAllowsSelfPromotion() {
            Long currentUserId = 1L;
            Long targetUserId = 1L;
            User currentUser = createUser(targetUserId, "user@example.com", "유저");
            given(userRepository.findById(targetUserId)).willReturn(Optional.of(currentUser));

            UserResponse response = userService.updateUserRole(currentUserId, targetUserId, Role.ADMIN);

            assertThat(response.getRole()).isEqualTo(Role.ADMIN);
        }
    }

    @Nested
    @DisplayName("회원 삭제")
    class DeleteUser {

        @Test
        @DisplayName("관리자가 일반 사용자를 삭제할 수 있다")
        void deleteUserSuccess() {
            Long currentUserId = 1L;
            Long targetUserId = 2L;
            User targetUser = createUser(targetUserId, "user@example.com", "삭제대상");
            given(userRepository.findById(targetUserId)).willReturn(Optional.of(targetUser));

            userService.deleteUser(currentUserId, targetUserId);

            verify(userRepository).delete(targetUser);
        }

        @Test
        @DisplayName("자기 자신을 삭제하려고 하면 실패한다")
        void deleteUserFailsWhenSelfDelete() {
            Long currentUserId = 1L;
            Long targetUserId = 1L; // 자기 자신

            assertThatThrownBy(() -> userService.deleteUser(currentUserId, targetUserId))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(exception -> {
                        BusinessException businessException = (BusinessException) exception;
                        assertThat(businessException.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
                        assertThat(businessException.getMessage()).isEqualTo("자기 자신을 삭제할 수 없습니다.");
                    });

            verify(userRepository, never()).findById(targetUserId);
        }

        @Test
        @DisplayName("마지막 관리자를 삭제하려고 하면 실패한다")
        void deleteUserFailsWhenLastAdmin() {
            Long currentUserId = 1L;
            Long targetUserId = 2L;
            User targetUser = createAdminUser(targetUserId, "lastadmin@example.com", "마지막관리자");
            given(userRepository.findById(targetUserId)).willReturn(Optional.of(targetUser));
            given(userRepository.countByRole(Role.ADMIN)).willReturn(1L);

            assertThatThrownBy(() -> userService.deleteUser(currentUserId, targetUserId))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(exception -> {
                        BusinessException businessException = (BusinessException) exception;
                        assertThat(businessException.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
                        assertThat(businessException.getMessage()).isEqualTo("최소 1명의 관리자가 필요합니다. 마지막 관리자를 삭제할 수 없습니다.");
                    });

            verify(userRepository, never()).delete(targetUser);
        }

        @Test
        @DisplayName("관리자가 2명 이상일 때 다른 관리자를 삭제할 수 있다")
        void deleteAdminWhenMultipleAdmins() {
            Long currentUserId = 1L;
            Long targetUserId = 2L;
            User targetUser = createAdminUser(targetUserId, "admin2@example.com", "관리자2");
            given(userRepository.findById(targetUserId)).willReturn(Optional.of(targetUser));
            given(userRepository.countByRole(Role.ADMIN)).willReturn(2L);

            userService.deleteUser(currentUserId, targetUserId);

            verify(userRepository).delete(targetUser);
        }
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

    private User createAdminUser(Long id, String email, String name) {
        User user = User.builder()
                .email(email)
                .password("encoded-password")
                .name(name)
                .role(Role.ADMIN)
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
