package com.restartpoint.domain.review.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.restartpoint.domain.review.dto.*;
import com.restartpoint.domain.review.entity.ReviewType;
import com.restartpoint.domain.review.entity.RubricItem;
import com.restartpoint.domain.review.service.ReviewAssistantService;
import com.restartpoint.domain.review.service.ReviewPatternService;
import com.restartpoint.domain.review.service.ReviewService;
import com.restartpoint.global.exception.BusinessException;
import com.restartpoint.global.exception.ErrorCode;
import com.restartpoint.global.security.CustomUserPrincipal;
import com.restartpoint.global.security.JwtAuthenticationFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReviewController.class)
@AutoConfigureMockMvc(addFilters = false)
class ReviewControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockBean
  private ReviewService reviewService;

  @MockBean
  private ReviewPatternService reviewPatternService;

  @MockBean
  private ReviewAssistantService reviewAssistantService;

  @MockBean
  private JwtAuthenticationFilter jwtAuthenticationFilter;

  @BeforeEach
  void setUp() {
    SecurityContextHolder.clearContext();
  }

  private void setAuthentication(Long userId, String email, String role) {
    CustomUserPrincipal principal = new CustomUserPrincipal(userId, email, role);
    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
        principal, null, List.of(new SimpleGrantedAuthority("ROLE_" + role))
    );
    SecurityContextHolder.getContext().setAuthentication(authentication);
  }

  @Nested
  @DisplayName("심사 제출 API")
  class CreateReviewApi {

    @Test
    @DisplayName("심사 제출에 성공한다")
    void createReviewSuccess() throws Exception {
      // given
      setAuthentication(1L, "reviewer@test.com", "USER");
      ReviewCreateRequest request = createReviewCreateRequest();
      ReviewResponse response = createReviewResponse(1L);

      given(reviewService.createReview(eq(1L), eq(1L), any(ReviewCreateRequest.class))).willReturn(response);

      // when & then
      mockMvc.perform(post("/api/v1/projects/1/reviews")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success").value(true))
          .andExpect(jsonPath("$.data.id").value(1))
          .andExpect(jsonPath("$.message").value("심사가 제출되었습니다."));
    }

    @Test
    @DisplayName("수료 인증되지 않은 사용자가 심사하면 실패한다")
    void createReviewFailsWhenNotCertified() throws Exception {
      // given
      setAuthentication(1L, "user@test.com", "USER");
      ReviewCreateRequest request = createReviewCreateRequest();

      given(reviewService.createReview(eq(1L), eq(1L), any(ReviewCreateRequest.class)))
          .willThrow(new BusinessException(ErrorCode.NOT_CERTIFIED_REVIEWER));

      // when & then
      mockMvc.perform(post("/api/v1/projects/1/reviews")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isForbidden())
          .andExpect(jsonPath("$.success").value(false))
          .andExpect(jsonPath("$.errorCode").value("REVIEW_007"));
    }

    @Test
    @DisplayName("자신의 프로젝트를 심사하려고 하면 실패한다")
    void createReviewFailsWhenOwnProject() throws Exception {
      // given
      setAuthentication(1L, "user@test.com", "USER");
      ReviewCreateRequest request = createReviewCreateRequest();

      given(reviewService.createReview(eq(1L), eq(1L), any(ReviewCreateRequest.class)))
          .willThrow(new BusinessException(ErrorCode.CANNOT_REVIEW_OWN_PROJECT));

      // when & then
      mockMvc.perform(post("/api/v1/projects/1/reviews")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isForbidden())
          .andExpect(jsonPath("$.success").value(false))
          .andExpect(jsonPath("$.errorCode").value("REVIEW_005"));
    }
  }

  @Nested
  @DisplayName("심사 조회 API")
  class GetReviewsApi {

    @Test
    @DisplayName("프로젝트별 심사 목록 조회에 성공한다")
    void getReviewsByProjectSuccess() throws Exception {
      // given
      setAuthentication(1L, "member@test.com", "USER");
      List<ReviewResponse> reviews = List.of(createReviewResponse(1L), createReviewResponse(2L));
      given(reviewService.getReviewsByProject(1L, 1L)).willReturn(reviews);

      // when & then
      mockMvc.perform(get("/api/v1/projects/1/reviews"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success").value(true))
          .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    @DisplayName("권한이 없으면 심사 목록 조회에 실패한다")
    void getReviewsByProjectFailsWhenNotAuthorized() throws Exception {
      // given
      setAuthentication(3L, "other@test.com", "USER");
      given(reviewService.getReviewsByProject(3L, 1L))
          .willThrow(new BusinessException(ErrorCode.ACCESS_DENIED));

      // when & then
      mockMvc.perform(get("/api/v1/projects/1/reviews"))
          .andExpect(status().isForbidden())
          .andExpect(jsonPath("$.success").value(false));
    }
  }

  @Nested
  @DisplayName("심사 요약 API")
  class GetReviewSummaryApi {

    @Test
    @DisplayName("프로젝트 심사 요약 조회에 성공한다")
    void getReviewSummarySuccess() throws Exception {
      // given
      setAuthentication(1L, "member@test.com", "USER");
      ReviewSummaryResponse summary = createReviewSummaryResponse();
      given(reviewService.getReviewSummary(1L, 1L)).willReturn(summary);

      // when & then
      mockMvc.perform(get("/api/v1/projects/1/review-summary"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success").value(true))
          .andExpect(jsonPath("$.data.projectId").value(1))
          .andExpect(jsonPath("$.data.totalReviewCount").value(10))
          .andExpect(jsonPath("$.data.weightedAverageScore").value(4.5));
    }
  }

  @Nested
  @DisplayName("심사 가능 프로젝트 조회 API")
  class GetReviewableProjectsApi {

    @Test
    @DisplayName("심사 가능한 프로젝트 목록 조회에 성공한다")
    void getReviewableProjectsSuccess() throws Exception {
      // given
      setAuthentication(1L, "reviewer@test.com", "USER");
      given(reviewService.getReviewableProjects(1L, 1L)).willReturn(List.of());

      // when & then
      mockMvc.perform(get("/api/v1/seasons/1/reviewable-projects"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success").value(true))
          .andExpect(jsonPath("$.data").isArray());
    }
  }

  @Nested
  @DisplayName("내 심사 목록 조회 API")
  class GetMyReviewsApi {

    @Test
    @DisplayName("내 심사 목록 조회에 성공한다")
    void getMyReviewsSuccess() throws Exception {
      // given
      setAuthentication(1L, "reviewer@test.com", "USER");
      List<ReviewResponse> reviews = List.of(createReviewResponse(1L));
      given(reviewService.getMyReviews(1L)).willReturn(reviews);

      // when & then
      mockMvc.perform(get("/api/v1/users/me/reviews"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success").value(true))
          .andExpect(jsonPath("$.data.length()").value(1));
    }
  }

  @Nested
  @DisplayName("루브릭 항목 조회 API")
  class GetRubricItemsApi {

    @Test
    @DisplayName("루브릭 항목 목록 조회에 성공한다")
    void getRubricItemsSuccess() throws Exception {
      // when & then
      mockMvc.perform(get("/api/v1/rubric-items"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success").value(true))
          .andExpect(jsonPath("$.data.length()").value(RubricItem.values().length));
    }
  }

  @Nested
  @DisplayName("내 심사 패턴 분석 API")
  class GetMyReviewPatternApi {

    @Test
    @DisplayName("내 심사 패턴 분석 조회에 성공한다")
    void getMyReviewPatternSuccess() throws Exception {
      // given
      setAuthentication(1L, "reviewer@test.com", "USER");
      ReviewPatternAnalysisResponse analysis = createPatternAnalysisResponse();
      given(reviewPatternService.analyzeMyPattern(1L)).willReturn(analysis);

      // when & then
      mockMvc.perform(get("/api/v1/users/me/review-pattern"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success").value(true))
          .andExpect(jsonPath("$.data.totalReviewCount").value(10));
    }
  }

  @Nested
  @DisplayName("운영자 전용 AI 심사 분석 API")
  class AdminReviewAnalysisApi {

    @Test
    @DisplayName("운영자가 프로젝트 심사 AI 분석을 조회할 수 있다")
    void getProjectReviewAnalysisAsAdmin() throws Exception {
      // given
      setAuthentication(1L, "admin@test.com", "ADMIN");
      ReviewAnalysisResponse analysis = createReviewAnalysisResponse();
      given(reviewAssistantService.analyzeProjectReviews(1L)).willReturn(analysis);

      // when & then
      mockMvc.perform(get("/api/v1/admin/projects/1/review-analysis"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success").value(true))
          .andExpect(jsonPath("$.data.projectId").value(1));
    }

    @Test
    @DisplayName("운영자가 시즌 전체 심사 AI 분석을 조회할 수 있다")
    void getSeasonReviewAnalysisAsAdmin() throws Exception {
      // given
      setAuthentication(1L, "admin@test.com", "ADMIN");
      List<ReviewAnalysisResponse> analyses = List.of(createReviewAnalysisResponse());
      given(reviewAssistantService.analyzeSeasonReviews(1L)).willReturn(analyses);

      // when & then
      mockMvc.perform(get("/api/v1/admin/seasons/1/review-analysis"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success").value(true))
          .andExpect(jsonPath("$.data.length()").value(1));
    }
  }

  // 헬퍼 메서드
  private ReviewCreateRequest createReviewCreateRequest() {
    ReviewCreateRequest request = new ReviewCreateRequest();
    try {
      var scores = List.of(
          createScoreRequest(RubricItem.PROBLEM_DEFINITION, 5),
          createScoreRequest(RubricItem.USER_VALUE, 4),
          createScoreRequest(RubricItem.AI_USAGE, 5),
          createScoreRequest(RubricItem.UX_COMPLETENESS, 4),
          createScoreRequest(RubricItem.TECHNICAL_FEASIBILITY, 5),
          createScoreRequest(RubricItem.COLLABORATION, 4)
      );
      var field = ReviewCreateRequest.class.getDeclaredField("scores");
      field.setAccessible(true);
      field.set(request, scores);

      field = ReviewCreateRequest.class.getDeclaredField("overallComment");
      field.setAccessible(true);
      field.set(request, "좋은 프로젝트입니다.");
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    return request;
  }

  private ReviewScoreRequest createScoreRequest(RubricItem item, int score) {
    ReviewScoreRequest request = new ReviewScoreRequest();
    try {
      var field = ReviewScoreRequest.class.getDeclaredField("rubricItem");
      field.setAccessible(true);
      field.set(request, item);

      field = ReviewScoreRequest.class.getDeclaredField("score");
      field.setAccessible(true);
      field.set(request, score);

      field = ReviewScoreRequest.class.getDeclaredField("comment");
      field.setAccessible(true);
      field.set(request, "잘함");
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    return request;
  }

  private ReviewResponse createReviewResponse(Long id) {
    return ReviewResponse.builder()
        .id(id)
        .projectId(1L)
        .projectName("프로젝트")
        .reviewerId(1L)
        .reviewerName("심사자")
        .reviewType(ReviewType.CANDIDATE)
        .overallComment("좋은 프로젝트입니다.")
        .averageScore(4.5)
        .scores(List.of())
        .submittedAt(LocalDateTime.now())
        .build();
  }

  private ReviewSummaryResponse createReviewSummaryResponse() {
    Map<RubricItem, Double> rubricAverages = new EnumMap<>(RubricItem.class);
    for (RubricItem item : RubricItem.values()) {
      rubricAverages.put(item, 4.5);
    }

    return ReviewSummaryResponse.builder()
        .projectId(1L)
        .projectName("프로젝트")
        .totalReviewCount(10)
        .expertReviewCount(3)
        .candidateReviewCount(7)
        .weightedAverageScore(4.5)
        .expertAverageScore(4.6)
        .candidateAverageScore(4.4)
        .rubricAverages(rubricAverages)
        .expertRubricAverages(rubricAverages)
        .candidateRubricAverages(rubricAverages)
        .build();
  }

  private ReviewPatternAnalysisResponse createPatternAnalysisResponse() {
    return ReviewPatternAnalysisResponse.builder()
        .totalReviewCount(10)
        .build();
  }

  private ReviewAnalysisResponse createReviewAnalysisResponse() {
    return ReviewAnalysisResponse.builder()
        .projectId(1L)
        .projectName("프로젝트")
        .build();
  }
}
