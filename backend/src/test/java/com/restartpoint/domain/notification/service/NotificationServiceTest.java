package com.restartpoint.domain.notification.service;

import com.restartpoint.domain.notification.dto.NotificationResponse;
import com.restartpoint.domain.notification.entity.Notification;
import com.restartpoint.domain.notification.entity.NotificationType;
import com.restartpoint.domain.notification.repository.NotificationRepository;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

  @Mock
  private NotificationRepository notificationRepository;

  @Mock
  private UserRepository userRepository;

  @InjectMocks
  private NotificationService notificationService;

  @Nested
  @DisplayName("알림 생성")
  class CreateNotification {

    @Test
    @DisplayName("알림 생성에 성공한다")
    void createNotificationSuccess() {
      // given
      User user = createUser(1L, "user@example.com", "사용자");
      given(userRepository.findById(1L)).willReturn(Optional.of(user));
      given(notificationRepository.save(any(Notification.class))).willAnswer(invocation -> {
        Notification notification = invocation.getArgument(0);
        setField(notification, "id", 1L);
        return notification;
      });

      // when
      Notification result = notificationService.createNotification(
          1L,
          NotificationType.CERTIFICATION_APPROVED,
          "제목",
          "메시지",
          "USER",
          1L
      );

      // then
      assertThat(result).isNotNull();
      verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    @DisplayName("존재하지 않는 사용자에게 알림 생성 시 예외가 발생한다")
    void createNotificationFailsWhenUserNotFound() {
      // given
      given(userRepository.findById(999L)).willReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> notificationService.createNotification(
          999L,
          NotificationType.CERTIFICATION_APPROVED,
          "제목",
          "메시지"
      ))
          .isInstanceOf(BusinessException.class)
          .satisfies(exception -> {
            BusinessException businessException = (BusinessException) exception;
            assertThat(businessException.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND);
          });

      verify(notificationRepository, never()).save(any(Notification.class));
    }
  }

  @Nested
  @DisplayName("알림 조회")
  class GetNotifications {

    @Test
    @DisplayName("사용자의 알림 목록을 조회할 수 있다")
    void getNotificationsSuccess() {
      // given
      User user = createUser(1L, "user@example.com", "사용자");
      Notification notification = createNotification(1L, user, NotificationType.CERTIFICATION_APPROVED, "제목");
      Page<Notification> notificationsPage = new PageImpl<>(List.of(notification));
      Pageable pageable = PageRequest.of(0, 10);

      given(notificationRepository.findByUserIdOrderByCreatedAtDesc(1L, pageable))
          .willReturn(notificationsPage);

      // when
      Page<NotificationResponse> result = notificationService.getNotifications(1L, pageable);

      // then
      assertThat(result.getContent()).hasSize(1);
      assertThat(result.getContent().get(0).getTitle()).isEqualTo("제목");
    }

    @Test
    @DisplayName("읽지 않은 알림 개수를 조회할 수 있다")
    void getUnreadCountSuccess() {
      // given
      given(notificationRepository.countByUserIdAndReadFalse(1L)).willReturn(5L);

      // when
      long count = notificationService.getUnreadCount(1L);

      // then
      assertThat(count).isEqualTo(5L);
    }
  }

  @Nested
  @DisplayName("알림 읽음 처리")
  class MarkAsRead {

    @Test
    @DisplayName("알림을 읽음 처리할 수 있다")
    void markAsReadSuccess() {
      // given
      User user = createUser(1L, "user@example.com", "사용자");
      Notification notification = createNotification(1L, user, NotificationType.CERTIFICATION_APPROVED, "제목");

      given(notificationRepository.findById(1L)).willReturn(Optional.of(notification));

      // when
      notificationService.markAsRead(1L, 1L);

      // then
      assertThat(notification.isRead()).isTrue();
    }

    @Test
    @DisplayName("다른 사용자의 알림을 읽음 처리하려고 하면 예외가 발생한다")
    void markAsReadFailsWhenNotOwner() {
      // given
      User user = createUser(1L, "user@example.com", "사용자");
      Notification notification = createNotification(1L, user, NotificationType.CERTIFICATION_APPROVED, "제목");

      given(notificationRepository.findById(1L)).willReturn(Optional.of(notification));

      // when & then
      assertThatThrownBy(() -> notificationService.markAsRead(1L, 2L))
          .isInstanceOf(BusinessException.class)
          .satisfies(exception -> {
            BusinessException businessException = (BusinessException) exception;
            assertThat(businessException.getErrorCode()).isEqualTo(ErrorCode.ACCESS_DENIED);
          });
    }

    @Test
    @DisplayName("존재하지 않는 알림을 읽음 처리하려고 하면 예외가 발생한다")
    void markAsReadFailsWhenNotFound() {
      // given
      given(notificationRepository.findById(999L)).willReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> notificationService.markAsRead(999L, 1L))
          .isInstanceOf(BusinessException.class)
          .satisfies(exception -> {
            BusinessException businessException = (BusinessException) exception;
            assertThat(businessException.getErrorCode()).isEqualTo(ErrorCode.NOTIFICATION_NOT_FOUND);
          });
    }

    @Test
    @DisplayName("모든 알림을 읽음 처리할 수 있다")
    void markAllAsReadSuccess() {
      // given
      given(notificationRepository.markAllAsReadByUserId(1L)).willReturn(5);

      // when
      int count = notificationService.markAllAsRead(1L);

      // then
      assertThat(count).isEqualTo(5);
      verify(notificationRepository).markAllAsReadByUserId(1L);
    }
  }

  @Nested
  @DisplayName("알림 삭제")
  class DeleteNotification {

    @Test
    @DisplayName("알림을 삭제할 수 있다")
    void deleteNotificationSuccess() {
      // given
      User user = createUser(1L, "user@example.com", "사용자");
      Notification notification = createNotification(1L, user, NotificationType.CERTIFICATION_APPROVED, "제목");

      given(notificationRepository.findById(1L)).willReturn(Optional.of(notification));

      // when
      notificationService.deleteNotification(1L, 1L);

      // then
      verify(notificationRepository).delete(notification);
    }

    @Test
    @DisplayName("다른 사용자의 알림을 삭제하려고 하면 예외가 발생한다")
    void deleteNotificationFailsWhenNotOwner() {
      // given
      User user = createUser(1L, "user@example.com", "사용자");
      Notification notification = createNotification(1L, user, NotificationType.CERTIFICATION_APPROVED, "제목");

      given(notificationRepository.findById(1L)).willReturn(Optional.of(notification));

      // when & then
      assertThatThrownBy(() -> notificationService.deleteNotification(1L, 2L))
          .isInstanceOf(BusinessException.class)
          .satisfies(exception -> {
            BusinessException businessException = (BusinessException) exception;
            assertThat(businessException.getErrorCode()).isEqualTo(ErrorCode.ACCESS_DENIED);
          });

      verify(notificationRepository, never()).delete(any(Notification.class));
    }
  }

  @Nested
  @DisplayName("알림 헬퍼 메서드")
  class NotificationHelpers {

    @Test
    @DisplayName("수료 인증 승인 알림을 생성할 수 있다")
    void notifyCertificationApprovedSuccess() {
      // given
      User user = createUser(1L, "user@example.com", "사용자");
      given(userRepository.findById(1L)).willReturn(Optional.of(user));
      given(notificationRepository.save(any(Notification.class))).willAnswer(invocation -> {
        Notification notification = invocation.getArgument(0);
        setField(notification, "id", 1L);
        return notification;
      });

      // when
      notificationService.notifyCertificationApproved(1L);

      // then
      verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    @DisplayName("팀 지원 알림을 생성할 수 있다")
    void notifyTeamApplicationSuccess() {
      // given
      User leader = createUser(1L, "leader@example.com", "팀장");
      given(userRepository.findById(1L)).willReturn(Optional.of(leader));
      given(notificationRepository.save(any(Notification.class))).willAnswer(invocation -> {
        Notification notification = invocation.getArgument(0);
        setField(notification, "id", 1L);
        return notification;
      });

      // when
      notificationService.notifyTeamApplication(1L, "지원자", "테스트팀", 10L);

      // then
      verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    @DisplayName("팀 가입 승인 알림을 생성할 수 있다")
    void notifyTeamInvitationSuccess() {
      // given
      User user = createUser(1L, "user@example.com", "사용자");
      given(userRepository.findById(1L)).willReturn(Optional.of(user));
      given(notificationRepository.save(any(Notification.class))).willAnswer(invocation -> {
        Notification notification = invocation.getArgument(0);
        setField(notification, "id", 1L);
        return notification;
      });

      // when
      notificationService.notifyTeamInvitation(1L, "테스트팀", 10L);

      // then
      verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    @DisplayName("체크포인트 마감 알림을 생성할 수 있다")
    void notifyCheckpointReminderSuccess() {
      // given
      User user = createUser(1L, "user@example.com", "사용자");
      given(userRepository.findById(1L)).willReturn(Optional.of(user));
      given(notificationRepository.save(any(Notification.class))).willAnswer(invocation -> {
        Notification notification = invocation.getArgument(0);
        setField(notification, "id", 1L);
        return notification;
      });

      // when
      notificationService.notifyCheckpointReminder(1L, "프로젝트", 1, 10L);

      // then
      verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    @DisplayName("성장 리포트 발행 알림을 생성할 수 있다")
    void notifyReportPublishedSuccess() {
      // given
      User user = createUser(1L, "user@example.com", "사용자");
      given(userRepository.findById(1L)).willReturn(Optional.of(user));
      given(notificationRepository.save(any(Notification.class))).willAnswer(invocation -> {
        Notification notification = invocation.getArgument(0);
        setField(notification, "id", 1L);
        return notification;
      });

      // when
      notificationService.notifyReportPublished(1L, "프로젝트", 10L);

      // then
      verify(notificationRepository).save(any(Notification.class));
    }
  }

  // 헬퍼 메서드
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

  private Notification createNotification(Long id, User user, NotificationType type, String title) {
    Notification notification = Notification.builder()
        .user(user)
        .notificationType(type)
        .title(title)
        .message("메시지")
        .build();
    setField(notification, "id", id);
    return notification;
  }

  private void setField(Object target, String fieldName, Object value) {
    try {
      Field field = findField(target.getClass(), fieldName);
      field.setAccessible(true);
      field.set(target, value);
    } catch (ReflectiveOperationException exception) {
      throw new IllegalStateException("테스트 필드 설정에 실패했습니다: " + fieldName, exception);
    }
  }

  private Field findField(Class<?> clazz, String fieldName) throws NoSuchFieldException {
    Class<?> current = clazz;
    while (current != null) {
      try {
        return current.getDeclaredField(fieldName);
      } catch (NoSuchFieldException e) {
        current = current.getSuperclass();
      }
    }
    throw new NoSuchFieldException(fieldName);
  }
}
