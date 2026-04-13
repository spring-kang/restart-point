package com.restartpoint.infra.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.restartpoint.domain.review.dto.ReviewAnalysisResponse.OutlierScore;
import com.restartpoint.domain.review.dto.ReviewAnalysisResponse.RubricAnalysis;
import com.restartpoint.domain.review.entity.Review;
import com.restartpoint.domain.review.entity.ReviewScore;
import com.restartpoint.domain.review.entity.RubricItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * AI 심사 보조 서비스
 * 심사 데이터를 분석하여 운영자에게 인사이트 제공
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AiReviewAssistantService {

    private final GroqService groqService;
    private final ObjectMapper objectMapper;

    private static final String SYSTEM_PROMPT = """
            당신은 프로젝트 심사 데이터를 분석하는 전문가입니다.
            주어진 심사 데이터를 분석하여 운영자가 심사 품질을 검토하는데 도움이 되는 인사이트를 제공합니다.

            분석 시 다음 사항을 고려하세요:
            1. 심사 코멘트에서 반복되는 키워드와 주요 피드백 추출
            2. 전문가(EXPERT) 평가 기준에서 특정 루브릭 항목의 강점과 약점 파악
            3. 평균에서 크게 벗어나는 이상치 점수의 가능한 원인 분석

            응답은 반드시 JSON 형식으로 제공하세요.
            """;

    /**
     * 심사 코멘트 종합 요약
     */
    public String summarizeComments(List<Review> reviews, String projectName) {
        if (reviews.isEmpty()) {
            return "심사 데이터가 없습니다.";
        }

        List<String> comments = reviews.stream()
                .filter(r -> r.getOverallComment() != null && !r.getOverallComment().isBlank())
                .map(Review::getOverallComment)
                .toList();

        if (comments.isEmpty()) {
            return "작성된 종합 코멘트가 없습니다.";
        }

        String userMessage = String.format("""
                프로젝트 '%s'에 대한 심사 코멘트를 요약해주세요.

                심사 코멘트 목록:
                %s

                다음 형식의 JSON으로 응답하세요:
                {
                    "summary": "전체 코멘트의 핵심 내용을 2-3문장으로 요약",
                    "strengths": ["강점1", "강점2"],
                    "weaknesses": ["약점1", "약점2"],
                    "commonFeedback": "가장 많이 언급된 피드백"
                }
                """, projectName, String.join("\n---\n", comments));

        String response = groqService.chat(SYSTEM_PROMPT, userMessage);
        if (response == null) {
            return "AI 분석에 실패했습니다.";
        }

        try {
            Map<String, Object> result = objectMapper.readValue(extractJson(response), Map.class);
            return (String) result.getOrDefault("summary", "요약을 생성할 수 없습니다.");
        } catch (JsonProcessingException e) {
            log.warn("코멘트 요약 파싱 실패: {}", e.getMessage());
            return response;
        }
    }

    /**
     * 강점/약점 분석
     */
    public Map<String, List<String>> analyzeStrengthsAndWeaknesses(
            List<Review> reviews, String projectName, Map<RubricItem, Double> rubricAverages) {

        Map<String, List<String>> result = new HashMap<>();
        result.put("strengths", new ArrayList<>());
        result.put("weaknesses", new ArrayList<>());

        if (reviews.isEmpty()) {
            return result;
        }

        // 루브릭 점수 기반 강점/약점 추출
        StringBuilder rubricInfo = new StringBuilder();
        for (Map.Entry<RubricItem, Double> entry : rubricAverages.entrySet()) {
            rubricInfo.append(String.format("- %s: %.1f점\n",
                    entry.getKey().getLabel(), entry.getValue()));
        }

        // 코멘트 수집
        List<String> allComments = new ArrayList<>();
        for (Review review : reviews) {
            if (review.getOverallComment() != null) {
                allComments.add(review.getOverallComment());
            }
            for (ReviewScore score : review.getScores()) {
                if (score.getComment() != null && !score.getComment().isBlank()) {
                    allComments.add(String.format("[%s] %s",
                            score.getRubricItem().getLabel(), score.getComment()));
                }
            }
        }

        String userMessage = String.format("""
                프로젝트 '%s'의 심사 데이터를 분석하여 강점과 약점을 추출해주세요.

                루브릭별 평균 점수:
                %s

                심사 코멘트 (일부):
                %s

                다음 형식의 JSON으로 응답하세요:
                {
                    "strengths": ["구체적인 강점1", "구체적인 강점2", "구체적인 강점3"],
                    "weaknesses": ["구체적인 약점1", "구체적인 약점2", "구체적인 약점3"]
                }

                각 항목은 구체적이고 실행 가능한 피드백이어야 합니다.
                """,
                projectName,
                rubricInfo.toString(),
                allComments.stream().limit(10).collect(Collectors.joining("\n")));

        String response = groqService.chat(SYSTEM_PROMPT, userMessage);
        if (response == null) {
            return result;
        }

        try {
            Map<String, Object> parsed = objectMapper.readValue(extractJson(response), Map.class);
            result.put("strengths", (List<String>) parsed.getOrDefault("strengths", List.of()));
            result.put("weaknesses", (List<String>) parsed.getOrDefault("weaknesses", List.of()));
        } catch (JsonProcessingException e) {
            log.warn("강점/약점 분석 파싱 실패: {}", e.getMessage());
        }

        return result;
    }

    public String summarizeExpertReviewPolicy() {
        return "현재 시즌 심사는 전문가 평가만 운영합니다. 프로젝트별 평균, 강점, 약점, 이상치 분석은 모두 전문가 심사 데이터만 기준으로 집계합니다.";
    }

    /**
     * 이상치 점수 원인 분석
     */
    public String analyzeOutlierReason(OutlierScore outlier, String projectContext) {
        String userMessage = String.format("""
                다음 이상치 점수의 가능한 원인을 분석해주세요.

                프로젝트 정보: %s

                이상치 정보:
                - 심사자 유형: %s
                - 평가 항목: %s
                - 부여 점수: %d점
                - 해당 항목 평균: %.1f점
                - 평균 대비 편차: %+.1f점

                가능한 원인을 1-2문장으로 설명해주세요.
                JSON 형식: {"reason": "원인 설명"}
                """,
                projectContext,
                outlier.getReviewType(),
                outlier.getRubricItem().getLabel(),
                outlier.getScore(),
                outlier.getAverageScore(),
                outlier.getDeviation());

        String response = groqService.chat(SYSTEM_PROMPT, userMessage);
        if (response == null) {
            return "원인 분석 불가";
        }

        try {
            Map<String, Object> parsed = objectMapper.readValue(extractJson(response), Map.class);
            return (String) parsed.getOrDefault("reason", "원인 분석 불가");
        } catch (JsonProcessingException e) {
            return response;
        }
    }

    /**
     * 루브릭별 AI 인사이트 생성
     */
    public String generateRubricInsight(RubricItem item, double avgScore,
            double expertAvg, List<String> relatedComments) {

        String commentsText = relatedComments.isEmpty() ? "없음" :
                String.join("\n", relatedComments.stream().limit(5).toList());

        String userMessage = String.format("""
                '%s' 항목에 대한 심사 인사이트를 생성해주세요.

                점수 현황:
                - 전체 평균: %.1f점
                - 전문가 평균: %.1f점

                관련 코멘트:
                %s

                1문장으로 핵심 인사이트를 제공하세요.
                JSON 형식: {"insight": "인사이트 내용"}
                """,
                item.getLabel(), avgScore, expertAvg, commentsText);

        String response = groqService.chat(SYSTEM_PROMPT, userMessage);
        if (response == null) {
            return String.format("평균 %.1f점으로 %s 수준입니다.",
                    avgScore, avgScore >= 4 ? "우수" : avgScore >= 3 ? "보통" : "개선 필요");
        }

        try {
            Map<String, Object> parsed = objectMapper.readValue(extractJson(response), Map.class);
            return (String) parsed.getOrDefault("insight", response);
        } catch (JsonProcessingException e) {
            return response;
        }
    }

    /**
     * JSON 추출 헬퍼 (마크다운 코드 블록 제거)
     */
    private String extractJson(String response) {
        if (response == null) return "{}";

        String trimmed = response.trim();
        if (trimmed.startsWith("```json")) {
            trimmed = trimmed.substring(7);
        } else if (trimmed.startsWith("```")) {
            trimmed = trimmed.substring(3);
        }
        if (trimmed.endsWith("```")) {
            trimmed = trimmed.substring(0, trimmed.length() - 3);
        }
        return trimmed.trim();
    }
}
