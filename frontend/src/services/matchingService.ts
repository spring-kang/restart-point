import api from './api';
import type { TeamRecommendation, MemberRecommendation } from '../types';

export const matchingService = {
  // 사용자에게 맞는 팀 추천 받기
  async getTeamRecommendations(seasonId: number, limit: number = 5): Promise<TeamRecommendation[]> {
    const response = await api.get('/matching/teams', {
      params: { seasonId, limit }
    });
    return response.data.data;
  },

  // 팀에 맞는 멤버 추천 받기 (팀 리더용)
  async getMemberRecommendations(teamId: number, limit: number = 5): Promise<MemberRecommendation[]> {
    const response = await api.get(`/matching/teams/${teamId}/members`, {
      params: { limit }
    });
    return response.data.data;
  }
};

// 일정 위험도 라벨
export const SCHEDULE_RISK_LABELS: Record<string, string> = {
  LOW: '낮음',
  MEDIUM: '보통',
  HIGH: '높음',
};

// 일정 위험도 색상
export const SCHEDULE_RISK_COLORS: Record<string, string> = {
  LOW: 'bg-green-100 text-green-700',
  MEDIUM: 'bg-yellow-100 text-yellow-700',
  HIGH: 'bg-red-100 text-red-700',
};
