import api from './api';
import type { GrowthReport } from '../types';

export const growthReportService = {
  // 내 성장 리포트 목록 조회
  async getMyReports(): Promise<GrowthReport[]> {
    const response = await api.get('/users/me/growth-reports');
    return response.data.data;
  },

  // 프로젝트 팀 리포트 조회
  async getTeamReport(projectId: number): Promise<GrowthReport> {
    const response = await api.get(`/projects/${projectId}/growth-reports/team`);
    return response.data.data;
  },

  // 프로젝트 개인 리포트 조회
  async getMyIndividualReport(projectId: number): Promise<GrowthReport> {
    const response = await api.get(`/projects/${projectId}/growth-reports/me`);
    return response.data.data;
  },

  // 프로젝트의 모든 리포트 조회
  async getProjectReports(projectId: number): Promise<GrowthReport[]> {
    const response = await api.get(`/projects/${projectId}/growth-reports`);
    return response.data.data;
  },

  // 리포트 재생성
  async regenerateReport(reportId: number): Promise<GrowthReport> {
    const response = await api.post(`/growth-reports/${reportId}/regenerate`);
    return response.data.data;
  },
};

// 리포트 타입 라벨
export const REPORT_TYPE_LABELS: Record<string, string> = {
  TEAM: '팀 리포트',
  INDIVIDUAL: '개인 리포트',
};
