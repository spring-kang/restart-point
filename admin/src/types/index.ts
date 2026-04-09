// API Response
export interface ApiResponse<T> {
  success: boolean;
  data: T;
  message: string | null;
  errorCode?: string;
}

// Pagination
export interface Page<T> {
  content: T[];
  totalPages: number;
  totalElements: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
  empty: boolean;
}

// User & Auth
export interface User {
  id: number;
  email: string;
  name: string;
  role: UserRole;
  emailVerified: boolean;
  certificationStatus: CertificationStatus;
  bootcampName?: string;
  bootcampGeneration?: string;
  graduationDate?: string;
  createdAt: string;
  updatedAt: string;
}

export type UserRole = 'USER' | 'ADMIN';
export type CertificationStatus = 'NONE' | 'PENDING' | 'APPROVED' | 'REJECTED';

export interface LoginRequest {
  email: string;
  password: string;
}

export interface LoginResponse {
  accessToken: string;
  user: User;
}

// Season
export interface Season {
  id: number;
  title: string;
  description?: string;
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
  currentPhase?: string;
  canJoin?: boolean;
  createdAt: string;
  updatedAt: string;
}

export type SeasonStatus =
  | 'DRAFT'
  | 'RECRUITING'
  | 'TEAM_BUILDING'
  | 'IN_PROGRESS'
  | 'REVIEWING'
  | 'COMPLETED';

export interface SeasonCreateRequest {
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
  expertReviewWeight: number;
  candidateReviewWeight: number;
}

export const SEASON_STATUS_LABELS: Record<SeasonStatus, string> = {
  DRAFT: '초안',
  RECRUITING: '모집 중',
  TEAM_BUILDING: '팀빌딩',
  IN_PROGRESS: '진행 중',
  REVIEWING: '심사 중',
  COMPLETED: '완료',
};

export const SEASON_STATUS_COLORS: Record<SeasonStatus, string> = {
  DRAFT: 'bg-gray-100 text-gray-700',
  RECRUITING: 'bg-blue-100 text-blue-700',
  TEAM_BUILDING: 'bg-purple-100 text-purple-700',
  IN_PROGRESS: 'bg-green-100 text-green-700',
  REVIEWING: 'bg-orange-100 text-orange-700',
  COMPLETED: 'bg-gray-100 text-gray-700',
};

export const CERTIFICATION_STATUS_LABELS: Record<CertificationStatus, string> = {
  NONE: '미신청',
  PENDING: '대기 중',
  APPROVED: '승인됨',
  REJECTED: '거절됨',
};

export const CERTIFICATION_STATUS_COLORS: Record<CertificationStatus, string> = {
  NONE: 'bg-gray-100 text-gray-700',
  PENDING: 'bg-yellow-100 text-yellow-700',
  APPROVED: 'bg-green-100 text-green-700',
  REJECTED: 'bg-red-100 text-red-700',
};

export const USER_ROLE_LABELS: Record<UserRole, string> = {
  USER: '일반 사용자',
  ADMIN: '관리자',
};

export const USER_ROLE_COLORS: Record<UserRole, string> = {
  USER: 'bg-gray-100 text-gray-700',
  ADMIN: 'bg-purple-100 text-purple-700',
};
