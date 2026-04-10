package com.restartpoint.domain.review.service;

import com.restartpoint.domain.review.dto.ReviewGuideResponse;
import com.restartpoint.domain.review.dto.ReviewGuideResponse.*;
import com.restartpoint.domain.review.entity.ReviewGuideCompletion;
import com.restartpoint.domain.review.entity.RubricItem;
import com.restartpoint.domain.review.repository.ReviewGuideCompletionRepository;
import com.restartpoint.domain.user.entity.User;
import com.restartpoint.domain.user.repository.UserRepository;
import com.restartpoint.global.exception.BusinessException;
import com.restartpoint.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 심사 가이드 학습 서비스
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class ReviewGuideService {

    private final ReviewGuideCompletionRepository guideCompletionRepository;
    private final UserRepository userRepository;

    /**
     * 심사 가이드 콘텐츠 조회
     */
    public ReviewGuideResponse getGuide(Long userId) {
        ReviewGuideStatus status = getCompletionStatus(userId);

        return ReviewGuideResponse.builder()
                .rubricGuides(buildRubricGuides())
                .exampleComparisons(buildExampleComparisons())
                .completionStatus(status)
                .build();
    }

    /**
     * 가이드 완료 상태 조회
     */
    public ReviewGuideStatus getCompletionStatus(Long userId) {
        return guideCompletionRepository.findByUserId(userId)
                .map(completion -> ReviewGuideStatus.builder()
                        .rubricLearningCompleted(completion.isRubricLearningCompleted())
                        .exampleComparisonCompleted(completion.isExampleComparisonCompleted())
                        .practiceEvaluationCompleted(completion.isPracticeEvaluationCompleted())
                        .fullyCompleted(completion.isFullyCompleted())
                        .build())
                .orElse(ReviewGuideStatus.builder()
                        .rubricLearningCompleted(false)
                        .exampleComparisonCompleted(false)
                        .practiceEvaluationCompleted(false)
                        .fullyCompleted(false)
                        .build());
    }

    /**
     * 가이드 완료 여부 확인
     */
    public boolean isGuideCompleted(Long userId) {
        return guideCompletionRepository.existsByUserIdAndFullyCompletedTrue(userId);
    }

    /**
     * 루브릭 학습 완료 처리
     */
    @Transactional
    public ReviewGuideStatus completeRubricLearning(Long userId) {
        ReviewGuideCompletion completion = getOrCreateCompletion(userId);
        completion.completeRubricLearning();
        guideCompletionRepository.save(completion);
        log.info("루브릭 학습 완료 - 사용자: {}", userId);
        return getCompletionStatus(userId);
    }

    /**
     * 사례 비교 학습 완료 처리
     * - 선행 조건: 루브릭 학습 완료
     */
    @Transactional
    public ReviewGuideStatus completeExampleComparison(Long userId) {
        ReviewGuideCompletion completion = getOrCreateCompletion(userId);

        // 루브릭 학습 완료 여부 검증
        if (!completion.isRubricLearningCompleted()) {
            throw new BusinessException(ErrorCode.PREVIOUS_STEP_NOT_COMPLETED);
        }

        completion.completeExampleComparison();
        guideCompletionRepository.save(completion);
        log.info("사례 비교 학습 완료 - 사용자: {}", userId);
        return getCompletionStatus(userId);
    }

    /**
     * 연습 평가 완료 처리
     * - 선행 조건: 사례 비교 학습 완료
     */
    @Transactional
    public ReviewGuideStatus completePracticeEvaluation(Long userId) {
        ReviewGuideCompletion completion = getOrCreateCompletion(userId);

        // 사례 비교 학습 완료 여부 검증
        if (!completion.isExampleComparisonCompleted()) {
            throw new BusinessException(ErrorCode.PREVIOUS_STEP_NOT_COMPLETED);
        }

        completion.completePracticeEvaluation();
        guideCompletionRepository.save(completion);
        log.info("연습 평가 완료 - 사용자: {}", userId);
        return getCompletionStatus(userId);
    }

    // === Private 헬퍼 메서드 ===

    private ReviewGuideCompletion getOrCreateCompletion(Long userId) {
        return guideCompletionRepository.findByUserId(userId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
                    return ReviewGuideCompletion.builder()
                            .user(user)
                            .build();
                });
    }

    /**
     * 루브릭 가이드 콘텐츠 생성
     */
    private List<RubricGuide> buildRubricGuides() {
        List<RubricGuide> guides = new ArrayList<>();

        // 문제 정의의 명확성
        guides.add(RubricGuide.builder()
                .rubricItem(RubricItem.PROBLEM_DEFINITION)
                .label(RubricItem.PROBLEM_DEFINITION.getLabel())
                .description(RubricItem.PROBLEM_DEFINITION.getDescription())
                .evaluationTips("문제가 구체적으로 정의되어 있는지, 해결하려는 핵심 이슈가 명확한지 확인하세요.")
                .scoreExamples(List.of(
                        ScoreExample.builder()
                                .score(5)
                                .description("매우 우수")
                                .example("구체적인 사용자 페인포인트와 시장 조사를 바탕으로 문제를 명확히 정의함")
                                .build(),
                        ScoreExample.builder()
                                .score(3)
                                .description("보통")
                                .example("문제는 정의되어 있으나 구체성이 부족하거나 범위가 불명확함")
                                .build(),
                        ScoreExample.builder()
                                .score(1)
                                .description("미흡")
                                .example("문제 정의가 모호하거나 해결하려는 바가 불분명함")
                                .build()
                ))
                .build());

        // 사용자 가치
        guides.add(RubricGuide.builder()
                .rubricItem(RubricItem.USER_VALUE)
                .label(RubricItem.USER_VALUE.getLabel())
                .description(RubricItem.USER_VALUE.getDescription())
                .evaluationTips("타깃 사용자가 명확하고, 제안하는 솔루션이 실질적인 가치를 제공하는지 확인하세요.")
                .scoreExamples(List.of(
                        ScoreExample.builder()
                                .score(5)
                                .description("매우 우수")
                                .example("명확한 타깃 사용자 정의와 함께 실질적이고 측정 가능한 가치를 제공함")
                                .build(),
                        ScoreExample.builder()
                                .score(3)
                                .description("보통")
                                .example("타깃 사용자는 정의되어 있으나 제공 가치가 추상적임")
                                .build(),
                        ScoreExample.builder()
                                .score(1)
                                .description("미흡")
                                .example("타깃 사용자가 불분명하거나 가치 제안이 미약함")
                                .build()
                ))
                .build());

        // AI 활용 적절성
        guides.add(RubricGuide.builder()
                .rubricItem(RubricItem.AI_USAGE)
                .label(RubricItem.AI_USAGE.getLabel())
                .description(RubricItem.AI_USAGE.getDescription())
                .evaluationTips("AI가 단순 기능이 아닌 핵심 가치에 기여하는지, 적절하게 활용되는지 확인하세요.")
                .scoreExamples(List.of(
                        ScoreExample.builder()
                                .score(5)
                                .description("매우 우수")
                                .example("AI가 핵심 문제 해결에 필수적이며, 창의적이고 효과적으로 활용됨")
                                .build(),
                        ScoreExample.builder()
                                .score(3)
                                .description("보통")
                                .example("AI를 활용하고 있으나 필수적이지 않거나 대체 가능함")
                                .build(),
                        ScoreExample.builder()
                                .score(1)
                                .description("미흡")
                                .example("AI 활용이 형식적이거나 부적절함")
                                .build()
                ))
                .build());

        // UX 완성도
        guides.add(RubricGuide.builder()
                .rubricItem(RubricItem.UX_COMPLETENESS)
                .label(RubricItem.UX_COMPLETENESS.getLabel())
                .description(RubricItem.UX_COMPLETENESS.getDescription())
                .evaluationTips("사용자 흐름이 직관적이고, 디자인이 일관성 있으며, 완성도가 높은지 확인하세요.")
                .scoreExamples(List.of(
                        ScoreExample.builder()
                                .score(5)
                                .description("매우 우수")
                                .example("직관적인 UX, 일관된 디자인 시스템, 세부 인터랙션까지 완성됨")
                                .build(),
                        ScoreExample.builder()
                                .score(3)
                                .description("보통")
                                .example("기본적인 UX는 갖추었으나 일부 흐름이나 디자인에 개선 여지가 있음")
                                .build(),
                        ScoreExample.builder()
                                .score(1)
                                .description("미흡")
                                .example("UX가 혼란스럽거나 디자인 완성도가 낮음")
                                .build()
                ))
                .build());

        // 기술 구현 가능성
        guides.add(RubricGuide.builder()
                .rubricItem(RubricItem.TECHNICAL_FEASIBILITY)
                .label(RubricItem.TECHNICAL_FEASIBILITY.getLabel())
                .description(RubricItem.TECHNICAL_FEASIBILITY.getDescription())
                .evaluationTips("기술 스택이 적절하고, 실제 구현 가능하며, 확장 가능한 구조인지 확인하세요.")
                .scoreExamples(List.of(
                        ScoreExample.builder()
                                .score(5)
                                .description("매우 우수")
                                .example("적절한 기술 선택, 확장 가능한 아키텍처, 실제 동작하는 프로토타입")
                                .build(),
                        ScoreExample.builder()
                                .score(3)
                                .description("보통")
                                .example("기술적으로 구현 가능하나 일부 설계에 개선 여지가 있음")
                                .build(),
                        ScoreExample.builder()
                                .score(1)
                                .description("미흡")
                                .example("기술적 타당성이 낮거나 구현이 어려워 보임")
                                .build()
                ))
                .build());

        // 협업 완성도
        guides.add(RubricGuide.builder()
                .rubricItem(RubricItem.COLLABORATION)
                .label(RubricItem.COLLABORATION.getLabel())
                .description(RubricItem.COLLABORATION.getDescription())
                .evaluationTips("팀원 간 역할 분담이 적절하고, 협업 흔적이 보이며, 팀워크가 잘 이루어졌는지 확인하세요.")
                .scoreExamples(List.of(
                        ScoreExample.builder()
                                .score(5)
                                .description("매우 우수")
                                .example("명확한 역할 분담, 체계적인 협업 프로세스, 균형 잡힌 기여도")
                                .build(),
                        ScoreExample.builder()
                                .score(3)
                                .description("보통")
                                .example("역할 분담은 있으나 일부 불균형하거나 협업 흔적이 제한적임")
                                .build(),
                        ScoreExample.builder()
                                .score(1)
                                .description("미흡")
                                .example("역할 분담이 불명확하거나 협업이 부족해 보임")
                                .build()
                ))
                .build());

        return guides;
    }

    /**
     * 우수/보통 사례 비교 콘텐츠 생성
     */
    private List<ExampleComparison> buildExampleComparisons() {
        List<ExampleComparison> comparisons = new ArrayList<>();

        // 문제 정의 비교
        comparisons.add(ExampleComparison.builder()
                .category("문제 정의")
                .excellentExample(ExampleProject.builder()
                        .name("우수 사례: 스마트 냉장고 관리 앱")
                        .problemDefinition("1인 가구 중 73%가 냉장고 속 식재료를 잊어 버려 음식물 쓰레기를 발생시키며, 월 평균 5만원의 식비를 낭비하고 있음")
                        .solution("AI 기반 식재료 인식 및 유통기한 관리, 맞춤 레시피 추천")
                        .aiUsage("컴퓨터 비전으로 식재료 자동 인식, LLM으로 재료 기반 레시피 추천")
                        .expectedScore(5)
                        .reasonForScore("구체적인 통계와 함께 명확한 페인포인트 정의, 측정 가능한 문제 규모 제시")
                        .build())
                .averageExample(ExampleProject.builder()
                        .name("보통 사례: 식재료 관리 앱")
                        .problemDefinition("사람들이 냉장고 속 식재료를 잘 관리하지 못해 낭비가 발생함")
                        .solution("식재료 등록 및 유통기한 알림")
                        .aiUsage("AI로 레시피 추천")
                        .expectedScore(3)
                        .reasonForScore("문제는 정의되어 있으나 구체적인 수치나 타깃 사용자가 불분명함")
                        .build())
                .comparisonNotes("우수 사례는 구체적인 타깃(1인 가구)과 수치화된 문제(73%, 월 5만원)를 제시하여 문제의 심각성과 해결 가치를 명확히 전달합니다.")
                .build());

        // AI 활용 비교
        comparisons.add(ExampleComparison.builder()
                .category("AI 활용")
                .excellentExample(ExampleProject.builder()
                        .name("우수 사례: 개인화 학습 플랫폼")
                        .problemDefinition("학습자마다 이해 속도와 취약점이 다른데 기존 강의는 일률적임")
                        .solution("AI가 학습자 수준을 분석하여 맞춤형 학습 경로와 문제를 실시간 생성")
                        .aiUsage("학습 패턴 분석 ML 모델 + 개인화 문제 생성 LLM + 실시간 피드백 시스템")
                        .expectedScore(5)
                        .reasonForScore("AI가 핵심 가치(개인화)를 실현하는 필수 요소이며, 다층적으로 활용됨")
                        .build())
                .averageExample(ExampleProject.builder()
                        .name("보통 사례: 학습 도우미")
                        .problemDefinition("공부할 때 질문할 곳이 마땅치 않음")
                        .solution("AI 챗봇으로 학습 질문에 답변")
                        .aiUsage("ChatGPT API로 질문 답변")
                        .expectedScore(3)
                        .reasonForScore("AI를 활용하지만 기존 서비스와 차별점이 부족하고, 단순 API 호출 수준")
                        .build())
                .comparisonNotes("우수 사례는 AI가 핵심 가치 제안의 필수 요소이며, 여러 AI 기술을 복합적으로 활용하여 차별화된 경험을 제공합니다.")
                .build());

        return comparisons;
    }
}
