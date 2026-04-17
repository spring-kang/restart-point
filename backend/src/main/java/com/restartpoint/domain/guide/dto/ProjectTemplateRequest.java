package com.restartpoint.domain.guide.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ProjectTemplateRequest {

    @NotBlank(message = "템플릿 제목은 필수입니다")
    private String title;

    private String description;

    @NotNull(message = "총 주차 수는 필수입니다")
    @Min(value = 1, message = "최소 1주 이상이어야 합니다")
    private Integer totalWeeks;
}
