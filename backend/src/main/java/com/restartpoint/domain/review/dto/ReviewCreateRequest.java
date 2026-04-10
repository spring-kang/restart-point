package com.restartpoint.domain.review.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class ReviewCreateRequest {

    @NotEmpty(message = "최소 하나 이상의 루브릭 점수가 필요합니다.")
    @Valid
    private List<ReviewScoreRequest> scores;

    @Size(max = 2000, message = "총평은 2000자 이하여야 합니다.")
    private String overallComment;
}
