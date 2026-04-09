import { useState, useEffect } from 'react';
import { useParams } from 'react-router-dom';
import { projectService, PROJECT_STATUS_LABELS, PROJECT_STATUS_COLORS } from '../services/projectService';
import { teamService, JOB_ROLE_LABELS } from '../services/teamService';
import type { Project, TeamMember, CheckpointCreateRequest, ProjectUpdateRequest, Checkpoint } from '../types';

export default function ProjectWorkspace() {
  const { teamId } = useParams<{ teamId: string }>();
  const [project, setProject] = useState<Project | null>(null);
  const [teamMembers, setTeamMembers] = useState<TeamMember[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [isEditing, setIsEditing] = useState(false);
  const [showCheckpointModal, setShowCheckpointModal] = useState(false);
  const [regeneratingFeedback, setRegeneratingFeedback] = useState<number | null>(null);

  // 편집 폼 상태
  const [editForm, setEditForm] = useState<ProjectUpdateRequest>({
    name: '',
    problemDefinition: '',
    targetUsers: '',
    solution: '',
    aiUsage: '',
    figmaUrl: '',
    githubUrl: '',
    notionUrl: '',
    demoUrl: '',
  });

  // 체크포인트 폼 상태
  const [checkpointForm, setCheckpointForm] = useState<CheckpointCreateRequest>({
    weekNumber: 1,
    weeklyGoal: '',
    progressSummary: '',
    blockers: '',
    nextWeekPlan: '',
  });

  useEffect(() => {
    loadData();
  }, [teamId]);

  const loadData = async () => {
    if (!teamId) return;

    setLoading(true);
    setError(null);

    try {
      // 팀 멤버 조회
      const members = await teamService.getTeamMembers(Number(teamId));
      setTeamMembers(members);

      // 프로젝트 조회 (없을 수도 있음)
      try {
        const projectData = await projectService.getProjectByTeam(Number(teamId));
        setProject(projectData);
        setEditForm({
          name: projectData.name || '',
          problemDefinition: projectData.problemDefinition || '',
          targetUsers: projectData.targetUsers || '',
          solution: projectData.solution || '',
          aiUsage: projectData.aiUsage || '',
          figmaUrl: projectData.figmaUrl || '',
          githubUrl: projectData.githubUrl || '',
          notionUrl: projectData.notionUrl || '',
          demoUrl: projectData.demoUrl || '',
        });
      } catch {
        // 프로젝트가 없는 경우
        setProject(null);
      }
    } catch (err: any) {
      setError(err.response?.data?.message || '데이터를 불러오는데 실패했습니다.');
    } finally {
      setLoading(false);
    }
  };

  const handleCreateProject = async () => {
    if (!teamId) return;

    try {
      const newProject = await projectService.createProject({
        teamId: Number(teamId),
        name: '새 프로젝트',
      });
      setProject(newProject);
      setIsEditing(true);
      setEditForm({
        name: newProject.name,
        problemDefinition: '',
        targetUsers: '',
        solution: '',
        aiUsage: '',
        figmaUrl: '',
        githubUrl: '',
        notionUrl: '',
        demoUrl: '',
      });
    } catch (err: any) {
      setError(err.response?.data?.message || '프로젝트 생성에 실패했습니다.');
    }
  };

  const handleUpdateProject = async () => {
    if (!project) return;

    try {
      const updated = await projectService.updateProject(project.id, editForm);
      setProject(updated);
      setIsEditing(false);
    } catch (err: any) {
      setError(err.response?.data?.message || '프로젝트 수정에 실패했습니다.');
    }
  };

  const handleStartProject = async () => {
    if (!project) return;

    try {
      const updated = await projectService.startProject(project.id);
      setProject(updated);
    } catch (err: any) {
      setError(err.response?.data?.message || '프로젝트 시작에 실패했습니다.');
    }
  };

  const handleCreateCheckpoint = async () => {
    if (!project) return;

    try {
      const checkpoint = await projectService.createCheckpoint(project.id, checkpointForm);
      setProject({
        ...project,
        checkpoints: [...(project.checkpoints || []), checkpoint],
      });
      setShowCheckpointModal(false);
      setCheckpointForm({
        weekNumber: (project.checkpoints?.length || 0) + 2,
        weeklyGoal: '',
        progressSummary: '',
        blockers: '',
        nextWeekPlan: '',
      });
    } catch (err: any) {
      setError(err.response?.data?.message || '체크포인트 생성에 실패했습니다.');
    }
  };

  const openNewCheckpointModal = () => {
    const nextWeek = (project?.checkpoints?.length || 0) + 1;
    setCheckpointForm({
      weekNumber: nextWeek,
      weeklyGoal: '',
      progressSummary: '',
      blockers: '',
      nextWeekPlan: '',
    });
    setShowCheckpointModal(true);
  };

  const handleRegenerateAiFeedback = async (checkpointId: number) => {
    if (!project) return;

    setRegeneratingFeedback(checkpointId);
    try {
      const updatedCheckpoint = await projectService.regenerateAiFeedback(checkpointId);
      setProject({
        ...project,
        checkpoints: project.checkpoints?.map((cp: Checkpoint) =>
          cp.id === checkpointId ? updatedCheckpoint : cp
        ),
      });
    } catch (err: any) {
      setError(err.response?.data?.message || 'AI 피드백 재생성에 실패했습니다.');
    } finally {
      setRegeneratingFeedback(null);
    }
  };

  if (loading) {
    return (
      <div className="flex justify-center items-center min-h-screen">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-sky-600"></div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="max-w-4xl mx-auto p-6">
        <div className="bg-red-50 text-red-700 p-4 rounded-lg">{error}</div>
      </div>
    );
  }

  // 프로젝트가 없는 경우 생성 버튼 표시
  if (!project) {
    return (
      <div className="max-w-4xl mx-auto p-6">
        <div className="bg-white rounded-lg shadow p-8 text-center">
          <h2 className="text-2xl font-bold text-gray-900 mb-4">프로젝트 워크스페이스</h2>
          <p className="text-gray-600 mb-6">
            아직 프로젝트가 생성되지 않았습니다. 프로젝트를 시작하려면 아래 버튼을 클릭하세요.
          </p>
          <button
            onClick={handleCreateProject}
            className="px-6 py-3 bg-sky-600 text-white rounded-lg hover:bg-sky-700 transition"
          >
            프로젝트 시작하기
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="max-w-6xl mx-auto p-6 space-y-6">
      {/* 헤더 */}
      <div className="bg-white rounded-lg shadow p-6">
        <div className="flex justify-between items-start mb-4">
          <div>
            <div className="flex items-center gap-3 mb-2">
              <h1 className="text-2xl font-bold text-gray-900">
                {isEditing ? (
                  <input
                    type="text"
                    value={editForm.name}
                    onChange={(e) => setEditForm({ ...editForm, name: e.target.value })}
                    className="border border-gray-300 rounded px-3 py-1 text-2xl font-bold"
                  />
                ) : (
                  project.name
                )}
              </h1>
              <span className={`px-3 py-1 rounded-full text-sm font-medium ${PROJECT_STATUS_COLORS[project.status]}`}>
                {PROJECT_STATUS_LABELS[project.status]}
              </span>
            </div>
            <p className="text-gray-600">팀: {project.teamName}</p>
          </div>
          <div className="flex gap-2">
            {project.status === 'DRAFT' && !isEditing && (
              <button
                onClick={handleStartProject}
                className="px-4 py-2 bg-green-600 text-white rounded-lg hover:bg-green-700 transition"
              >
                프로젝트 시작
              </button>
            )}
            {(project.status === 'DRAFT' || project.status === 'IN_PROGRESS') && (
              isEditing ? (
                <>
                  <button
                    onClick={handleUpdateProject}
                    className="px-4 py-2 bg-sky-600 text-white rounded-lg hover:bg-sky-700 transition"
                  >
                    저장
                  </button>
                  <button
                    onClick={() => setIsEditing(false)}
                    className="px-4 py-2 bg-gray-200 text-gray-700 rounded-lg hover:bg-gray-300 transition"
                  >
                    취소
                  </button>
                </>
              ) : (
                <button
                  onClick={() => setIsEditing(true)}
                  className="px-4 py-2 bg-gray-200 text-gray-700 rounded-lg hover:bg-gray-300 transition"
                >
                  수정
                </button>
              )
            )}
          </div>
        </div>

        {/* 외부 링크 */}
        <div className="flex flex-wrap gap-3 mt-4">
          {(isEditing || project.figmaUrl) && (
            isEditing ? (
              <input
                type="url"
                placeholder="Figma URL"
                value={editForm.figmaUrl}
                onChange={(e) => setEditForm({ ...editForm, figmaUrl: e.target.value })}
                className="border border-gray-300 rounded px-3 py-1 text-sm flex-1 min-w-[200px]"
              />
            ) : (
              <a href={project.figmaUrl} target="_blank" rel="noopener noreferrer"
                 className="inline-flex items-center gap-2 px-4 py-2 bg-purple-100 text-purple-700 rounded-lg hover:bg-purple-200 transition">
                Figma
              </a>
            )
          )}
          {(isEditing || project.githubUrl) && (
            isEditing ? (
              <input
                type="url"
                placeholder="GitHub URL"
                value={editForm.githubUrl}
                onChange={(e) => setEditForm({ ...editForm, githubUrl: e.target.value })}
                className="border border-gray-300 rounded px-3 py-1 text-sm flex-1 min-w-[200px]"
              />
            ) : (
              <a href={project.githubUrl} target="_blank" rel="noopener noreferrer"
                 className="inline-flex items-center gap-2 px-4 py-2 bg-gray-800 text-white rounded-lg hover:bg-gray-900 transition">
                GitHub
              </a>
            )
          )}
          {(isEditing || project.notionUrl) && (
            isEditing ? (
              <input
                type="url"
                placeholder="Notion URL"
                value={editForm.notionUrl}
                onChange={(e) => setEditForm({ ...editForm, notionUrl: e.target.value })}
                className="border border-gray-300 rounded px-3 py-1 text-sm flex-1 min-w-[200px]"
              />
            ) : (
              <a href={project.notionUrl} target="_blank" rel="noopener noreferrer"
                 className="inline-flex items-center gap-2 px-4 py-2 bg-gray-100 text-gray-700 rounded-lg hover:bg-gray-200 transition">
                Notion
              </a>
            )
          )}
          {(isEditing || project.demoUrl) && (
            isEditing ? (
              <input
                type="url"
                placeholder="Demo URL"
                value={editForm.demoUrl}
                onChange={(e) => setEditForm({ ...editForm, demoUrl: e.target.value })}
                className="border border-gray-300 rounded px-3 py-1 text-sm flex-1 min-w-[200px]"
              />
            ) : (
              <a href={project.demoUrl} target="_blank" rel="noopener noreferrer"
                 className="inline-flex items-center gap-2 px-4 py-2 bg-sky-100 text-sky-700 rounded-lg hover:bg-sky-200 transition">
                Demo
              </a>
            )
          )}
        </div>
      </div>

      {/* 프로젝트 상세 정보 */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        {/* 문제 정의 */}
        <div className="bg-white rounded-lg shadow p-6">
          <h3 className="text-lg font-semibold text-gray-900 mb-3">문제 정의</h3>
          {isEditing ? (
            <textarea
              value={editForm.problemDefinition}
              onChange={(e) => setEditForm({ ...editForm, problemDefinition: e.target.value })}
              className="w-full border border-gray-300 rounded-lg p-3 h-32 resize-none"
              placeholder="해결하고자 하는 문제를 정의해주세요"
            />
          ) : (
            <p className="text-gray-600 whitespace-pre-wrap">
              {project.problemDefinition || '아직 작성되지 않았습니다.'}
            </p>
          )}
        </div>

        {/* 타깃 사용자 */}
        <div className="bg-white rounded-lg shadow p-6">
          <h3 className="text-lg font-semibold text-gray-900 mb-3">타깃 사용자</h3>
          {isEditing ? (
            <textarea
              value={editForm.targetUsers}
              onChange={(e) => setEditForm({ ...editForm, targetUsers: e.target.value })}
              className="w-full border border-gray-300 rounded-lg p-3 h-32 resize-none"
              placeholder="주요 타깃 사용자를 정의해주세요"
            />
          ) : (
            <p className="text-gray-600 whitespace-pre-wrap">
              {project.targetUsers || '아직 작성되지 않았습니다.'}
            </p>
          )}
        </div>

        {/* 핵심 솔루션 */}
        <div className="bg-white rounded-lg shadow p-6">
          <h3 className="text-lg font-semibold text-gray-900 mb-3">핵심 솔루션</h3>
          {isEditing ? (
            <textarea
              value={editForm.solution}
              onChange={(e) => setEditForm({ ...editForm, solution: e.target.value })}
              className="w-full border border-gray-300 rounded-lg p-3 h-32 resize-none"
              placeholder="제안하는 솔루션을 설명해주세요"
            />
          ) : (
            <p className="text-gray-600 whitespace-pre-wrap">
              {project.solution || '아직 작성되지 않았습니다.'}
            </p>
          )}
        </div>

        {/* AI 활용 방식 */}
        <div className="bg-white rounded-lg shadow p-6">
          <h3 className="text-lg font-semibold text-gray-900 mb-3">AI 활용 방식</h3>
          {isEditing ? (
            <textarea
              value={editForm.aiUsage}
              onChange={(e) => setEditForm({ ...editForm, aiUsage: e.target.value })}
              className="w-full border border-gray-300 rounded-lg p-3 h-32 resize-none"
              placeholder="AI를 어떻게 활용하는지 설명해주세요"
            />
          ) : (
            <p className="text-gray-600 whitespace-pre-wrap">
              {project.aiUsage || '아직 작성되지 않았습니다.'}
            </p>
          )}
        </div>
      </div>

      {/* 팀 멤버 */}
      <div className="bg-white rounded-lg shadow p-6">
        <h3 className="text-lg font-semibold text-gray-900 mb-4">팀 멤버</h3>
        <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-4 gap-4">
          {teamMembers.map((member) => (
            <div key={member.id} className="border border-gray-200 rounded-lg p-4">
              <p className="font-medium text-gray-900">{member.userName}</p>
              <p className="text-sm text-gray-500">{JOB_ROLE_LABELS[member.role]}</p>
            </div>
          ))}
        </div>
      </div>

      {/* 체크포인트 */}
      <div className="bg-white rounded-lg shadow p-6">
        <div className="flex justify-between items-center mb-4">
          <h3 className="text-lg font-semibold text-gray-900">주차별 체크포인트</h3>
          {(project.status === 'DRAFT' || project.status === 'IN_PROGRESS') && (
            <button
              onClick={openNewCheckpointModal}
              className="px-4 py-2 bg-sky-600 text-white rounded-lg hover:bg-sky-700 transition"
            >
              체크포인트 추가
            </button>
          )}
        </div>

        {project.checkpoints && project.checkpoints.length > 0 ? (
          <div className="space-y-4">
            {project.checkpoints.map((checkpoint) => (
              <div key={checkpoint.id} className="border border-gray-200 rounded-lg p-4">
                <div className="flex justify-between items-start mb-3">
                  <h4 className="font-semibold text-gray-900">Week {checkpoint.weekNumber}</h4>
                  <span className="text-sm text-gray-500">
                    {new Date(checkpoint.createdAt).toLocaleDateString('ko-KR')}
                  </span>
                </div>

                {checkpoint.weeklyGoal && (
                  <div className="mb-3">
                    <p className="text-sm font-medium text-gray-700">이번 주 목표</p>
                    <p className="text-gray-600">{checkpoint.weeklyGoal}</p>
                  </div>
                )}

                {checkpoint.progressSummary && (
                  <div className="mb-3">
                    <p className="text-sm font-medium text-gray-700">진행 상황</p>
                    <p className="text-gray-600">{checkpoint.progressSummary}</p>
                  </div>
                )}

                {checkpoint.blockers && (
                  <div className="mb-3">
                    <p className="text-sm font-medium text-red-600">막힘 요소</p>
                    <p className="text-gray-600">{checkpoint.blockers}</p>
                  </div>
                )}

                {checkpoint.nextWeekPlan && (
                  <div className="mb-3">
                    <p className="text-sm font-medium text-gray-700">다음 주 계획</p>
                    <p className="text-gray-600">{checkpoint.nextWeekPlan}</p>
                  </div>
                )}

                {/* AI 프로젝트 코치 피드백 */}
                <div className="mt-4 bg-gradient-to-r from-sky-50 to-indigo-50 rounded-lg p-4 border border-sky-100">
                  <div className="flex justify-between items-start mb-3">
                    <div className="flex items-center gap-2">
                      <svg className="w-5 h-5 text-sky-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9.663 17h4.673M12 3v1m6.364 1.636l-.707.707M21 12h-1M4 12H3m3.343-5.657l-.707-.707m2.828 9.9a5 5 0 117.072 0l-.548.547A3.374 3.374 0 0014 18.469V19a2 2 0 11-4 0v-.531c0-.895-.356-1.754-.988-2.386l-.548-.547z" />
                      </svg>
                      <span className="text-sm font-semibold text-sky-700">AI 프로젝트 코치</span>
                    </div>
                    <button
                      onClick={() => handleRegenerateAiFeedback(checkpoint.id)}
                      disabled={regeneratingFeedback === checkpoint.id}
                      className="text-sm text-sky-600 hover:text-sky-800 disabled:opacity-50 disabled:cursor-not-allowed flex items-center gap-1"
                    >
                      {regeneratingFeedback === checkpoint.id ? (
                        <>
                          <svg className="animate-spin w-4 h-4" fill="none" viewBox="0 0 24 24">
                            <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                            <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                          </svg>
                          생성 중...
                        </>
                      ) : (
                        <>
                          <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
                          </svg>
                          재생성
                        </>
                      )}
                    </button>
                  </div>
                  {checkpoint.aiFeedback ? (
                    <div className="prose prose-sm max-w-none text-gray-700">
                      <div className="whitespace-pre-wrap text-sm leading-relaxed">{checkpoint.aiFeedback}</div>
                    </div>
                  ) : (
                    <p className="text-gray-500 text-sm italic">AI 피드백을 생성하는 중이거나 아직 생성되지 않았습니다.</p>
                  )}
                </div>
              </div>
            ))}
          </div>
        ) : (
          <p className="text-gray-500 text-center py-8">
            아직 등록된 체크포인트가 없습니다.
          </p>
        )}
      </div>

      {/* 체크포인트 모달 */}
      {showCheckpointModal && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg shadow-xl p-6 w-full max-w-lg mx-4">
            <h3 className="text-lg font-semibold text-gray-900 mb-4">
              체크포인트 추가
            </h3>

            <div className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">주차</label>
                <input
                  type="number"
                  min="1"
                  value={checkpointForm.weekNumber}
                  onChange={(e) => setCheckpointForm({ ...checkpointForm, weekNumber: Number(e.target.value) })}
                  className="w-full border border-gray-300 rounded-lg px-3 py-2"
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">이번 주 목표</label>
                <textarea
                  value={checkpointForm.weeklyGoal}
                  onChange={(e) => setCheckpointForm({ ...checkpointForm, weeklyGoal: e.target.value })}
                  className="w-full border border-gray-300 rounded-lg px-3 py-2 h-20 resize-none"
                  placeholder="이번 주에 달성하고자 하는 목표"
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">진행 상황</label>
                <textarea
                  value={checkpointForm.progressSummary}
                  onChange={(e) => setCheckpointForm({ ...checkpointForm, progressSummary: e.target.value })}
                  className="w-full border border-gray-300 rounded-lg px-3 py-2 h-20 resize-none"
                  placeholder="현재까지의 진행 상황 요약"
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">막힘 요소</label>
                <textarea
                  value={checkpointForm.blockers}
                  onChange={(e) => setCheckpointForm({ ...checkpointForm, blockers: e.target.value })}
                  className="w-full border border-gray-300 rounded-lg px-3 py-2 h-20 resize-none"
                  placeholder="현재 겪고 있는 어려움이나 장애물"
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">다음 주 계획</label>
                <textarea
                  value={checkpointForm.nextWeekPlan}
                  onChange={(e) => setCheckpointForm({ ...checkpointForm, nextWeekPlan: e.target.value })}
                  className="w-full border border-gray-300 rounded-lg px-3 py-2 h-20 resize-none"
                  placeholder="다음 주에 진행할 계획"
                />
              </div>
            </div>

            <div className="flex justify-end gap-3 mt-6">
              <button
                onClick={() => setShowCheckpointModal(false)}
                className="px-4 py-2 bg-gray-200 text-gray-700 rounded-lg hover:bg-gray-300 transition"
              >
                취소
              </button>
              <button
                onClick={handleCreateCheckpoint}
                className="px-4 py-2 bg-sky-600 text-white rounded-lg hover:bg-sky-700 transition"
              >
                저장
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
