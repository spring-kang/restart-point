// 사용자 관련 타입
export type Role = 'USER' | 'REVIEWER' | 'ADMIN';
export type CertificationStatus = 'NONE' | 'PENDING' | 'APPROVED' | 'REJECTED';

export interface User {
  id: number;
  email: string;
  name: string;
  role: Role;
  emailVerified: boolean;
  certificationStatus: CertificationStatus;
  bootcampName?: string;
  bootcampGeneration?: string;
  graduationDate?: string;
  certificateUrl?: string;
  createdAt: string;
  updatedAt: string;
}

// 프로필 관련 타입
export type JobRole = 'PLANNER' | 'UXUI' | 'FRONTEND' | 'BACKEND';
export type CollaborationStyle = 'LEADER' | 'FOLLOWER' | 'COLLABORATIVE' | 'INDEPENDENT';
export type ProjectDifficulty = 'BEGINNER' | 'INTERMEDIATE' | 'ADVANCED';

export interface Profile {
  id: number;
  userId: number;
  jobRole: JobRole;
  techStacks: string[];
  portfolioUrl?: string;
  interestedDomains: string[];
  availableHoursPerWeek?: number;
  collaborationStyle?: CollaborationStyle;
  improvementGoal?: string;
  preferredDifficulty?: ProjectDifficulty;
  introduction?: string;
  createdAt: string;
  updatedAt: string;
}

// 시즌 관련 타입
export type SeasonStatus = 'DRAFT' | 'RECRUITING' | 'TEAM_BUILDING' | 'IN_PROGRESS' | 'REVIEWING' | 'COMPLETED';

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
  createdAt: string;
  updatedAt: string;
}

// 팀 관련 타입
export type TeamStatus = 'RECRUITING' | 'COMPLETE' | 'IN_PROGRESS' | 'SUBMITTED' | 'REVIEWED';
export type TeamMemberStatus = 'PENDING' | 'ACCEPTED' | 'REJECTED';

export interface Team {
  id: number;
  name: string;
  description?: string;
  seasonId: number;
  leaderId: number;
  status: TeamStatus;
  recruitingPlanner: boolean;
  recruitingUxui: boolean;
  recruitingFrontend: boolean;
  recruitingBackend: boolean;
  memberCount: number;
  createdAt: string;
  updatedAt: string;
}

export interface TeamMember {
  id: number;
  teamId: number;
  userId: number;
  userName: string;
  role: JobRole;
  status: TeamMemberStatus;
  applicationMessage?: string;
  createdAt: string;
}

// API 응답 타입
export interface ApiResponse<T> {
  success: boolean;
  data: T;
  message?: string;
  errorCode?: string;
}

// 인증 관련 타입
export interface LoginRequest {
  email: string;
  password: string;
}

export interface SignupRequest {
  email: string;
  password: string;
  name: string;
  signupToken: string;
}

export interface AuthResponse {
  accessToken: string;
  user: User;
}

export interface EmailVerificationResponse {
  signupToken: string;
}

// 수료 인증 요청 타입
export interface CertificationRequest {
  bootcampName: string;
  bootcampGeneration: string;
  graduationDate: string;
  certificateUrl: string;
}
