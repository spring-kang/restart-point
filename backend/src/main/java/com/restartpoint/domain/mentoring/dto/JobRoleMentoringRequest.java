package com.restartpoint.domain.mentoring.dto;

import com.restartpoint.domain.profile.entity.JobRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class JobRoleMentoringRequest {

    @NotNull(message = "직무는 필수입니다")
    private JobRole jobRole;

    @NotBlank(message = "멘토링 제목은 필수입니다")
    private String title;

    private String description;

    private String learningObjectives;
}
