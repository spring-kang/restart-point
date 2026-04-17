import { useState, useEffect } from 'react';
import {
  Book,
  Plus,
  Edit2,
  Trash2,
  ChevronDown,
  ChevronRight,
  FileText,
  AlertCircle,
  Power,
} from 'lucide-react';
import adminService from '../services/adminService';

type JobRole = 'PLANNER' | 'UXUI' | 'FRONTEND' | 'BACKEND';

const JOB_ROLE_LABELS: Record<JobRole, string> = {
  PLANNER: '기획자',
  UXUI: 'UX/UI 디자이너',
  FRONTEND: '프론트엔드',
  BACKEND: '백엔드',
};

// Backend DTO: ProjectTemplateResponse
interface ProjectTemplate {
  id: number;
  seasonId: number;
  seasonTitle: string;
  title: string;
  description: string;
  totalWeeks: number;
  active: boolean;
  guidelineCount: number;
  createdAt: string;
  updatedAt: string;
}

// Backend DTO: WeeklyGuidelineResponse
interface WeeklyGuideline {
  id: number;
  templateId: number;
  weekNumber: number;
  title: string;
  description: string;
  keyObjectives: string;
  milestones: string[];
  recommendedActions: string[];
  guideContent: string;
  focusRole: JobRole | null;
  checklistItems: string[];
  referenceLinks: string[];
  createdAt: string;
  updatedAt: string;
}

interface Season {
  id: number;
  title: string;
  status: string;
}

