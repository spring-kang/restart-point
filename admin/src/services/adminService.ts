import api from './api';
import type { ApiResponse, Season, SeasonCreateRequest, CertificationRequest } from '../types';

export const adminService = {
  // Season APIs
  getSeasons: async (): Promise<Season[]> => {
    const response = await api.get<ApiResponse<Season[]>>('/admin/seasons');
    return response.data.data;
  },

  getSeason: async (seasonId: number): Promise<Season> => {
    const response = await api.get<ApiResponse<Season>>(`/admin/seasons/${seasonId}`);
    return response.data.data;
  },

  createSeason: async (data: SeasonCreateRequest): Promise<Season> => {
    const response = await api.post<ApiResponse<Season>>('/admin/seasons', data);
    return response.data.data;
  },

  updateSeason: async (seasonId: number, data: SeasonCreateRequest): Promise<Season> => {
    const response = await api.put<ApiResponse<Season>>(`/admin/seasons/${seasonId}`, data);
    return response.data.data;
  },

  updateSeasonStatus: async (seasonId: number, status: string): Promise<Season> => {
    const response = await api.patch<ApiResponse<Season>>(`/admin/seasons/${seasonId}/status`, { status });
    return response.data.data;
  },

  deleteSeason: async (seasonId: number): Promise<void> => {
    await api.delete(`/admin/seasons/${seasonId}`);
  },

  // Certification APIs
  getPendingCertifications: async (): Promise<CertificationRequest[]> => {
    const response = await api.get<ApiResponse<CertificationRequest[]>>('/admin/users/certifications/pending');
    return response.data.data;
  },

  approveCertification: async (userId: number): Promise<void> => {
    await api.post(`/admin/users/${userId}/certification/approve`);
  },

  rejectCertification: async (userId: number, reason: string): Promise<void> => {
    await api.post(`/admin/users/${userId}/certification/reject`, { reason });
  },
};

export default adminService;
