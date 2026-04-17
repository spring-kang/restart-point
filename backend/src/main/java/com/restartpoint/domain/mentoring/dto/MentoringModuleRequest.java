package com.restartpoint.domain.mentoring.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class MentoringModuleRequest {

    @NotNull(message = "주차 번호는 필수입니다")
    @Min(value = 1, message = "주차 번호는 1 이상이어야 합니다")
    private Integer weekNumber;

    @NotBlank(message = "모듈 제목은 필수입니다")
    private String title;

    private String description;

    private String learningContent;

    private List<String> keyPoints;

    private List<String> commonMistakes;

    private List<String> practiceTasks;

    private List<String> referenceMaterials;

    private Integer estimatedMinutes;
}
