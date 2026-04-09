import api from './api';
import type { ApiResponse, Page, Season, SeasonCreateRequest, User, UserRole, CertificationStatus } from '../types';

export interface UserSearchParams {
  keyword?: string;
  role?: UserRole;
  certificationStatus?: CertificationStatus;
  page?: number;
  size?: number;
}

export const adminService = {
  // User Management APIs
  getUsers: async (params: UserSearchParams = {}): Promise<Page<User>> => {
    const response = await api.get<ApiResponse<Page<User>>>('/admin/users', {
      params: {
        keyword: params.keyword || undefined,
        role: params.role || undefined,
        certificationStatus: params.certificationStatus || undefined,
        page: params.page || 0,
        size: params.size || 20,
      },
    });
    return response.data.data;
  },

  getUser: async (userId: number): Promise<User> => {
    const response = await api.get<ApiResponse<User>>(`/admin/users/${userId}`);
    return response.data.data;
  },

  updateUserRole: async (userId: number, role: UserRole): Promise<User> => {
    const response = await api.patch<ApiResponse<User>>(`/admin/users/${userId}/role`, { role });
    return response.data.data;
  },

  deleteUser: async (userId: number): Promise<void> => {
    await api.delete(`/admin/users/${userId}`);
  },

  // Season APIs
  getSeasons: async (page = 0, size = 20): Promise<Page<Season>> => {
    const response = await api.get<ApiResponse<Page<Season>>>('/admin/seasons', {
      params: { page, size },
    });
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

  // Certification APIs (returns User, not CertificationRequest)
  getPendingCertifications: async (): Promise<User[]> => {
    const response = await api.get<ApiResponse<User[]>>('/admin/users/certifications/pending');
    return response.data.data;
  },

  approveCertification: async (userId: number): Promise<User> => {
    const response = await api.post<ApiResponse<User>>(`/admin/users/${userId}/certification/approve`);
    return response.data.data;
  },

  // Note: Backend doesn't accept rejection reason
  rejectCertification: async (userId: number): Promise<User> => {
    const response = await api.post<ApiResponse<User>>(`/admin/users/${userId}/certification/reject`);
    return response.data.data;
  },
};

export default adminService;
