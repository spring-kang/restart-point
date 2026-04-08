package com.restartpoint.domain.matching.dto;

import com.restartpoint.domain.profile.dto.ProfileResponse;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class MemberRecommendationResponse {

    private ProfileResponse profile;
    private int matchScore;          // 추천 점수 (0-100)
    private List<String> reasons;    // 추천 이유 (최소 3개)
    private String balanceAnalysis;  // 팀 밸런스 분석
    private String scheduleRisk;     // 일정 충돌 위험도 (LOW, MEDIUM, HIGH)
    private List<String> complementarySkills; // 보완 가능한 스킬
}
