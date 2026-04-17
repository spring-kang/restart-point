import api from './api';
import type { ApiResponse, Page, Season, SeasonCreateRequest, User, UserRole, CertificationStatus, ReviewAnalysis, SeasonDashboard, OverallDashboard } from '../types';

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

  // Review Analysis APIs
  getProjectReviewAnalysis: async (projectId: number): Promise<ReviewAnalysis> => {
    const response = await api.get<ApiResponse<ReviewAnalysis>>(`/admin/projects/${projectId}/review-analysis`);
    return response.data.data;
  },

  getSeasonReviewAnalysis: async (seasonId: number): Promise<ReviewAnalysis[]> => {
    const response = await api.get<ApiResponse<ReviewAnalysis[]>>(`/admin/seasons/${seasonId}/review-analysis`);
    return response.data.data;
  },

  markProjectAsFeatured: async (projectId: number) => {
    const response = await api.post<ApiResponse<{ featuredRank?: number | null }>>(`/admin/projects/${projectId}/featured`);
    return response.data.data;
  },

  unmarkProjectAsFeatured: async (projectId: number) => {
    const response = await api.delete<ApiResponse<{ featuredRank?: number | null }>>(`/admin/projects/${projectId}/featured`);
    return response.data.data;
  },

  // Dashboard APIs
  getOverallDashboard: async (): Promise<OverallDashboard> => {
    const response = await api.get<ApiResponse<OverallDashboard>>('/admin/dashboard');
    return response.data.data;
  },

  getSeasonDashboard: async (seasonId: number): Promise<SeasonDashboard> => {
    const response = await api.get<ApiResponse<SeasonDashboard>>(`/admin/dashboard/seasons/${seasonId}`);
    return response.data.data;
  },

  // File APIs
  getPresignedUrl: async (fileUrl: string): Promise<string> => {
    const response = await api.get<{ presignedUrl: string }>('/files/presign', {
      params: { url: fileUrl },
    });
    return response.data.presignedUrl;
  },

  // ========== Guide/Template APIs ==========
  // Note: GuideController returns bare ResponseEntity<T>, not ApiResponse wrapper
  getProjectTemplates: async (seasonId: number): Promise<any[]> => {
    const response = await api.get<any[]>(`/seasons/${seasonId}/templates`);
    return response.data;
  },

  createProjectTemplate: async (seasonId: number, data: any): Promise<any> => {
    const response = await api.post<any>(`/seasons/${seasonId}/templates`, data);
    return response.data;
  },

  updateProjectTemplate: async (templateId: number, data: any): Promise<any> => {
    const response = await api.put<any>(`/templates/${templateId}`, data);
    return response.data;
  },

  deleteProjectTemplate: async (templateId: number): Promise<void> => {
    await api.delete(`/templates/${templateId}`);
  },

  activateProjectTemplate: async (templateId: number): Promise<void> => {
    await api.post(`/templates/${templateId}/activate`);
  },

  getWeeklyGuidelines: async (templateId: number): Promise<any[]> => {
    const response = await api.get<any[]>(`/templates/${templateId}/guidelines`);
    return response.data;
  },

  createWeeklyGuideline: async (templateId: number, data: any): Promise<any> => {
    const response = await api.post<any>(`/templates/${templateId}/guidelines`, data);
    return response.data;
  },

  updateWeeklyGuideline: async (guidelineId: number, data: any): Promise<any> => {
    const response = await api.put<any>(`/guidelines/${guidelineId}`, data);
    return response.data;
  },

  deleteWeeklyGuideline: async (guidelineId: number): Promise<void> => {
    await api.delete(`/guidelines/${guidelineId}`);
  },

  // ========== Mentoring APIs ==========
  // Note: MentoringController returns bare ResponseEntity<T>, not ApiResponse wrapper
  getMentorings: async (seasonId: number): Promise<any[]> => {
    const response = await api.get<any[]>(`/seasons/${seasonId}/mentorings`);
    return response.data;
  },

  createMentoring: async (seasonId: number, data: any): Promise<any> => {
    const response = await api.post<any>(`/seasons/${seasonId}/mentorings`, data);
    return response.data;
  },

  updateMentoring: async (mentoringId: number, data: any): Promise<any> => {
    const response = await api.put<any>(`/mentorings/${mentoringId}`, data);
    return response.data;
  },

  deleteMentoring: async (mentoringId: number): Promise<void> => {
    await api.delete(`/mentorings/${mentoringId}`);
  },

  getMentoringModules: async (mentoringId: number): Promise<any[]> => {
    const response = await api.get<any[]>(`/mentorings/${mentoringId}/modules`);
    return response.data;
  },

  createMentoringModule: async (mentoringId: number, data: any): Promise<any> => {
    const response = await api.post<any>(`/mentorings/${mentoringId}/modules`, data);
    return response.data;
  },

  updateMentoringModule: async (moduleId: number, data: any): Promise<any> => {
    const response = await api.put<any>(`/modules/${moduleId}`, data);
    return response.data;
  },

  deleteMentoringModule: async (moduleId: number): Promise<void> => {
    await api.delete(`/modules/${moduleId}`);
  },

  // ========== Payment APIs ==========
  // Note: PaymentController returns bare ResponseEntity<T>, not ApiResponse wrapper
  getPricingPlans: async (seasonId: number): Promise<any[]> => {
    const response = await api.get<any[]>(`/seasons/${seasonId}/pricing-plans`);
    return response.data;
  },

  createPricingPlan: async (seasonId: number, data: any): Promise<any> => {
    const response = await api.post<any>(`/seasons/${seasonId}/pricing-plans`, data);
    return response.data;
  },

  updatePricingPlan: async (planId: number, data: any): Promise<any> => {
    const response = await api.put<any>(`/pricing-plans/${planId}`, data);
    return response.data;
  },

  deletePricingPlan: async (planId: number): Promise<void> => {
    await api.delete(`/pricing-plans/${planId}`);
  },

  getOrders: async (seasonId: number): Promise<any[]> => {
    const response = await api.get<ApiResponse<any[]>>(`/admin/seasons/${seasonId}/orders`);
    return response.data.data;
  },

  refundOrder: async (orderId: number, reason: string): Promise<any> => {
    const response = await api.post<ApiResponse<any>>(`/admin/orders/${orderId}/refund`, { reason });
    return response.data.data;
  },

  getSubscriptions: async (seasonId: number): Promise<any[]> => {
    const response = await api.get<ApiResponse<any[]>>(`/admin/seasons/${seasonId}/subscriptions`);
    return response.data.data;
  },

  getPaymentStats: async (seasonId: number): Promise<any> => {
    const response = await api.get<ApiResponse<any>>(`/admin/seasons/${seasonId}/payment-stats`);
    return response.data.data;
  },
};

export default adminService;
