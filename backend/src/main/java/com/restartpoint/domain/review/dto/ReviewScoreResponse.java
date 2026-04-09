package com.restartpoint.domain.review.dto;

import com.restartpoint.domain.review.entity.ReviewScore;
import com.restartpoint.domain.review.entity.RubricItem;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ReviewScoreResponse {

    private Long id;
    private RubricItem rubricItem;
    private String rubricLabel;
    private String rubricDescription;
    private int score;
    private String comment;

    public static ReviewScoreResponse from(ReviewScore score) {
        return ReviewScoreResponse.builder()
                .id(score.getId())
                .rubricItem(score.getRubricItem())
                .rubricLabel(score.getRubricItem().getLabel())
                .rubricDescription(score.getRubricItem().getDescription())
                .score(score.getScore())
                .comment(score.getComment())
                .build();
    }
}
