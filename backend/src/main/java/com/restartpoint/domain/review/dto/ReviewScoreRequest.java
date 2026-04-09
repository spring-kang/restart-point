package com.restartpoint.domain.review.dto;

import com.restartpoint.domain.review.entity.RubricItem;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ReviewScoreRequest {

    @NotNull(message = "루브릭 항목은 필수입니다.")
    private RubricItem rubricItem;

    @NotNull(message = "점수는 필수입니다.")
    @Min(value = 1, message = "점수는 1 이상이어야 합니다.")
    @Max(value = 5, message = "점수는 5 이하여야 합니다.")
    private Integer score;

    private String comment;
}
