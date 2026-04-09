// API Response
export interface ApiResponse<T> {
  success: boolean;
  data: T;
  message: string | null;
  errorCode?: string;
}

// User & Auth
export interface User {
  id: number;
  email: string;
  name: string;
  role: UserRole;
  certificationStatus: CertificationStatus;
  profileImageUrl?: string;
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
  name: string;
  description?: string;
  status: SeasonStatus;
  recruitmentStartDate: string;
  recruitmentEndDate: string;
  teamBuildingStartDate: string;
  teamBuildingEndDate: string;
  projectStartDate: string;
  projectEndDate: string;
  submissionDeadline: string;
  judgingStartDate: string;
  judgingEndDate: string;
  minTeamSize: number;
  maxTeamSize: number;
  expertJudgeWeight: number;
  peerJudgeWeight: number;
  createdAt: string;
  updatedAt: string;
}

export type SeasonStatus =
  | 'DRAFT'
  | 'RECRUITING'
  | 'TEAM_BUILDING'
  | 'IN_PROGRESS'
  | 'SUBMISSION'
  | 'JUDGING'
  | 'COMPLETED';

export interface SeasonCreateRequest {
  name: string;
  description?: string;
  recruitmentStartDate: string;
  recruitmentEndDate: string;
  teamBuildingStartDate: string;
  teamBuildingEndDate: string;
  projectStartDate: string;
  projectEndDate: string;
  submissionDeadline: string;
  judgingStartDate: string;
  judgingEndDate: string;
  minTeamSize: number;
  maxTeamSize: number;
  expertJudgeWeight: number;
  peerJudgeWeight: number;
}

export interface SeasonUpdateRequest extends SeasonCreateRequest {
  id: number;
}

// Certification
export interface CertificationRequest {
  id: number;
  userId: number;
  userName: string;
  userEmail: string;
  bootcampName: string;
  cohort: string;
  completionDate: string;
  jobRole: JobRole;
  certificateImageUrl?: string;
  status: CertificationStatus;
  requestedAt: string;
  reviewedAt?: string;
  rejectionReason?: string;
}

export type JobRole = 'PLANNER' | 'DESIGNER' | 'FRONTEND' | 'BACKEND';

export const JOB_ROLE_LABELS: Record<JobRole, string> = {
  PLANNER: '기획',
  DESIGNER: 'UX/UI',
  FRONTEND: '프론트엔드',
  BACKEND: '백엔드',
};

export const SEASON_STATUS_LABELS: Record<SeasonStatus, string> = {
  DRAFT: '초안',
  RECRUITING: '모집 중',
  TEAM_BUILDING: '팀빌딩',
  IN_PROGRESS: '진행 중',
  SUBMISSION: '제출 기간',
  JUDGING: '심사 중',
  COMPLETED: '완료',
};

export const SEASON_STATUS_COLORS: Record<SeasonStatus, string> = {
  DRAFT: 'bg-gray-100 text-gray-700',
  RECRUITING: 'bg-blue-100 text-blue-700',
  TEAM_BUILDING: 'bg-purple-100 text-purple-700',
  IN_PROGRESS: 'bg-green-100 text-green-700',
  SUBMISSION: 'bg-yellow-100 text-yellow-700',
  JUDGING: 'bg-orange-100 text-orange-700',
  COMPLETED: 'bg-gray-100 text-gray-700',
};

export const CERTIFICATION_STATUS_LABELS: Record<CertificationStatus, string> = {
  NONE: '미신청',
  PENDING: '대기 중',
  APPROVED: '승인됨',
  REJECTED: '거절됨',
};
