package com.restartpoint.domain.mentoring.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class MentoringSessionRequest {

    // 세션 노트 업데이트용
    private String sessionNotes;
    private String questions;
    private List<Integer> completedTaskIndexes;
}
