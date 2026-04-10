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

// Review Analysis Types
export type RubricItem =
  | 'PROBLEM_DEFINITION'
  | 'USER_VALUE'
  | 'AI_USAGE'
  | 'UX_COMPLETENESS'
  | 'TECHNICAL_FEASIBILITY'
  | 'COLLABORATION';

export interface RubricAnalysis {
  rubricItem: RubricItem;
  label: string;
  averageScore: number;
  expertAverageScore: number;
  candidateAverageScore: number;
  scoreDifference: number;
  aiInsight: string;
}

export interface OutlierScore {
  reviewId: number;
  reviewerName: string;
  reviewType: 'EXPERT' | 'CANDIDATE';
  rubricItem: RubricItem;
  score: number;
  averageScore: number;
  deviation: number;
  possibleReason: string;
}

export interface ReviewAnalysis {
  projectId: number;
  projectName: string;
  teamName: string;
  totalReviewCount: number;
  expertReviewCount: number;
  candidateReviewCount: number;
  overallAverageScore: number;
  expertAverageScore: number;
  candidateAverageScore: number;
  scoreDifference: number;
  rubricAnalyses: RubricAnalysis[];
  commentSummary: string;
  strengths: string[];
  weaknesses: string[];
  outliers: OutlierScore[];
  expertVsCandidateAnalysis: string;
}

export const RUBRIC_ITEM_LABELS: Record<RubricItem, string> = {
  PROBLEM_DEFINITION: '문제 정의의 명확성',
  USER_VALUE: '사용자 가치',
  AI_USAGE: 'AI 활용 적절성',
  UX_COMPLETENESS: 'UX 완성도',
  TECHNICAL_FEASIBILITY: '기술 구현 가능성',
  COLLABORATION: '협업 완성도',
};

// Dashboard Types
export type JobRole = 'PLANNER' | 'UXUI' | 'FRONTEND' | 'BACKEND';

export const JOB_ROLE_LABELS: Record<JobRole, string> = {
  PLANNER: '기획자',
  UXUI: 'UX/UI',
  FRONTEND: '프론트엔드',
  BACKEND: '백엔드',
};

export interface ParticipantStats {
  totalParticipants: number;
  certifiedParticipants: number;
  pendingCertifications: number;
  roleDistribution: Record<JobRole, number>;
}

export interface IncompleteTeam {
  teamId: number;
  teamName: string;
  currentMembers: number;
  requiredMembers: number;
  missingRoles: JobRole[];
}

export interface TeamStats {
  totalTeams: number;
  completeTeams: number;
  incompleteTeams: number;
  recruitingTeams: number;
  incompleteTeamList: IncompleteTeam[];
}

export interface ProjectStats {
  totalProjects: number;
  submittedProjects: number;
  inProgressProjects: number;
  submissionRate: number;
  checkpointMissingCount: number;
}

export interface ScoreDistribution {
  excellent: number;
  good: number;
  average: number;
  belowAverage: number;
}

export interface ReviewStats {
  totalReviews: number;
  completedReviews: number;
  pendingReviews: number;
  reviewCompletionRate: number;
  averageScore: number;
  scoreDistribution: ScoreDistribution;
}

export interface ReportStats {
  totalReports: number;
  generatedReports: number;
  pendingReports: number;
  generationRate: number;
}

export interface RiskTeam {
  teamId: number;
  teamName: string;
  projectName: string;
  riskType: 'INCOMPLETE_TEAM' | 'CHECKPOINT_MISSING' | 'SUBMISSION_DELAYED';
  riskDescription: string;
  riskLevel: number;
}

export interface SeasonDashboard {
  seasonId: number;
  seasonTitle: string;
  seasonStatus: string;
  participantStats: ParticipantStats;
  teamStats: TeamStats;
  projectStats: ProjectStats;
  reviewStats: ReviewStats;
  reportStats: ReportStats;
  riskTeams: RiskTeam[];
}

export interface OverallDashboard {
  pendingCertifications: number;
  activeSeasonCount: number;
  activeSeasons: Array<{
    id: number;
    title: string;
    status: string;
  }>;
}

export const RISK_TYPE_LABELS: Record<string, string> = {
  INCOMPLETE_TEAM: '팀 미완성',
  CHECKPOINT_MISSING: '체크포인트 미제출',
  SUBMISSION_DELAYED: '프로젝트 미제출',
};

export const RISK_LEVEL_COLORS: Record<number, string> = {
  1: 'bg-yellow-100 text-yellow-700',
  2: 'bg-orange-100 text-orange-700',
  3: 'bg-red-100 text-red-700',
};
