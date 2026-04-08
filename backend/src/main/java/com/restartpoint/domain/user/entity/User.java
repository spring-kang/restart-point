package com.restartpoint.domain.user.entity;

import com.restartpoint.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CertificationStatus certificationStatus;

    // 이메일 인증 여부
    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean emailVerified = false;

    // 수료 인증 관련 정보
    @Column(name = "bootcamp_name")
    private String bootcampName;

    @Column(name = "bootcamp_generation")
    private String bootcampGeneration;

    @Column(name = "graduation_date")
    private String graduationDate;

    @Column(name = "certificate_url")
    private String certificateUrl;

    @Builder
    public User(String email, String password, String name, Role role) {
        this.email = email;
        this.password = password;
        this.name = name;
        this.role = role;
        this.certificationStatus = CertificationStatus.NONE;
    }

    public void requestCertification(String bootcampName, String bootcampGeneration,
                                     String graduationDate, String certificateUrl) {
        this.bootcampName = bootcampName;
        this.bootcampGeneration = bootcampGeneration;
        this.graduationDate = graduationDate;
        this.certificateUrl = certificateUrl;
        this.certificationStatus = CertificationStatus.PENDING;
    }

    public void approveCertification() {
        this.certificationStatus = CertificationStatus.APPROVED;
    }

    public void rejectCertification() {
        this.certificationStatus = CertificationStatus.REJECTED;
    }

    public boolean isCertified() {
        return this.certificationStatus == CertificationStatus.APPROVED;
    }

    public void verifyEmail() {
        this.emailVerified = true;
    }

    public boolean isEmailVerified() {
        return this.emailVerified;
    }
}
