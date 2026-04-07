import api from './api';

export interface Season {
  id: number;
  title: string;
  description: string;
  status: SeasonStatus;
  recruitmentStartAt: string;
  recruitmentEndAt: string;
  teamBuildingStartAt: string;
  teamBuildingEndAt: string;
  projectStartAt: string;
  projectEndAt: string;
  reviewStartAt: string;
  reviewEndAt: string;
  expertReviewWeight: number;
  candidateReviewWeight: number;
  createdAt: string;
  updatedAt: string;
  currentPhase: string;
  canJoin: boolean;
}

export type SeasonStatus =
  | 'DRAFT'
  | 'RECRUITING'
  | 'TEAM_BUILDING'
  | 'IN_PROGRESS'
  | 'REVIEWING'
  | 'COMPLETED';

export interface SeasonRequest {
  title: string;
  description?: string;
  recruitmentStartAt: string;
  recruitmentEndAt: string;
  teamBuildingStartAt: string;
  teamBuildingEndAt: string;
  projectStartAt: string;
  projectEndAt: string;
  reviewStartAt: string;
  reviewEndAt: string;
  expertReviewWeight?: number;
  candidateReviewWeight?: number;
}

export const seasonService = {
  // 공개 시즌 목록 조회
  async getPublicSeasons(): Promise<Season[]> {
    const response = await api.get('/seasons');
    return response.data.data;
  },

  // 현재 참여 가능한 시즌 조회
  async getActiveSeasons(): Promise<Season[]> {
    const response = await api.get('/seasons/active');
    return response.data.data;
  },

  // 시즌 상세 조회
  async getSeason(seasonId: number): Promise<Season> {
    const response = await api.get(`/seasons/${seasonId}`);
    return response.data.data;
  },

  // === 운영자 전용 API ===

  // 모든 시즌 목록 조회 (DRAFT 포함)
  async getAllSeasons(page = 0, size = 10): Promise<{ content: Season[]; totalPages: number; totalElements: number }> {
    const response = await api.get('/admin/seasons', { params: { page, size } });
    return response.data.data;
  },

  // 시즌 생성
  async createSeason(request: SeasonRequest): Promise<Season> {
    const response = await api.post('/admin/seasons', request);
    return response.data.data;
  },

  // 시즌 수정
  async updateSeason(seasonId: number, request: SeasonRequest): Promise<Season> {
    const response = await api.put(`/admin/seasons/${seasonId}`, request);
    return response.data.data;
  },

  // 시즌 상태 ���경
  async updateSeasonStatus(seasonId: number, status: SeasonStatus): Promise<Season> {
    const response = await api.patch(`/admin/seasons/${seasonId}/status`, { status });
    return response.data.data;
  },

  // 시즌 삭제
  async deleteSeason(seasonId: number): Promise<void> {
    await api.delete(`/admin/seasons/${seasonId}`);
  },
};

// 상태 라벨
export const SEASON_STATUS_LABELS: Record<SeasonStatus, string> = {
  DRAFT: '초안',
  RECRUITING: '모집 ���',
  TEAM_BUILDING: '팀빌딩',
  IN_PROGRESS: '진행 중',
  REVIEWING: '심사 중',
  COMPLETED: '��료',
};

// 상태 색상
export const SEASON_STATUS_COLORS: Record<SeasonStatus, string> = {
  DRAFT: 'bg-neutral-100 text-neutral-700',
  RECRUITING: 'bg-green-100 text-green-700',
  TEAM_BUILDING: 'bg-blue-100 text-blue-700',
  IN_PROGRESS: 'bg-purple-100 text-purple-700',
  REVIEWING: 'bg-orange-100 text-orange-700',
  COMPLETED: 'bg-neutral-200 text-neutral-600',
};
