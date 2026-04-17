import { useState, useEffect } from 'react';
import {
  GraduationCap,
  Plus,
  Edit2,
  Trash2,
  ChevronDown,
  ChevronRight,
  BookOpen,
  AlertCircle,
} from 'lucide-react';
import adminService from '../services/adminService';

type JobRole = 'PLANNER' | 'UXUI' | 'FRONTEND' | 'BACKEND';

const JOB_ROLE_LABELS: Record<JobRole, string> = {
  PLANNER: '기획자',
  UXUI: 'UX/UI 디자이너',
  FRONTEND: '프론트엔드',
  BACKEND: '백엔드',
};

interface JobRoleMentoring {
  id: number;
  seasonId: number;
  jobRole: JobRole;
  title: string;
  description: string;
  learningObjectives: string;
  active: boolean;
  moduleCount?: number;
}

interface MentoringModule {
  id: number;
  mentoringId: number;
  weekNumber: number;
  title: string;
  description: string;
  learningContent: string;
  keyPoints: string[];
  commonMistakes: string[];
  practiceTasks: string[];
  referenceMaterials: string[];
  estimatedMinutes: number;
}

interface Season {
  id: number;
  title: string;
  status: string;
}

export default function MentoringPage() {
  const [seasons, setSeasons] = useState<Season[]>([]);
  const [selectedSeasonId, setSelectedSeasonId] = useState<number | null>(null);
  const [mentorings, setMentorings] = useState<JobRoleMentoring[]>([]);
  const [expandedMentoring, setExpandedMentoring] = useState<number | null>(null);
  const [modules, setModules] = useState<Record<number, MentoringModule[]>>({});
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [moduleErrors, setModuleErrors] = useState<Record<number, string>>({});
  const [showMentoringModal, setShowMentoringModal] = useState(false);
  const [showModuleModal, setShowModuleModal] = useState(false);
  const [editingMentoring, setEditingMentoring] = useState<JobRoleMentoring | null>(null);
  const [editingModule, setEditingModule] = useState<MentoringModule | null>(null);
  const [selectedMentoringId, setSelectedMentoringId] = useState<number | null>(null);

  useEffect(() => {
    loadSeasons();
  }, []);

  useEffect(() => {
    if (selectedSeasonId) {
      loadMentorings(selectedSeasonId);
    }
  }, [selectedSeasonId]);

  const loadSeasons = async () => {
    try {
      const data = await adminService.getSeasons();
      setSeasons(data.content);
      if (data.content.length > 0) {
        setSelectedSeasonId(data.content[0].id);
      }
    } catch (err) {
      console.error('Failed to load seasons:', err);
      setError('시즌 목록을 불러오는데 실패했습니다.');
    } finally {
      setIsLoading(false);
    }
  };

  const loadMentorings = async (seasonId: number) => {
    setError(null);
    try {
      const data = await adminService.getMentorings(seasonId);
      setMentorings(data || []);
    } catch (err) {
      console.error('Failed to load mentorings:', err);
      setError('멘토링 목록을 불러오는데 실패했습니다.');
      setMentorings([]);
    }
  };

  const loadModules = async (mentoringId: number) => {
    setModuleErrors((prev) => ({ ...prev, [mentoringId]: '' }));
    try {
      const data = await adminService.getMentoringModules(mentoringId);
      setModules((prev) => ({ ...prev, [mentoringId]: data || [] }));
    } catch (err) {
      console.error('Failed to load modules:', err);
      setModuleErrors((prev) => ({
        ...prev,
        [mentoringId]: '모듈을 불러오는데 실패했습니다.',
      }));
    }
  };

  const handleExpandMentoring = (mentoringId: number) => {
    if (expandedMentoring === mentoringId) {
      setExpandedMentoring(null);
    } else {
      setExpandedMentoring(mentoringId);
      if (!modules[mentoringId]) {
        loadModules(mentoringId);
      }
    }
  };

  const handleCreateMentoring = () => {
    setEditingMentoring(null);
    setShowMentoringModal(true);
  };

  const handleEditMentoring = (mentoring: JobRoleMentoring) => {
    setEditingMentoring(mentoring);
    setShowMentoringModal(true);
  };

  const handleCreateModule = (mentoringId: number) => {
    setSelectedMentoringId(mentoringId);
    setEditingModule(null);
    setShowModuleModal(true);
  };

  const handleEditModule = (module: MentoringModule) => {
    setSelectedMentoringId(module.mentoringId);
    setEditingModule(module);
    setShowModuleModal(true);
  };

  const handleDeleteMentoring = async (mentoringId: number) => {
    if (!confirm('정말 이 멘토링 프로그램을 삭제하시겠습니까?')) return;
    try {
      await adminService.deleteMentoring(mentoringId);
      setMentorings((prev) => prev.filter((m) => m.id !== mentoringId));
    } catch (error) {
      console.error('Failed to delete mentoring:', error);
      alert('삭제에 실패했습니다.');
    }
  };

  const handleDeleteModule = async (moduleId: number, mentoringId: number) => {
    if (!confirm('정말 이 모듈을 삭제하시겠습니까?')) return;
    try {
      await adminService.deleteMentoringModule(moduleId);
      setModules((prev) => ({
        ...prev,
        [mentoringId]: prev[mentoringId].filter((m) => m.id !== moduleId),
      }));
    } catch (error) {
      console.error('Failed to delete module:', error);
      alert('삭제에 실패했습니다.');
    }
  };

  if (isLoading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="text-gray-500">로딩 중...</div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">멘토링 관리</h1>
          <p className="text-gray-500 mt-1">직무별 멘토링 프로그램과 주차별 모듈을 관리합니다.</p>
        </div>
        <button
          onClick={handleCreateMentoring}
          className="btn-primary flex items-center gap-2"
        >
          <Plus className="w-4 h-4" />
          멘토링 추가
        </button>
      </div>

      {/* Season Selector */}
      <div className="card">
        <div className="flex items-center gap-4">
          <label className="text-sm font-medium text-gray-700">시즌 선택:</label>
          <div className="relative">
            <select
              value={selectedSeasonId || ''}
              onChange={(e) => setSelectedSeasonId(Number(e.target.value))}
              className="appearance-none bg-white border border-gray-300 rounded-lg px-4 py-2 pr-8 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500"
            >
              {seasons.map((season) => (
                <option key={season.id} value={season.id}>
                  {season.title}
                </option>
              ))}
            </select>
            <ChevronDown className="absolute right-2 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-500 pointer-events-none" />
          </div>
        </div>
      </div>

      {/* Error State */}
      {error && (
        <div className="card bg-red-50 border-red-200 flex items-center gap-3">
          <AlertCircle className="w-5 h-5 text-red-500" />
          <p className="text-red-700">{error}</p>
        </div>
      )}

      {/* Mentorings List */}
      <div className="space-y-4">
        {mentorings.length === 0 && !error ? (
          <div className="card text-center py-12">
            <GraduationCap className="w-12 h-12 text-gray-300 mx-auto mb-4" />
            <p className="text-gray-500">등록된 멘토링 프로그램이 없습니다.</p>
            <button
              onClick={handleCreateMentoring}
              className="mt-4 text-primary-600 hover:text-primary-700 text-sm font-medium"
            >
              첫 멘토링 만들기
            </button>
          </div>
        ) : (
          mentorings.map((mentoring) => (
            <div key={mentoring.id} className="card">
              <div
                className="flex items-center justify-between cursor-pointer"
                onClick={() => handleExpandMentoring(mentoring.id)}
              >
                <div className="flex items-center gap-3">
                  {expandedMentoring === mentoring.id ? (
                    <ChevronDown className="w-5 h-5 text-gray-500" />
                  ) : (
                    <ChevronRight className="w-5 h-5 text-gray-500" />
                  )}
                  <div
                    className={`w-10 h-10 rounded-lg flex items-center justify-center ${
                      mentoring.jobRole === 'PLANNER'
                        ? 'bg-purple-100 text-purple-600'
                        : mentoring.jobRole === 'UXUI'
                        ? 'bg-pink-100 text-pink-600'
                        : mentoring.jobRole === 'FRONTEND'
                        ? 'bg-blue-100 text-blue-600'
                        : 'bg-green-100 text-green-600'
                    }`}
                  >
                    <GraduationCap className="w-5 h-5" />
                  </div>
                  <div>
                    <div className="flex items-center gap-2">
                      <h3 className="font-medium text-gray-900">{mentoring.title}</h3>
                      <span className="px-2 py-0.5 bg-gray-100 text-gray-600 text-xs rounded">
                        {JOB_ROLE_LABELS[mentoring.jobRole]}
                      </span>
                    </div>
                    <p className="text-sm text-gray-500">{mentoring.description}</p>
                  </div>
                </div>
                <div className="flex items-center gap-2">
                  <span
                    className={`px-2 py-1 rounded text-xs font-medium ${
                      mentoring.active
                        ? 'bg-green-100 text-green-700'
                        : 'bg-gray-100 text-gray-600'
                    }`}
                  >
                    {mentoring.active ? '활성' : '비활성'}
                  </span>
                  <button
                    onClick={(e) => {
                      e.stopPropagation();
                      handleEditMentoring(mentoring);
                    }}
                    className="p-2 text-gray-500 hover:text-primary-600 hover:bg-gray-100 rounded"
                  >
                    <Edit2 className="w-4 h-4" />
                  </button>
                  <button
                    onClick={(e) => {
                      e.stopPropagation();
                      handleDeleteMentoring(mentoring.id);
                    }}
                    className="p-2 text-gray-500 hover:text-red-600 hover:bg-gray-100 rounded"
                  >
                    <Trash2 className="w-4 h-4" />
                  </button>
                </div>
              </div>

              {/* Expanded Content */}
              {expandedMentoring === mentoring.id && (
                <div className="mt-4 pt-4 border-t border-gray-200">
                  <div className="mb-4">
                    <p className="text-xs text-gray-500 mb-1">학습 목표</p>
                    <p className="text-sm text-gray-700">{mentoring.learningObjectives || '-'}</p>
                  </div>

                  {/* Modules */}
                  <div className="mt-4">
                    <div className="flex items-center justify-between mb-3">
                      <h4 className="font-medium text-gray-900 flex items-center gap-2">
                        <BookOpen className="w-4 h-4" />
                        주차별 모듈
                      </h4>
                      <button
                        onClick={() => handleCreateModule(mentoring.id)}
                        className="text-sm text-primary-600 hover:text-primary-700 flex items-center gap-1"
                      >
                        <Plus className="w-4 h-4" />
                        모듈 추가
                      </button>
                    </div>

                    {/* Module Error */}
                    {moduleErrors[mentoring.id] && (
                      <div className="mb-3 p-3 bg-red-50 border border-red-200 rounded-lg flex items-center gap-2">
                        <AlertCircle className="w-4 h-4 text-red-500" />
                        <p className="text-sm text-red-700">{moduleErrors[mentoring.id]}</p>
                      </div>
                    )}

                    {modules[mentoring.id]?.length > 0 ? (
                      <div className="space-y-2">
                        {modules[mentoring.id]
                          .sort((a, b) => a.weekNumber - b.weekNumber)
                          .map((module) => (
                            <div
                              key={module.id}
                              className="flex items-center justify-between p-3 bg-gray-50 rounded-lg"
                            >
                              <div className="flex items-center gap-3">
                                <span className="w-8 h-8 bg-primary-100 text-primary-700 rounded-full flex items-center justify-center text-sm font-medium">
                                  {module.weekNumber}
                                </span>
                                <div>
                                  <p className="font-medium text-gray-900">{module.title}</p>
                                  <p className="text-xs text-gray-500">
                                    예상 {module.estimatedMinutes}분 ·{' '}
                                    {module.practiceTasks?.length || 0}개 실습
                                  </p>
                                </div>
                              </div>
                              <div className="flex items-center gap-1">
                                <button
                                  onClick={() => handleEditModule(module)}
                                  className="p-1.5 text-gray-500 hover:text-primary-600 hover:bg-gray-100 rounded"
                                >
                                  <Edit2 className="w-4 h-4" />
                                </button>
                                <button
                                  onClick={() => handleDeleteModule(module.id, mentoring.id)}
                                  className="p-1.5 text-gray-500 hover:text-red-600 hover:bg-gray-100 rounded"
                                >
                                  <Trash2 className="w-4 h-4" />
                                </button>
                              </div>
                            </div>
                          ))}
                      </div>
                    ) : !moduleErrors[mentoring.id] ? (
                      <p className="text-sm text-gray-500 text-center py-4">
                        등록된 모듈이 없습니다.
                      </p>
                    ) : null}
                  </div>
                </div>
              )}
            </div>
          ))
        )}
      </div>

      {/* Mentoring Modal */}
      {showMentoringModal && (
        <MentoringModal
          mentoring={editingMentoring}
          seasonId={selectedSeasonId!}
          onClose={() => setShowMentoringModal(false)}
          onSave={() => {
            setShowMentoringModal(false);
            loadMentorings(selectedSeasonId!);
          }}
        />
      )}

      {/* Module Modal */}
      {showModuleModal && selectedMentoringId && (
        <ModuleModal
          module={editingModule}
          mentoringId={selectedMentoringId}
          onClose={() => setShowModuleModal(false)}
          onSave={() => {
            setShowModuleModal(false);
            loadModules(selectedMentoringId);
          }}
        />
      )}
    </div>
  );
}

