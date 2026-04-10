package com.restartpoint.domain.review.service;

import com.restartpoint.domain.review.dto.ReviewPatternAnalysisResponse;
import com.restartpoint.domain.review.entity.Review;
import com.restartpoint.domain.review.entity.ReviewScore;
import com.restartpoint.domain.review.entity.ReviewType;
import com.restartpoint.domain.review.entity.RubricItem;
import com.restartpoint.domain.review.repository.ReviewRepository;
import com.restartpoint.infra.ai.AiReviewPatternService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 심사 패턴 분석 서비스
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class ReviewPatternService {

    private final ReviewRepository reviewRepository;
    private final AiReviewPatternService aiReviewPatternService;

    /**
     * 사용자의 심사 패턴 분석
     */
    public ReviewPatternAnalysisResponse analyzeMyPattern(Long userId) {
        // 사용자의 모든 심사 조회
        List<Review> myReviews = reviewRepository.findByReviewerIdWithProject(userId);

        // 전문가 심사자들의 평균 점수 조회 (비교용)
        Map<RubricItem, Double> expertAverages = calculateExpertAverages();

        // AI 분석 수행
        return aiReviewPatternService.analyzePattern(myReviews, expertAverages);
    }

    /**
     * 전문가 심사자들의 루브릭별 평균 점수 계산
     */
    private Map<RubricItem, Double> calculateExpertAverages() {
        List<Review> expertReviews = reviewRepository.findByReviewType(ReviewType.EXPERT);

        Map<RubricItem, Double> averages = new EnumMap<>(RubricItem.class);

        for (RubricItem item : RubricItem.values()) {
            double avg = expertReviews.stream()
                    .flatMap(r -> r.getScores().stream())
                    .filter(s -> s.getRubricItem() == item)
                    .mapToInt(ReviewScore::getScore)
                    .average()
                    .orElse(3.0); // 데이터 없으면 중간값 사용
            averages.put(item, Math.round(avg * 100.0) / 100.0);
        }

        return averages;
    }
}
