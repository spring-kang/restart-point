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
  seasonTitle?: string;
  leaderId: number;
  leaderName?: string;
  leaderEmail?: string;
  status: TeamStatus;
  recruitingPlanner: boolean;
  recruitingUxui: boolean;
  recruitingFrontend: boolean;
  recruitingBackend: boolean;
  memberCount: number;
  maxMemberCount: number;
  members?: TeamMember[];
  createdAt: string;
  updatedAt?: string;
}

export interface TeamMember {
  id: number;
  teamId?: number;
  userId: number;
  userName: string;
  userEmail?: string;
  role: JobRole;
  status: TeamMemberStatus;
  applicationMessage?: string;
  createdAt: string;
}

// 팀 요청 타입
export interface TeamRequest {
  name: string;
  description?: string;
  seasonId: number;
  leaderRole: JobRole;
  recruitingPlanner?: boolean;
  recruitingUxui?: boolean;
  recruitingFrontend?: boolean;
  recruitingBackend?: boolean;
}

// 팀 지원 요청 타입
export interface TeamApplyRequest {
  role: JobRole;
  applicationMessage?: string;
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

// AI 매칭 관련 타입
export type ScheduleRisk = 'LOW' | 'MEDIUM' | 'HIGH';

export interface TeamRecommendation {
  team: Team;
  matchScore: number;
  reasons: string[];
  balanceAnalysis: string;
  scheduleRisk: ScheduleRisk;
  missingRoles: string[];
}

export interface MemberRecommendation {
  profile: Profile & { userName?: string };
  matchScore: number;
  reasons: string[];
  balanceAnalysis: string;
  scheduleRisk: ScheduleRisk;
  complementarySkills: string[];
}

// 프로젝트 관련 타입
export type ProjectStatus = 'DRAFT' | 'IN_PROGRESS' | 'SUBMITTED' | 'COMPLETED';

export interface Project {
  id: number;
  teamId: number;
  teamName: string;
  name: string;
  problemDefinition?: string;
  targetUsers?: string;
  solution?: string;
  aiUsage?: string;
  figmaUrl?: string;
  githubUrl?: string;
  notionUrl?: string;
  demoUrl?: string;
  status: ProjectStatus;
  teamRetrospective?: string;
  checkpoints?: Checkpoint[];
  createdAt: string;
  updatedAt?: string;
}

export interface Checkpoint {
  id: number;
  projectId: number;
  weekNumber: number;
  weeklyGoal?: string;
  progressSummary?: string;
  blockers?: string;
  nextWeekPlan?: string;
  createdById?: number;
  createdByName?: string;
  aiFeedback?: string;
  memberProgresses?: MemberProgress[];
  createdAt: string;
  updatedAt?: string;
}

export interface MemberProgress {
  id: number;
  userId: number;
  userName: string;
  jobRole: JobRole;
  completedTasks?: string;
  inProgressTasks?: string;
  personalBlockers?: string;
  contributionPercentage?: number;
}

// 프로젝트 요청 타입
export interface ProjectCreateRequest {
  teamId: number;
  name: string;
  problemDefinition?: string;
  targetUsers?: string;
  solution?: string;
  aiUsage?: string;
  figmaUrl?: string;
  githubUrl?: string;
  notionUrl?: string;
  demoUrl?: string;
}

export interface ProjectUpdateRequest {
  name: string;
  problemDefinition?: string;
  targetUsers?: string;
  solution?: string;
  aiUsage?: string;
  figmaUrl?: string;
  githubUrl?: string;
  notionUrl?: string;
  demoUrl?: string;
}

export interface ProjectSubmitRequest {
  teamRetrospective: string;
}

export interface CheckpointCreateRequest {
  weekNumber: number;
  weeklyGoal?: string;
  progressSummary?: string;
  blockers?: string;
  nextWeekPlan?: string;
  memberProgresses?: MemberProgressRequest[];
}

export interface CheckpointUpdateRequest {
  weeklyGoal?: string;
  progressSummary?: string;
  blockers?: string;
  nextWeekPlan?: string;
  memberProgresses?: MemberProgressRequest[];
}

export interface MemberProgressRequest {
  userId: number;
  jobRole: JobRole;
  completedTasks?: string;
  inProgressTasks?: string;
  personalBlockers?: string;
  contributionPercentage?: number;
}

// 심사 관련 타입
export type ReviewType = 'EXPERT' | 'CANDIDATE';

export type RubricItem =
  | 'PROBLEM_DEFINITION'
  | 'USER_VALUE'
  | 'AI_USAGE'
  | 'UX_COMPLETENESS'
  | 'TECHNICAL_FEASIBILITY'
  | 'COLLABORATION';

export interface RubricItemInfo {
  item: RubricItem;
  label: string;
  description: string;
}

export interface ReviewScore {
  id?: number;
  rubricItem: RubricItem;
  rubricLabel?: string;
  rubricDescription?: string;
  score: number;
  comment?: string;
}

export interface Review {
  id: number;
  projectId: number;
  projectName: string;
  reviewerId: number;
  reviewerName: string;
  reviewType: ReviewType;
  overallComment?: string;
  averageScore: number;
  totalScore: number;
  scores?: ReviewScore[];
  submittedAt: string;
}

export interface ReviewSummary {
  projectId: number;
  projectName: string;
  totalReviewCount: number;
  expertReviewCount: number;
  candidateReviewCount: number;
  weightedAverageScore: number;
  expertAverageScore: number;
  candidateAverageScore: number;
  rubricAverages: Record<RubricItem, number>;
  expertRubricAverages: Record<RubricItem, number>;
  candidateRubricAverages: Record<RubricItem, number>;
}

// 심사 요청 타입
export interface ReviewScoreRequest {
  rubricItem: RubricItem;
  score: number;
  comment?: string;
}

export interface ReviewCreateRequest {
  scores: ReviewScoreRequest[];
  overallComment?: string;
}

// 성장 리포트 관련 타입
export type ReportType = 'TEAM' | 'INDIVIDUAL';

export interface GrowthReport {
  id: number;
  projectId: number;
  projectName: string;
  teamName: string;
  userId?: number;
  userName?: string;
  userRole?: string;
  reportType: ReportType;
  teamStrengths?: string;
  teamImprovements?: string;
  roleSpecificFeedback?: string;
  nextProjectActions?: string;
  portfolioImprovements?: string;
  recommendedAreas?: string;
  averageScore?: number;
  rubricScoreSummary?: string;
  generated: boolean;
  createdAt: string;
}

// 심사 가이드 관련 타입
export interface ReviewGuideStatus {
  rubricLearningCompleted: boolean;
  exampleComparisonCompleted: boolean;
  practiceEvaluationCompleted: boolean;
  fullyCompleted: boolean;
}

export interface ScoreExample {
  score: number;
  description: string;
  example: string;
}

export interface RubricGuide {
  rubricItem: RubricItem;
  label: string;
  description: string;
  evaluationTips: string;
  scoreExamples: ScoreExample[];
}

export interface ExampleProject {
  name: string;
  problemDefinition: string;
  solution: string;
  aiUsage: string;
  expectedScore: number;
  reasonForScore: string;
}

export interface ExampleComparison {
  category: string;
  excellentExample: ExampleProject;
  averageExample: ExampleProject;
  comparisonNotes: string;
}

export interface ReviewGuide {
  rubricGuides: RubricGuide[];
  exampleComparisons: ExampleComparison[];
  completionStatus: ReviewGuideStatus;
}

// 심사 패턴 분석 관련 타입
export interface ScoreDistribution {
  score1Count: number;
  score2Count: number;
  score3Count: number;
  score4Count: number;
  score5Count: number;
}

export interface ReviewPatternAnalysis {
  totalReviewCount: number;
  averageScore: number;
  rubricAverages: Record<RubricItem, number>;
  overallTendency: string;
  strengths: string;
  areasForImprovement: string;
  comparisonWithExperts: string;
  recommendations: string;
  scoreDistribution: ScoreDistribution;
}

// AI 심사 분석 관련 타입 (운영자용)
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

// 커뮤니티 관련 타입
export type PostType = 'RECRUITMENT' | 'ANNOUNCEMENT' | 'SHOWCASE' | 'QNA';

export interface PostAuthor {
  id: number;
  name: string;
}

export interface PostSeason {
  id: number;
  title: string;
}

export interface PostProject {
  id: number;
  name: string;
  teamId: number;
}

export interface Post {
  id: number;
  postType: PostType;
  title: string;
  content: string;
  author: PostAuthor;
  season?: PostSeason;
  project?: PostProject;
  viewCount: number;
  likeCount: number;
  commentCount: number;
  pinned: boolean;
  liked: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface PostListItem {
  id: number;
  postType: PostType;
  title: string;
  author: PostAuthor;
  viewCount: number;
  likeCount: number;
  commentCount: number;
  pinned: boolean;
  createdAt: string;
}

export interface PostCreateRequest {
  postType: PostType;
  title: string;
  content: string;
  seasonId?: number;
  projectId?: number;
}

export interface PostUpdateRequest {
  title: string;
  content: string;
}

export interface CommentAuthor {
  id: number;
  name: string;
}

export interface Comment {
  id: number;
  content: string;
  author: CommentAuthor;
  parentId?: number;
  replies?: Comment[];
  deleted: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface CommentCreateRequest {
  content: string;
  parentId?: number;
}

export interface CommentUpdateRequest {
  content: string;
}

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
