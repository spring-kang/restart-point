import api from './api';
import type {
  ApiResponse,
  AuthResponse,
  LoginRequest,
  SignupRequest,
  User,
  CertificationRequest,
  EmailVerificationResponse,
} from '../types';

export interface EmailVerificationRequest {
  email: string;
}

export interface EmailVerificationConfirmRequest {
  email: string;
  code: string;
}

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

  // 이메일 인증 코드 발송
  sendVerificationCode: async (email: string): Promise<void> => {
    await api.post<ApiResponse<void>>('/auth/email/send', { email });
  },

  // 이메일 인증 코드 확인
  verifyEmail: async (data: EmailVerificationConfirmRequest): Promise<EmailVerificationResponse> => {
    const response = await api.post<ApiResponse<EmailVerificationResponse>>('/auth/email/verify', data);
    return response.data.data;
  },

  // 이메일 인증 코드 재발송
  resendVerificationCode: async (email: string): Promise<void> => {
    await api.post<ApiResponse<void>>('/auth/email/resend', { email });
  },
};
