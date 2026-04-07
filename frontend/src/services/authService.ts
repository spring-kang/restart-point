import api from './api';
import type { ApiResponse, AuthResponse, LoginRequest, SignupRequest, User, CertificationRequest } from '../types';

export const authService = {
  signup: async (data: SignupRequest): Promise<AuthResponse> => {
    const response = await api.post<ApiResponse<AuthResponse>>('/auth/signup', data);
    return response.data.data;
  },

  login: async (data: LoginRequest): Promise<AuthResponse> => {
    const response = await api.post<ApiResponse<AuthResponse>>('/auth/login', data);
    return response.data.data;
  },

  getMe: async (): Promise<User> => {
    const response = await api.get<ApiResponse<User>>('/users/me');
    return response.data.data;
  },

  requestCertification: async (data: CertificationRequest): Promise<User> => {
    const response = await api.post<ApiResponse<User>>('/users/me/certification', data);
    return response.data.data;
  },
};