// Mentoring Modal Component
function MentoringModal({
  mentoring,
  seasonId,
  onClose,
  onSave,
}: {
  mentoring: JobRoleMentoring | null;
  seasonId: number;
  onClose: () => void;
  onSave: () => void;
}) {
  const [formData, setFormData] = useState({
    jobRole: mentoring?.jobRole || ('PLANNER' as JobRole),
    title: mentoring?.title || '',
    description: mentoring?.description || '',
    learningObjectives: mentoring?.learningObjectives || '',
  });
  const [isSaving, setIsSaving] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsSaving(true);

    try {
      if (mentoring) {
        await adminService.updateMentoring(mentoring.id, formData);
      } else {
        await adminService.createMentoring(seasonId, formData);
      }
      onSave();
    } catch (error) {
      console.error('Failed to save mentoring:', error);
      alert('저장에 실패했습니다.');
    } finally {
      setIsSaving(false);
    }
  };

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
      <div className="bg-white rounded-lg w-full max-w-lg">
        <div className="p-6 border-b border-gray-200">
          <h2 className="text-lg font-semibold">
            {mentoring ? '멘토링 수정' : '새 멘토링 만들기'}
          </h2>
        </div>
        <form onSubmit={handleSubmit} className="p-6 space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              직무 *
            </label>
            <select
              value={formData.jobRole}
              onChange={(e) => setFormData({ ...formData, jobRole: e.target.value as JobRole })}
              className="input"
              disabled={!!mentoring}
            >
              {Object.entries(JOB_ROLE_LABELS).map(([value, label]) => (
                <option key={value} value={value}>
                  {label}
                </option>
              ))}
            </select>
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              제목 *
            </label>
            <input
              type="text"
              value={formData.title}
              onChange={(e) => setFormData({ ...formData, title: e.target.value })}
              className="input"
              required
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              설명
            </label>
            <textarea
              value={formData.description}
              onChange={(e) => setFormData({ ...formData, description: e.target.value })}
              className="input"
              rows={2}
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              학습 목표
            </label>
            <textarea
              value={formData.learningObjectives}
              onChange={(e) => setFormData({ ...formData, learningObjectives: e.target.value })}
              className="input"
              rows={3}
            />
          </div>
          <div className="flex justify-end gap-3 pt-4 border-t">
            <button type="button" onClick={onClose} className="btn-secondary">
              취소
            </button>
            <button type="submit" className="btn-primary" disabled={isSaving}>
              {isSaving ? '저장 중...' : '저장'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

// Module Modal Component
function ModuleModal({
  module,
  mentoringId,
  onClose,
  onSave,
}: {
  module: MentoringModule | null;
  mentoringId: number;
  onClose: () => void;
  onSave: () => void;
}) {
  const [formData, setFormData] = useState({
    weekNumber: module?.weekNumber || 1,
    title: module?.title || '',
    description: module?.description || '',
    learningContent: module?.learningContent || '',
    keyPoints: module?.keyPoints?.join('\n') || '',
    commonMistakes: module?.commonMistakes?.join('\n') || '',
    practiceTasks: module?.practiceTasks?.join('\n') || '',
    referenceMaterials: module?.referenceMaterials?.join('\n') || '',
    estimatedMinutes: module?.estimatedMinutes || 60,
  });
  const [isSaving, setIsSaving] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsSaving(true);

    try {
      const payload = {
        ...formData,
        keyPoints: formData.keyPoints.split('\n').map((s) => s.trim()).filter(Boolean),
        commonMistakes: formData.commonMistakes.split('\n').map((s) => s.trim()).filter(Boolean),
        practiceTasks: formData.practiceTasks.split('\n').map((s) => s.trim()).filter(Boolean),
        referenceMaterials: formData.referenceMaterials.split('\n').map((s) => s.trim()).filter(Boolean),
      };

      if (module) {
        await adminService.updateMentoringModule(module.id, payload);
      } else {
        await adminService.createMentoringModule(mentoringId, payload);
      }
      onSave();
    } catch (error) {
      console.error('Failed to save module:', error);
      alert('저장에 실패했습니다.');
    } finally {
      setIsSaving(false);
    }
  };

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
      <div className="bg-white rounded-lg w-full max-w-2xl max-h-[90vh] overflow-y-auto">
        <div className="p-6 border-b border-gray-200">
          <h2 className="text-lg font-semibold">
            {module ? '모듈 수정' : '새 모듈 만들기'}
          </h2>
        </div>
        <form onSubmit={handleSubmit} className="p-6 space-y-4">
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                주차 *
              </label>
              <input
                type="number"
                value={formData.weekNumber}
                onChange={(e) => setFormData({ ...formData, weekNumber: Number(e.target.value) })}
                className="input"
                min={1}
                max={12}
                required
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                예상 소요 시간 (분)
              </label>
              <input
                type="number"
                value={formData.estimatedMinutes}
                onChange={(e) => setFormData({ ...formData, estimatedMinutes: Number(e.target.value) })}
                className="input"
                min={1}
              />
            </div>
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              제목 *
            </label>
            <input
              type="text"
              value={formData.title}
              onChange={(e) => setFormData({ ...formData, title: e.target.value })}
              className="input"
              required
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              설명
            </label>
            <textarea
              value={formData.description}
              onChange={(e) => setFormData({ ...formData, description: e.target.value })}
              className="input"
              rows={2}
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              학습 내용
            </label>
            <textarea
              value={formData.learningContent}
              onChange={(e) => setFormData({ ...formData, learningContent: e.target.value })}
              className="input"
              rows={3}
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              핵심 포인트 (줄바꿈으로 구분)
            </label>
            <textarea
              value={formData.keyPoints}
              onChange={(e) => setFormData({ ...formData, keyPoints: e.target.value })}
              className="input"
              rows={3}
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              흔한 실수 (줄바꿈으로 구분)
            </label>
            <textarea
              value={formData.commonMistakes}
              onChange={(e) => setFormData({ ...formData, commonMistakes: e.target.value })}
              className="input"
              rows={2}
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              실습 과제 (줄바꿈으로 구분)
            </label>
            <textarea
              value={formData.practiceTasks}
              onChange={(e) => setFormData({ ...formData, practiceTasks: e.target.value })}
              className="input"
              rows={3}
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              참고 자료 (줄바꿈으로 구분)
            </label>
            <textarea
              value={formData.referenceMaterials}
              onChange={(e) => setFormData({ ...formData, referenceMaterials: e.target.value })}
              className="input"
              rows={2}
            />
          </div>
          <div className="flex justify-end gap-3 pt-4 border-t">
            <button type="button" onClick={onClose} className="btn-secondary">
              취소
            </button>
            <button type="submit" className="btn-primary" disabled={isSaving}>
              {isSaving ? '저장 중...' : '저장'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
