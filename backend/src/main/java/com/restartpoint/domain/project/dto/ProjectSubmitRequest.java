package com.restartpoint.domain.project.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ProjectSubmitRequest {

    @NotBlank(message = "팀 회고는 필수입니다.")
    private String teamRetrospective;
}
