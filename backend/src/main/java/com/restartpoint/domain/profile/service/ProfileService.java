package com.restartpoint.domain.profile.service;

import com.restartpoint.domain.profile.dto.ProfileRequest;
import com.restartpoint.domain.profile.dto.ProfileResponse;
import com.restartpoint.domain.profile.entity.Profile;
import com.restartpoint.domain.profile.repository.ProfileRepository;
import com.restartpoint.domain.user.entity.User;
import com.restartpoint.domain.user.repository.UserRepository;
import com.restartpoint.global.exception.BusinessException;
import com.restartpoint.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProfileService {

    private final ProfileRepository profileRepository;
    private final UserRepository userRepository;

    public Optional<ProfileResponse> getMyProfile(Long userId) {
        return profileRepository.findByUserId(userId)
                .map(ProfileResponse::from);
    }

    public ProfileResponse getProfile(Long profileId) {
        Profile profile = profileRepository.findById(profileId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROFILE_NOT_FOUND));
        return ProfileResponse.from(profile);
    }

    @Transactional
    public ProfileResponse createOrUpdateProfile(Long userId, ProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Optional<Profile> existingProfile = profileRepository.findByUser(user);

        if (existingProfile.isPresent()) {
            // 기존 프로필 수정
            Profile profile = existingProfile.get();
            profile.update(
                    request.getJobRole(),
                    request.getTechStacks(),
                    request.getPortfolioUrl(),
                    request.getInterestedDomains(),
                    request.getAvailableHoursPerWeek(),
                    request.getCollaborationStyle(),
                    request.getImprovementGoal(),
                    request.getPreferredDifficulty(),
                    request.getIntroduction()
            );
            return ProfileResponse.from(profile);
        } else {
            // 새 프로필 생성
            Profile profile = Profile.builder()
                    .user(user)
                    .jobRole(request.getJobRole())
                    .techStacks(request.getTechStacks())
                    .portfolioUrl(request.getPortfolioUrl())
                    .interestedDomains(request.getInterestedDomains())
                    .availableHoursPerWeek(request.getAvailableHoursPerWeek())
                    .collaborationStyle(request.getCollaborationStyle())
                    .improvementGoal(request.getImprovementGoal())
                    .preferredDifficulty(request.getPreferredDifficulty())
                    .introduction(request.getIntroduction())
                    .build();

            Profile savedProfile = profileRepository.save(profile);
            return ProfileResponse.from(savedProfile);
        }
    }
}
