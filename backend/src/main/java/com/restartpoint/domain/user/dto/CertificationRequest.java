package com.restartpoint.domain.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CertificationRequest {

    @NotBlank(message = "부트캠프명은 필수입니다.")
    private String bootcampName;

    @NotBlank(message = "기수는 필수입니다.")
    private String bootcampGeneration;

    @NotBlank(message = "수료일은 필수입니다.")
    private String graduationDate;

    @NotBlank(message = "수료증 URL은 필수입니다.")
    private String certificateUrl;
}
