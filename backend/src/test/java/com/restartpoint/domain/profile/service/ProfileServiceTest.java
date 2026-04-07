package com.restartpoint.domain.profile.service;

import com.restartpoint.domain.profile.dto.ProfileRequest;
import com.restartpoint.domain.profile.dto.ProfileResponse;
import com.restartpoint.domain.profile.entity.CollaborationStyle;
import com.restartpoint.domain.profile.entity.JobRole;
import com.restartpoint.domain.profile.entity.Profile;
import com.restartpoint.domain.profile.entity.ProjectDifficulty;
import com.restartpoint.domain.profile.repository.ProfileRepository;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

    @Mock
    private ProfileRepository profileRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ProfileService profileService;

    @Test
    @DisplayName("내 프로필 조회 시 프로필이 있으면 응답으로 반환한다")
    void getMyProfileReturnsProfile() {
        User user = createUser(1L, "tester@example.com", "테스터");
        Profile profile = createProfile(10L, user, JobRole.BACKEND, List.of("Java", "Spring Boot"));
        given(profileRepository.findByUserId(1L)).willReturn(Optional.of(profile));

        Optional<ProfileResponse> response = profileService.getMyProfile(1L);

        assertThat(response).isPresent();
        assertThat(response.get().getId()).isEqualTo(10L);
        assertThat(response.get().getJobRole()).isEqualTo(JobRole.BACKEND);
    }

    @Test
    @DisplayName("프로필 상세 조회 시 존재하지 않으면 예외가 발생한다")
    void getProfileFailsWhenProfileNotFound() {
        given(profileRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> profileService.getProfile(99L))
                .isInstanceOf(BusinessException.class)
                .satisfies(exception -> {
                    BusinessException businessException = (BusinessException) exception;
                    assertThat(businessException.getErrorCode()).isEqualTo(ErrorCode.PROFILE_NOT_FOUND);
                });
    }

    @Test
    @DisplayName("기존 프로필이 없으면 새 프로필을 생성한다")
    void createOrUpdateProfileCreatesNewProfile() {
        User user = createUser(1L, "tester@example.com", "테스터");
        ProfileRequest request = createProfileRequest(
                JobRole.BACKEND,
                List.of("Java", "Spring Boot"),
                List.of("AI/ML"),
                20
        );
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(profileRepository.findByUser(user)).willReturn(Optional.empty());
        given(profileRepository.save(any(Profile.class))).willAnswer(invocation -> {
            Profile savedProfile = invocation.getArgument(0);
            setField(savedProfile, "id", 100L);
            return savedProfile;
        });

        ProfileResponse response = profileService.createOrUpdateProfile(1L, request);

        assertThat(response.getId()).isEqualTo(100L);
        assertThat(response.getUserId()).isEqualTo(1L);
        assertThat(response.getJobRole()).isEqualTo(JobRole.BACKEND);
        assertThat(response.getTechStacks()).containsExactly("Java", "Spring Boot");
        assertThat(response.getInterestedDomains()).containsExactly("AI/ML");
        assertThat(response.getAvailableHoursPerWeek()).isEqualTo(20);
    }

    @Test
    @DisplayName("기존 프로필이 있으면 새로 생성하지 않고 내용을 수정한다")
    void createOrUpdateProfileUpdatesExistingProfile() {
        User user = createUser(1L, "tester@example.com", "테스터");
        Profile existingProfile = createProfile(10L, user, JobRole.FRONTEND, List.of("React"));
        ProfileRequest request = createProfileRequest(
                JobRole.BACKEND,
                List.of("Java", "Spring Boot"),
                List.of("B2B SaaS", "AI/ML"),
                25
        );
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(profileRepository.findByUser(user)).willReturn(Optional.of(existingProfile));

        ProfileResponse response = profileService.createOrUpdateProfile(1L, request);

        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getJobRole()).isEqualTo(JobRole.BACKEND);
        assertThat(response.getTechStacks()).containsExactly("Java", "Spring Boot");
        assertThat(response.getInterestedDomains()).containsExactly("B2B SaaS", "AI/ML");
        assertThat(response.getAvailableHoursPerWeek()).isEqualTo(25);
        assertThat(response.getCollaborationStyle()).isEqualTo(CollaborationStyle.COLLABORATIVE);
    }

    @Test
    @DisplayName("프로필 생성 또는 수정 시 사용자가 없으면 예외가 발생한다")
    void createOrUpdateProfileFailsWhenUserNotFound() {
        ProfileRequest request = createProfileRequest(
                JobRole.BACKEND,
                List.of("Java"),
                List.of("교육"),
                10
        );
        given(userRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> profileService.createOrUpdateProfile(1L, request))
                .isInstanceOf(BusinessException.class)
                .satisfies(exception -> {
                    BusinessException businessException = (BusinessException) exception;
                    assertThat(businessException.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND);
                });
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

    private Profile createProfile(Long id, User user, JobRole jobRole, List<String> techStacks) {
        Profile profile = Profile.builder()
                .user(user)
                .jobRole(jobRole)
                .techStacks(techStacks)
                .portfolioUrl("https://example.com/portfolio")
                .interestedDomains(List.of("교육"))
                .availableHoursPerWeek(15)
                .collaborationStyle(CollaborationStyle.LEADER)
                .improvementGoal("코드 리뷰 경험")
                .preferredDifficulty(ProjectDifficulty.INTERMEDIATE)
                .introduction("안녕하세요")
                .build();
        setField(profile, "id", id);
        return profile;
    }

    private ProfileRequest createProfileRequest(
            JobRole jobRole,
            List<String> techStacks,
            List<String> interestedDomains,
            Integer availableHoursPerWeek
    ) {
        ProfileRequest request = new ProfileRequest();
        setField(request, "jobRole", jobRole);
        setField(request, "techStacks", techStacks);
        setField(request, "portfolioUrl", "https://example.com/new-portfolio");
        setField(request, "interestedDomains", interestedDomains);
        setField(request, "availableHoursPerWeek", availableHoursPerWeek);
        setField(request, "collaborationStyle", CollaborationStyle.COLLABORATIVE);
        setField(request, "improvementGoal", "백엔드 아키텍처 설계");
        setField(request, "preferredDifficulty", ProjectDifficulty.ADVANCED);
        setField(request, "introduction", "백엔드 개발자입니다.");
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
