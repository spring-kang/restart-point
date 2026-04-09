package com.restartpoint.domain.review.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 심사 루브릭 항목
 */
@Getter
@RequiredArgsConstructor
public enum RubricItem {
    PROBLEM_DEFINITION("문제 정의의 명확성", "문제가 명확하게 정의되어 있고, 해결해야 할 핵심 이슈가 잘 드러나는가?"),
    USER_VALUE("사용자 가치", "타깃 사용자에게 실질적인 가치를 제공하는가?"),
    AI_USAGE("AI 활용 적절성", "AI를 적절하고 효과적으로 활용하고 있는가?"),
    UX_COMPLETENESS("UX 완성도", "사용자 경험이 직관적이고 완성도가 높은가?"),
    TECHNICAL_FEASIBILITY("기술 구현 가능성", "기술적으로 구현 가능하고 확장 가능한 구조인가?"),
    COLLABORATION("협업 완성도", "팀원 간 협업이 잘 이루어졌고, 역할 분담이 적절한가?");

    private final String label;
    private final String description;
}
