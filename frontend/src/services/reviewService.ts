import api from './api';
import type {
  Review,
  ReviewSummary,
  ReviewCreateRequest,
  RubricItemInfo,
  RubricItem,
  Project,
  ReviewGuide,
  ReviewGuideStatus,
  ReviewPatternAnalysis,
  ReviewAnalysis,
} from '../types';

export const reviewService = {
  // 심사 제출
  async createReview(projectId: number, request: ReviewCreateRequest): Promise<Review> {
    const response = await api.post(`/projects/${projectId}/reviews`, request);
    return response.data.data;
  },

  // 프로젝트별 심사 목록 조회
  async getReviewsByProject(projectId: number): Promise<Review[]> {
    const response = await api.get(`/projects/${projectId}/reviews`);
    return response.data.data;
  },

  // 프로젝트 심사 요약 조회
  async getReviewSummary(projectId: number): Promise<ReviewSummary> {
    const response = await api.get(`/projects/${projectId}/review-summary`);
    return response.data.data;
  },

  // 심사 가능한 프로젝트 목록 조회
  async getReviewableProjects(seasonId: number): Promise<Project[]> {
    const response = await api.get(`/seasons/${seasonId}/reviewable-projects`);
    return response.data.data;
  },

  // 내가 심사한 목록 조회
  async getMyReviews(): Promise<Review[]> {
    const response = await api.get('/users/me/reviews');
    return response.data.data;
  },

  // 루브릭 항목 목록 조회
  async getRubricItems(): Promise<RubricItemInfo[]> {
    const response = await api.get('/rubric-items');
    return response.data.data;
  },

  // 심사 가이드 조회
  async getReviewGuide(): Promise<ReviewGuide> {
    const response = await api.get('/review-guide');
    return response.data.data;
  },

  // 가이드 완료 상태 조회
  async getGuideStatus(): Promise<ReviewGuideStatus> {
    const response = await api.get('/review-guide/status');
    return response.data.data;
  },

  // 루브릭 학습 완료
  async completeRubricLearning(): Promise<ReviewGuideStatus> {
    const response = await api.post('/review-guide/complete/rubric');
    return response.data.data;
  },

  // 사례 비교 완료
  async completeExampleComparison(): Promise<ReviewGuideStatus> {
    const response = await api.post('/review-guide/complete/examples');
    return response.data.data;
  },

  // 연습 평가 완료
  async completePracticeEvaluation(): Promise<ReviewGuideStatus> {
    const response = await api.post('/review-guide/complete/practice');
    return response.data.data;
  },

  // 심사 패턴 분석 조회
  async getMyReviewPattern(): Promise<ReviewPatternAnalysis> {
    const response = await api.get('/users/me/review-pattern');
    return response.data.data;
  },

  // === 운영자 전용 AI 심사 분석 API ===

  // 프로젝트 심사 AI 분석 조회 (운영자 전용)
  async getProjectReviewAnalysis(projectId: number): Promise<ReviewAnalysis> {
    const response = await api.get(`/admin/projects/${projectId}/review-analysis`);
    return response.data.data;
  },

  // 시즌 전체 심사 AI 분석 조회 (운영자 전용)
  async getSeasonReviewAnalysis(seasonId: number): Promise<ReviewAnalysis[]> {
    const response = await api.get(`/admin/seasons/${seasonId}/review-analysis`);
    return response.data.data;
  },
};

// 루브릭 항목 라벨
export const RUBRIC_ITEM_LABELS: Record<RubricItem, string> = {
  PROBLEM_DEFINITION: '문제 정의의 명확성',
  USER_VALUE: '사용자 가치',
  AI_USAGE: 'AI 활용 적절성',
  UX_COMPLETENESS: 'UX 완성도',
  TECHNICAL_FEASIBILITY: '기술 구현 가능성',
  COLLABORATION: '협업 완성도',
};

// 루브릭 항목 설명
export const RUBRIC_ITEM_DESCRIPTIONS: Record<RubricItem, string> = {
  PROBLEM_DEFINITION: '문제가 명확하게 정의되어 있고, 해결해야 할 핵심 이슈가 잘 드러나는가?',
  USER_VALUE: '타깃 사용자에게 실질적인 가치를 제공하는가?',
  AI_USAGE: 'AI를 적절하고 효과적으로 활용하고 있는가?',
  UX_COMPLETENESS: '사용자 경험이 직관적이고 완성도가 높은가?',
  TECHNICAL_FEASIBILITY: '기술적으로 구현 가능하고 확장 가능한 구조인가?',
  COLLABORATION: '팀원 간 협업이 잘 이루어졌고, 역할 분담이 적절한가?',
};

// 점수 라벨
export const SCORE_LABELS: Record<number, string> = {
  1: '매우 부족',
  2: '부족',
  3: '보통',
  4: '우수',
  5: '매우 우수',
};
