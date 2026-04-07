import api from './api';
import type { ApiResponse, Profile } from '../types';

export interface ProfileRequest {
  jobRole: string;
  techStacks: string[];
  portfolioUrl?: string;
  interestedDomains: string[];
  availableHoursPerWeek?: number;
  collaborationStyle?: string;
  improvementGoal?: string;
  preferredDifficulty?: string;
  introduction?: string;
}

export const profileService = {
  getMyProfile: async (): Promise<Profile | null> => {
    const response = await api.get<ApiResponse<Profile | null>>('/users/me/profile');
    return response.data.data;
  },

  saveProfile: async (data: ProfileRequest): Promise<Profile> => {
    const response = await api.put<ApiResponse<Profile>>('/users/me/profile', data);
    return response.data.data;
  },

  getProfile: async (profileId: number): Promise<Profile> => {
    const response = await api.get<ApiResponse<Profile>>(`/profiles/${profileId}`);
    return response.data.data;
  },
};
