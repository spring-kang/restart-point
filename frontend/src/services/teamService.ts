import api from './api';
import type { Team, TeamMember, TeamRequest, TeamApplyRequest, TeamStatus, JobRole } from '../types';

export const teamService = {
  // 팀 생성
  async createTeam(request: TeamRequest): Promise<Team> {
    const response = await api.post('/teams', request);
    return response.data.data;
  },

  // 팀 상세 조회
  async getTeam(teamId: number): Promise<Team> {
    const response = await api.get(`/teams/${teamId}`);
    return response.data.data;
  },

  // 시즌별 팀 목록 조회
  async getTeamsBySeason(seasonId: number): Promise<Team[]> {
    const response = await api.get(`/seasons/${seasonId}/teams`);
    return response.data.data;
  },

  // 시즌별 모집 중인 팀 목록 조회
  async getRecruitingTeams(seasonId: number): Promise<Team[]> {
    const response = await api.get(`/seasons/${seasonId}/teams/recruiting`);
    return response.data.data;
  },

  // 내가 리더인 팀 목록 조회
  async getMyTeams(): Promise<Team[]> {
    const response = await api.get('/users/me/teams');
    return response.data.data;
  },

  // 내가 멤버로 속한 팀 목록 조회
  async getTeamsAsMember(): Promise<Team[]> {
    const response = await api.get('/users/me/teams/member');
    return response.data.data;
  },

  // 팀 정보 수정 (리더만 가능)
  async updateTeam(teamId: number, request: TeamRequest): Promise<Team> {
    const response = await api.put(`/teams/${teamId}`, request);
    return response.data.data;
  },

  // 팀 상태 변경 (리더만 가능)
  async updateTeamStatus(teamId: number, status: TeamStatus): Promise<Team> {
    const response = await api.patch(`/teams/${teamId}/status`, { status });
    return response.data.data;
  },

  // 팀 지원
  async applyToTeam(teamId: number, request: TeamApplyRequest): Promise<TeamMember> {
    const response = await api.post(`/teams/${teamId}/applications`, request);
    return response.data.data;
  },

  // 팀 지원 목록 조회 (리더만 가능)
  async getTeamApplications(teamId: number): Promise<TeamMember[]> {
    const response = await api.get(`/teams/${teamId}/applications`);
    return response.data.data;
  },

  // 팀 멤버 목록 조회
  async getTeamMembers(teamId: number): Promise<TeamMember[]> {
    const response = await api.get(`/teams/${teamId}/members`);
    return response.data.data;
  },

  // 팀 지원 수락 (리더만 가능)
  async acceptApplication(teamId: number, memberId: number): Promise<TeamMember> {
    const response = await api.post(`/teams/${teamId}/applications/${memberId}/accept`);
    return response.data.data;
  },

  // 팀 지원 거절 (리더만 가능)
  async rejectApplication(teamId: number, memberId: number): Promise<TeamMember> {
    const response = await api.post(`/teams/${teamId}/applications/${memberId}/reject`);
    return response.data.data;
  },

  // 팀 탈퇴
  async leaveTeam(teamId: number): Promise<void> {
    await api.delete(`/teams/${teamId}/members/me`);
  },

  // 내 지원 현황 조회
  async getMyApplications(): Promise<TeamMember[]> {
    const response = await api.get('/users/me/applications');
    return response.data.data;
  },
};

// 팀 상태 라벨
export const TEAM_STATUS_LABELS: Record<TeamStatus, string> = {
  RECRUITING: '모집 중',
  COMPLETE: '팀 구성 완료',
  IN_PROGRESS: '프로젝트 진행 중',
  SUBMITTED: '제출 완료',
  REVIEWED: '심사 완료',
};

// 팀 상태 색상
export const TEAM_STATUS_COLORS: Record<TeamStatus, string> = {
  RECRUITING: 'bg-green-100 text-green-700',
  COMPLETE: 'bg-blue-100 text-blue-700',
  IN_PROGRESS: 'bg-purple-100 text-purple-700',
  SUBMITTED: 'bg-orange-100 text-orange-700',
  REVIEWED: 'bg-neutral-200 text-neutral-600',
};

// 역할 라벨
export const JOB_ROLE_LABELS: Record<JobRole, string> = {
  PLANNER: '기획자',
  UXUI: 'UX/UI 디자이너',
  FRONTEND: '프론트엔드',
  BACKEND: '백엔드',
};

// 역할 색상
export const JOB_ROLE_COLORS: Record<JobRole, string> = {
  PLANNER: 'bg-pink-100 text-pink-700',
  UXUI: 'bg-purple-100 text-purple-700',
  FRONTEND: 'bg-blue-100 text-blue-700',
  BACKEND: 'bg-green-100 text-green-700',
};
