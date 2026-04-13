package com.restartpoint.domain.user.dto;

import com.restartpoint.domain.user.entity.CertificationStatus;
import com.restartpoint.domain.user.entity.Role;
import com.restartpoint.domain.user.entity.User;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class UserResponse {

    private Long id;
    private String email;
    private String name;
    private Role role;
    private boolean emailVerified;
    private CertificationStatus certificationStatus;
    private String bootcampName;
    private String bootcampGeneration;
    private String graduationDate;
    private String certificateUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static UserResponse from(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .role(user.getRole())
                .emailVerified(user.isEmailVerified())
                .certificationStatus(user.getCertificationStatus())
                .bootcampName(user.getBootcampName())
                .bootcampGeneration(user.getBootcampGeneration())
                .graduationDate(user.getGraduationDate())
                .certificateUrl(user.getCertificateUrl())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