export default function GuidesPage() {
  const [seasons, setSeasons] = useState<Season[]>([]);
  const [selectedSeasonId, setSelectedSeasonId] = useState<number | null>(null);
  const [templates, setTemplates] = useState<ProjectTemplate[]>([]);
  const [expandedTemplate, setExpandedTemplate] = useState<number | null>(null);
  const [guidelines, setGuidelines] = useState<Record<number, WeeklyGuideline[]>>({});
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [guidelineErrors, setGuidelineErrors] = useState<Record<number, string>>({});
  const [showTemplateModal, setShowTemplateModal] = useState(false);
  const [showGuidelineModal, setShowGuidelineModal] = useState(false);
  const [editingTemplate, setEditingTemplate] = useState<ProjectTemplate | null>(null);
  const [editingGuideline, setEditingGuideline] = useState<WeeklyGuideline | null>(null);
  const [selectedTemplateId, setSelectedTemplateId] = useState<number | null>(null);

  useEffect(() => {
    loadSeasons();
  }, []);

  useEffect(() => {
    if (selectedSeasonId) {
      loadTemplates(selectedSeasonId);
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

  const loadTemplates = async (seasonId: number) => {
    setError(null);
    try {
      const data = await adminService.getProjectTemplates(seasonId);
      setTemplates(data || []);
    } catch (err) {
      console.error('Failed to load templates:', err);
      setError('템플릿 목록을 불러오는데 실패했습니다.');
      setTemplates([]);
    }
  };

  const loadGuidelines = async (templateId: number) => {
    setGuidelineErrors((prev) => ({ ...prev, [templateId]: '' }));
    try {
      const data = await adminService.getWeeklyGuidelines(templateId);
      setGuidelines((prev) => ({ ...prev, [templateId]: data || [] }));
    } catch (err) {
      console.error('Failed to load guidelines:', err);
      setGuidelineErrors((prev) => ({
        ...prev,
        [templateId]: '가이드라인을 불러오는데 실패했습니다.',
      }));
    }
  };

  const handleExpandTemplate = (templateId: number) => {
    if (expandedTemplate === templateId) {
      setExpandedTemplate(null);
    } else {
      setExpandedTemplate(templateId);
      if (!guidelines[templateId]) {
        loadGuidelines(templateId);
      }
    }
  };

  const handleCreateTemplate = () => {
    setEditingTemplate(null);
    setShowTemplateModal(true);
  };

  const handleEditTemplate = (template: ProjectTemplate) => {
    setEditingTemplate(template);
    setShowTemplateModal(true);
  };

  const handleCreateGuideline = (templateId: number) => {
    setSelectedTemplateId(templateId);
    setEditingGuideline(null);
    setShowGuidelineModal(true);
  };

  const handleEditGuideline = (guideline: WeeklyGuideline) => {
    setSelectedTemplateId(guideline.templateId);
    setEditingGuideline(guideline);
    setShowGuidelineModal(true);
  };

  const handleDeleteTemplate = async (templateId: number) => {
    if (!confirm('정말 이 템플릿을 삭제하시겠습니까?')) return;
    try {
      await adminService.deleteProjectTemplate(templateId);
      setTemplates((prev) => prev.filter((t) => t.id !== templateId));
    } catch (err) {
      console.error('Failed to delete template:', err);
      alert('템플릿 삭제에 실패했습니다.');
    }
  };

  const handleActivateTemplate = async (templateId: number) => {
    try {
      await adminService.activateProjectTemplate(templateId);
      // Reload templates to reflect the change
      if (selectedSeasonId) {
        loadTemplates(selectedSeasonId);
      }
    } catch (err) {
      console.error('Failed to activate template:', err);
      alert('템플릿 활성화에 실패했습니다.');
    }
  };

  const handleDeleteGuideline = async (guidelineId: number, templateId: number) => {
    if (!confirm('정말 이 가이드라인을 삭제하시겠습니까?')) return;
    try {
      await adminService.deleteWeeklyGuideline(guidelineId);
      setGuidelines((prev) => ({
        ...prev,
        [templateId]: prev[templateId].filter((g) => g.id !== guidelineId),
      }));
    } catch (err) {
      console.error('Failed to delete guideline:', err);
      alert('가이드라인 삭제에 실패했습니다.');
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
          <h1 className="text-2xl font-bold text-gray-900">프로젝트 가이드 관리</h1>
          <p className="text-gray-500 mt-1">프로젝트 템플릿과 주차별 가이드라인을 관리합니다.</p>
        </div>
        <button
          onClick={handleCreateTemplate}
          className="btn-primary flex items-center gap-2"
        >
          <Plus className="w-4 h-4" />
          템플릿 추가
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

      {/* Templates List */}
      <div className="space-y-4">
        {templates.length === 0 && !error ? (
          <div className="card text-center py-12">
            <Book className="w-12 h-12 text-gray-300 mx-auto mb-4" />
            <p className="text-gray-500">등록된 프로젝트 템플릿이 없습니다.</p>
            <button
              onClick={handleCreateTemplate}
              className="mt-4 text-primary-600 hover:text-primary-700 text-sm font-medium"
            >
              첫 템플릿 만들기
            </button>
          </div>
        ) : (
          templates.map((template) => (
            <div key={template.id} className="card">
              <div
                className="flex items-center justify-between cursor-pointer"
                onClick={() => handleExpandTemplate(template.id)}
              >
                <div className="flex items-center gap-3">
                  {expandedTemplate === template.id ? (
                    <ChevronDown className="w-5 h-5 text-gray-500" />
                  ) : (
                    <ChevronRight className="w-5 h-5 text-gray-500" />
                  )}
                  <div>
                    <h3 className="font-medium text-gray-900">{template.title}</h3>
                    <p className="text-sm text-gray-500">
                      {template.description || '설명 없음'} · {template.totalWeeks}주차 ·{' '}
                      {template.guidelineCount}개 가이드라인
                    </p>
                  </div>
                </div>
                <div className="flex items-center gap-2">
                  <span
                    className={`px-2 py-1 rounded text-xs font-medium ${
                      template.active
                        ? 'bg-green-100 text-green-700'
                        : 'bg-gray-100 text-gray-600'
                    }`}
                  >
                    {template.active ? '활성' : '비활성'}
                  </span>
                  {!template.active && (
                    <button
                      onClick={(e) => {
                        e.stopPropagation();
                        handleActivateTemplate(template.id);
                      }}
                      className="p-2 text-gray-500 hover:text-green-600 hover:bg-gray-100 rounded"
                      title="활성화"
                    >
                      <Power className="w-4 h-4" />
                    </button>
                  )}
                  <button
                    onClick={(e) => {
                      e.stopPropagation();
                      handleEditTemplate(template);
                    }}
                    className="p-2 text-gray-500 hover:text-primary-600 hover:bg-gray-100 rounded"
                  >
                    <Edit2 className="w-4 h-4" />
                  </button>
                  <button
                    onClick={(e) => {
                      e.stopPropagation();
                      handleDeleteTemplate(template.id);
                    }}
                    className="p-2 text-gray-500 hover:text-red-600 hover:bg-gray-100 rounded"
                  >
                    <Trash2 className="w-4 h-4" />
                  </button>
                </div>
              </div>

              {/* Expanded Content */}
              {expandedTemplate === template.id && (
                <div className="mt-4 pt-4 border-t border-gray-200">
                  {/* Weekly Guidelines */}
                  <div>
                    <div className="flex items-center justify-between mb-3">
                      <h4 className="font-medium text-gray-900 flex items-center gap-2">
                        <FileText className="w-4 h-4" />
                        주차별 가이드라인
                      </h4>
                      <button
                        onClick={() => handleCreateGuideline(template.id)}
                        className="text-sm text-primary-600 hover:text-primary-700 flex items-center gap-1"
                      >
                        <Plus className="w-4 h-4" />
                        가이드라인 추가
                      </button>
                    </div>

                    {/* Guideline Error */}
                    {guidelineErrors[template.id] && (
                      <div className="mb-3 p-3 bg-red-50 border border-red-200 rounded-lg flex items-center gap-2">
                        <AlertCircle className="w-4 h-4 text-red-500" />
                        <p className="text-sm text-red-700">{guidelineErrors[template.id]}</p>
                      </div>
                    )}

                    {guidelines[template.id]?.length > 0 ? (
                      <div className="space-y-2">
                        {guidelines[template.id]
                          .sort((a, b) => a.weekNumber - b.weekNumber)
                          .map((guideline) => (
                            <div
                              key={guideline.id}
                              className="flex items-center justify-between p-3 bg-gray-50 rounded-lg"
                            >
                              <div className="flex items-center gap-3">
                                <span className="w-8 h-8 bg-primary-100 text-primary-700 rounded-full flex items-center justify-center text-sm font-medium">
                                  {guideline.weekNumber}
                                </span>
                                <div>
                                  <p className="font-medium text-gray-900">{guideline.title}</p>
                                  <p className="text-xs text-gray-500">
                                    {guideline.focusRole
                                      ? JOB_ROLE_LABELS[guideline.focusRole]
                                      : '전체'}{' '}
                                    · {guideline.checklistItems?.length || 0}개 체크리스트
                                  </p>
                                </div>
                              </div>
                              <div className="flex items-center gap-1">
                                <button
                                  onClick={() => handleEditGuideline(guideline)}
                                  className="p-1.5 text-gray-500 hover:text-primary-600 hover:bg-gray-100 rounded"
                                >
                                  <Edit2 className="w-4 h-4" />
                                </button>
                                <button
                                  onClick={() => handleDeleteGuideline(guideline.id, template.id)}
                                  className="p-1.5 text-gray-500 hover:text-red-600 hover:bg-gray-100 rounded"
                                >
                                  <Trash2 className="w-4 h-4" />
                                </button>
                              </div>
                            </div>
                          ))}
                      </div>
                    ) : !guidelineErrors[template.id] ? (
                      <p className="text-sm text-gray-500 text-center py-4">
                        등록된 가이드라인이 없습니다.
                      </p>
                    ) : null}
                  </div>
                </div>
              )}
            </div>
          ))
        )}
      </div>

      {/* Template Modal */}
      {showTemplateModal && (
        <TemplateModal
          template={editingTemplate}
          seasonId={selectedSeasonId!}
          onClose={() => setShowTemplateModal(false)}
          onSave={() => {
            setShowTemplateModal(false);
            loadTemplates(selectedSeasonId!);
          }}
        />
      )}

      {/* Guideline Modal */}
      {showGuidelineModal && selectedTemplateId && (
        <GuidelineModal
          guideline={editingGuideline}
          templateId={selectedTemplateId}
          onClose={() => setShowGuidelineModal(false)}
          onSave={() => {
            setShowGuidelineModal(false);
            loadGuidelines(selectedTemplateId);
          }}
        />
      )}
    </div>
  );
}

// Template Modal Component - matches ProjectTemplateRequest
function TemplateModal({
  template,
  seasonId,
  onClose,
  onSave,
}: {
  template: ProjectTemplate | null;
  seasonId: number;
  onClose: () => void;
  onSave: () => void;
}) {
  const [formData, setFormData] = useState({
    title: template?.title || '',
    description: template?.description || '',
    totalWeeks: template?.totalWeeks || 8,
  });
  const [isSaving, setIsSaving] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsSaving(true);

    try {
      if (template) {
        await adminService.updateProjectTemplate(template.id, formData);
      } else {
        await adminService.createProjectTemplate(seasonId, formData);
      }
      onSave();
    } catch (err) {
      console.error('Failed to save template:', err);
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
            {template ? '템플릿 수정' : '새 템플릿 만들기'}
          </h2>
        </div>
        <form onSubmit={handleSubmit} className="p-6 space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              템플릿 제목 *
            </label>
            <input
              type="text"
              value={formData.title}
              onChange={(e) => setFormData({ ...formData, title: e.target.value })}
              className="input"
              placeholder="예: 8주 프로젝트 가이드"
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
              rows={3}
              placeholder="템플릿에 대한 간단한 설명을 입력하세요."
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              총 주차 수 *
            </label>
            <input
              type="number"
              value={formData.totalWeeks}
              onChange={(e) => setFormData({ ...formData, totalWeeks: Number(e.target.value) })}
              className="input"
              min={1}
              max={52}
              required
            />
            <p className="mt-1 text-xs text-gray-500">프로젝트 진행 기간을 주 단위로 입력하세요.</p>
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

// Guideline Modal Component - matches WeeklyGuidelineRequest
function GuidelineModal({
  guideline,
  templateId,
  onClose,
  onSave,
}: {
  guideline: WeeklyGuideline | null;
  templateId: number;
  onClose: () => void;
  onSave: () => void;
}) {
  const [formData, setFormData] = useState({
    weekNumber: guideline?.weekNumber || 1,
    title: guideline?.title || '',
    description: guideline?.description || '',
    keyObjectives: guideline?.keyObjectives || '',
    milestones: guideline?.milestones?.join('\n') || '',
    recommendedActions: guideline?.recommendedActions?.join('\n') || '',
    guideContent: guideline?.guideContent || '',
    focusRole: guideline?.focusRole || ('' as JobRole | ''),
    checklistItems: guideline?.checklistItems?.join('\n') || '',
    referenceLinks: guideline?.referenceLinks?.join('\n') || '',
  });
  const [isSaving, setIsSaving] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsSaving(true);

    try {
      const payload = {
        weekNumber: formData.weekNumber,
        title: formData.title,
        description: formData.description || null,
        keyObjectives: formData.keyObjectives || null,
        milestones: formData.milestones
          .split('\n')
          .map((s) => s.trim())
          .filter(Boolean),
        recommendedActions: formData.recommendedActions
          .split('\n')
          .map((s) => s.trim())
          .filter(Boolean),
        guideContent: formData.guideContent || null,
        focusRole: formData.focusRole || null,
        checklistItems: formData.checklistItems
          .split('\n')
          .map((s) => s.trim())
          .filter(Boolean),
        referenceLinks: formData.referenceLinks
          .split('\n')
          .map((s) => s.trim())
          .filter(Boolean),
      };

      if (guideline) {
        await adminService.updateWeeklyGuideline(guideline.id, payload);
      } else {
        await adminService.createWeeklyGuideline(templateId, payload);
      }
      onSave();
    } catch (err) {
      console.error('Failed to save guideline:', err);
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
            {guideline ? '가이드라인 수정' : '새 가이드라인 만들기'}
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
                max={52}
                required
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                집중 직무
              </label>
              <select
                value={formData.focusRole}
                onChange={(e) => setFormData({ ...formData, focusRole: e.target.value as JobRole | '' })}
                className="input"
              >
                <option value="">전체</option>
                {Object.entries(JOB_ROLE_LABELS).map(([value, label]) => (
                  <option key={value} value={value}>
                    {label}
                  </option>
                ))}
              </select>
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
              placeholder="예: 아이디어 구체화 및 기획"
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
              placeholder="이번 주차에 대한 간단한 설명"
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              핵심 목표
            </label>
            <textarea
              value={formData.keyObjectives}
              onChange={(e) => setFormData({ ...formData, keyObjectives: e.target.value })}
              className="input"
              rows={2}
              placeholder="이번 주차의 핵심 목표를 작성하세요."
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              마일스톤 (줄바꿈으로 구분)
            </label>
            <textarea
              value={formData.milestones}
              onChange={(e) => setFormData({ ...formData, milestones: e.target.value })}
              className="input"
              rows={3}
              placeholder="팀 구성 완료&#10;아이디어 선정&#10;역할 분담 완료"
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              추천 활동 (줄바꿈으로 구분)
            </label>
            <textarea
              value={formData.recommendedActions}
              onChange={(e) => setFormData({ ...formData, recommendedActions: e.target.value })}
              className="input"
              rows={3}
              placeholder="팀 미팅 진행&#10;경쟁 서비스 분석&#10;사용자 인터뷰 계획"
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              가이드 내용
            </label>
            <textarea
              value={formData.guideContent}
              onChange={(e) => setFormData({ ...formData, guideContent: e.target.value })}
              className="input"
              rows={4}
              placeholder="상세한 가이드 내용을 작성하세요. (Markdown 지원)"
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              체크리스트 (줄바꿈으로 구분)
            </label>
            <textarea
              value={formData.checklistItems}
              onChange={(e) => setFormData({ ...formData, checklistItems: e.target.value })}
              className="input"
              rows={3}
              placeholder="팀원 역할 분담 확정&#10;Slack 채널 개설&#10;GitHub 레포지토리 생성"
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              참고 링크 (줄바꿈으로 구분)
            </label>
            <textarea
              value={formData.referenceLinks}
              onChange={(e) => setFormData({ ...formData, referenceLinks: e.target.value })}
              className="input"
              rows={2}
              placeholder="https://example.com/guide&#10;https://notion.so/template"
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
