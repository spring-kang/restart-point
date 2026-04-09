package com.restartpoint.domain.project.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ProjectUpdateRequest {

    @NotBlank(message = "프로젝트 이름은 필수입니다.")
    private String name;

    private String problemDefinition;
    private String targetUsers;
    private String solution;
    private String aiUsage;
    private String figmaUrl;
    private String githubUrl;
    private String notionUrl;
    private String demoUrl;
}
