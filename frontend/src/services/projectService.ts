import api from './api';
import type {
  Project,
  Checkpoint,
  ProjectCreateRequest,
  ProjectUpdateRequest,
  ProjectSubmitRequest,
  CheckpointCreateRequest,
  CheckpointUpdateRequest,
  ProjectStatus
} from '../types';

export const projectService = {
  // 프로젝트 생성
  async createProject(request: ProjectCreateRequest): Promise<Project> {
    const response = await api.post('/projects', request);
    return response.data.data;
  },

  // 프로젝트 상세 조회
  async getProject(projectId: number): Promise<Project> {
    const response = await api.get(`/projects/${projectId}`);
    return response.data.data;
  },

  // 팀의 프로젝트 조회
  async getProjectByTeam(teamId: number): Promise<Project> {
    const response = await api.get(`/teams/${teamId}/project`);
    return response.data.data;
  },

  // 시즌별 프로젝트 목록 조회
  async getProjectsBySeason(seasonId: number, status?: ProjectStatus): Promise<Project[]> {
    const params = status ? { status } : {};
    const response = await api.get(`/seasons/${seasonId}/projects`, { params });
    return response.data.data.content || response.data.data;
  },

  // 프로젝트 수정
  async updateProject(projectId: number, request: ProjectUpdateRequest): Promise<Project> {
    const response = await api.put(`/projects/${projectId}`, request);
    return response.data.data;
  },

  // 프로젝트 시작
  async startProject(projectId: number): Promise<Project> {
    const response = await api.post(`/projects/${projectId}/start`);
    return response.data.data;
  },

  // 프로젝트 제출
  async submitProject(projectId: number, request: ProjectSubmitRequest): Promise<Project> {
    const response = await api.post(`/projects/${projectId}/submit`, request);
    return response.data.data;
  },

  // 체크포인트 생성
  async createCheckpoint(projectId: number, request: CheckpointCreateRequest): Promise<Checkpoint> {
    const response = await api.post(`/projects/${projectId}/checkpoints`, request);
    return response.data.data;
  },

  // 체크포인트 목록 조회
  async getCheckpoints(projectId: number): Promise<Checkpoint[]> {
    const response = await api.get(`/projects/${projectId}/checkpoints`);
    return response.data.data;
  },

  // 체크포인트 상세 조회
  async getCheckpoint(checkpointId: number): Promise<Checkpoint> {
    const response = await api.get(`/checkpoints/${checkpointId}`);
    return response.data.data;
  },

  // 체크포인트 수정
  async updateCheckpoint(checkpointId: number, request: CheckpointUpdateRequest): Promise<Checkpoint> {
    const response = await api.put(`/checkpoints/${checkpointId}`, request);
    return response.data.data;
  },

  // 체크포인트 삭제
  async deleteCheckpoint(checkpointId: number): Promise<void> {
    await api.delete(`/checkpoints/${checkpointId}`);
  },
};

// 프로젝트 상태 라벨
export const PROJECT_STATUS_LABELS: Record<ProjectStatus, string> = {
  DRAFT: '초안',
  IN_PROGRESS: '진행 중',
  SUBMITTED: '제출 완료',
  COMPLETED: '심사 완료',
};

// 프로젝트 상태 색상
export const PROJECT_STATUS_COLORS: Record<ProjectStatus, string> = {
  DRAFT: 'bg-gray-100 text-gray-700',
  IN_PROGRESS: 'bg-blue-100 text-blue-700',
  SUBMITTED: 'bg-green-100 text-green-700',
  COMPLETED: 'bg-purple-100 text-purple-700',
};
