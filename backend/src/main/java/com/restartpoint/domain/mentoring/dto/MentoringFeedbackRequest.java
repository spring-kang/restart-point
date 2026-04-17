package com.restartpoint.domain.mentoring.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class MentoringFeedbackRequest {

    private String mentorFeedback;

    @Min(value = 1, message = "점수는 1 이상이어야 합니다")
    @Max(value = 5, message = "점수는 5 이하여야 합니다")
    private Integer feedbackScore;

    private String nextSteps;
}
