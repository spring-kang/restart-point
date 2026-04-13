import api from './api';
import type { TeamInvitation, TeamInvitationRequest } from '../types';

export const invitationService = {
  // 영입 요청 발송 (팀 리더만 가능)
  async sendInvitation(teamId: number, request: TeamInvitationRequest): Promise<TeamInvitation> {
    const response = await api.post(`/teams/${teamId}/invitations`, request);
    return response.data.data;
  },

  // 팀에서 보낸 영입 요청 목록 조회 (팀 리더만 가능)
  async getTeamInvitations(teamId: number): Promise<TeamInvitation[]> {
    const response = await api.get(`/teams/${teamId}/invitations`);
    return response.data.data;
  },

  // 영입 요청 취소 (팀 리더만 가능)
  async cancelInvitation(teamId: number, invitationId: number): Promise<void> {
    await api.delete(`/teams/${teamId}/invitations/${invitationId}`);
  },

  // 내가 받은 영입 요청 목록 조회
  async getMyInvitations(): Promise<TeamInvitation[]> {
    const response = await api.get('/users/me/invitations');
    return response.data.data;
  },

  // 내가 받은 대기 중인 영입 요청 목록 조회
  async getMyPendingInvitations(): Promise<TeamInvitation[]> {
    const response = await api.get('/users/me/invitations/pending');
    return response.data.data;
  },

  // 대기 중인 영입 요청 수 조회
  async getPendingInvitationCount(): Promise<number> {
    const response = await api.get('/users/me/invitations/pending/count');
    return response.data.data;
  },

  // 영입 요청 수락
  async acceptInvitation(invitationId: number): Promise<TeamInvitation> {
    const response = await api.post(`/invitations/${invitationId}/accept`);
    return response.data.data;
  },

  // 영입 요청 거절
  async rejectInvitation(invitationId: number): Promise<TeamInvitation> {
    const response = await api.post(`/invitations/${invitationId}/reject`);
    return response.data.data;
  }
};

// 영입 요청 상태 라벨
export const INVITATION_STATUS_LABELS: Record<string, string> = {
  PENDING: '대기 중',
  ACCEPTED: '수락됨',
  REJECTED: '거절됨',
  EXPIRED: '만료됨',
};

// 영입 요청 상태 색상
export const INVITATION_STATUS_COLORS: Record<string, string> = {
  PENDING: 'bg-yellow-100 text-yellow-700',
  ACCEPTED: 'bg-green-100 text-green-700',
  REJECTED: 'bg-red-100 text-red-700',
  EXPIRED: 'bg-gray-100 text-gray-700',
};
