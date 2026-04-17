package com.restartpoint.domain.guide.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class GuidelineCompletionRequest {

    private String completionNotes;

    // 완료한 체크리스트 인덱스 목록
    private List<Integer> completedChecklistIndexes;
}
