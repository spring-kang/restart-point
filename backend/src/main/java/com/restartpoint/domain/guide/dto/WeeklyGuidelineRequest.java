package com.restartpoint.domain.guide.dto;

import com.restartpoint.domain.profile.entity.JobRole;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class WeeklyGuidelineRequest {

    @NotNull(message = "주차 번호는 필수입니다")
    @Min(value = 1, message = "주차 번호는 1 이상이어야 합니다")
    private Integer weekNumber;

    @NotBlank(message = "주차 제목은 필수입니다")
    private String title;

    private String description;

    private String keyObjectives;

    // JSON 문자열로 변환될 리스트
    private List<String> milestones;

    private List<String> recommendedActions;

    private String guideContent;

    private JobRole focusRole;

    private List<String> checklistItems;

    private List<String> referenceLinks;
}
