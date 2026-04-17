import { useState, useEffect } from 'react';
import {
  Book,
  Plus,
  Edit2,
  Trash2,
  ChevronDown,
  ChevronRight,
  FileText,
  CheckCircle,
} from 'lucide-react';
import adminService from '../services/adminService';

interface ProjectTemplate {
  id: number;
  seasonId: number;
  name: string;
  description: string;
  problemDefinition: string;
  targetUsers: string;
  expectedOutcome: string;
  techStack: string[];
  weeklyGoals: string[];
  active: boolean;
}

interface WeeklyGuideline {
  id: number;
  templateId: number;
  weekNumber: number;
  title: string;
  description: string;
  objectives: string[];
  tasks: string[];
  deliverables: string[];
  tips: string;
  estimatedHours: number;
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
    } catch (error) {
      console.error('Failed to load seasons:', error);
    } finally {
      setIsLoading(false);
    }
  };

  const loadTemplates = async (seasonId: number) => {
    try {
      const data = await adminService.getProjectTemplates(seasonId);
      setTemplates(data);
    } catch (error) {
      console.error('Failed to load templates:', error);
      setTemplates([]);
    }
  };

  const loadGuidelines = async (templateId: number) => {
    try {
      const data = await adminService.getWeeklyGuidelines(templateId);
      setGuidelines((prev) => ({ ...prev, [templateId]: data }));
    } catch (error) {
      console.error('Failed to load guidelines:', error);
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
    } catch (error) {
      console.error('Failed to delete template:', error);
      alert('템플릿 삭제에 실패했습니다.');
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
    } catch (error) {
      console.error('Failed to delete guideline:', error);
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

      {/* Templates List */}
      <div className="space-y-4">
        {templates.length === 0 ? (
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
                    <h3 className="font-medium text-gray-900">{template.name}</h3>
                    <p className="text-sm text-gray-500">{template.description}</p>
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
                  <div className="grid grid-cols-2 gap-4 mb-4">
                    <div>
                      <p className="text-xs text-gray-500 mb-1">문제 정의</p>
                      <p className="text-sm text-gray-700">{template.problemDefinition || '-'}</p>
                    </div>
                    <div>
                      <p className="text-xs text-gray-500 mb-1">타겟 사용자</p>
                      <p className="text-sm text-gray-700">{template.targetUsers || '-'}</p>
                    </div>
                    <div>
                      <p className="text-xs text-gray-500 mb-1">기술 스택</p>
                      <div className="flex flex-wrap gap-1">
                        {template.techStack?.map((tech, i) => (
                          <span
                            key={i}
                            className="px-2 py-0.5 bg-gray-100 text-gray-600 text-xs rounded"
                          >
                            {tech}
                          </span>
                        ))}
                      </div>
                    </div>
                  </div>

                  {/* Weekly Guidelines */}
                  <div className="mt-4">
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

                    {guidelines[template.id]?.length > 0 ? (
                      <div className="space-y-2">
                        {guidelines[template.id].map((guideline) => (
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
                                  예상 {guideline.estimatedHours}시간 ·{' '}
                                  {guideline.tasks?.length || 0}개 과제
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
                    ) : (
                      <p className="text-sm text-gray-500 text-center py-4">
                        등록된 가이드라인이 없습니다.
                      </p>
                    )}
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

// Template Modal Component
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
    name: template?.name || '',
    description: template?.description || '',
    problemDefinition: template?.problemDefinition || '',
    targetUsers: template?.targetUsers || '',
    expectedOutcome: template?.expectedOutcome || '',
    techStack: template?.techStack?.join(', ') || '',
    weeklyGoals: template?.weeklyGoals?.join('\n') || '',
    active: template?.active ?? true,
  });
  const [isSaving, setIsSaving] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsSaving(true);

    try {
      const payload = {
        ...formData,
        techStack: formData.techStack.split(',').map((s) => s.trim()).filter(Boolean),
        weeklyGoals: formData.weeklyGoals.split('\n').map((s) => s.trim()).filter(Boolean),
      };

      if (template) {
        await adminService.updateProjectTemplate(template.id, payload);
      } else {
        await adminService.createProjectTemplate(seasonId, payload);
      }
      onSave();
    } catch (error) {
      console.error('Failed to save template:', error);
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
            {template ? '템플릿 수정' : '새 템플릿 만들기'}
          </h2>
        </div>
        <form onSubmit={handleSubmit} className="p-6 space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              템플릿 이름 *
            </label>
            <input
              type="text"
              value={formData.name}
              onChange={(e) => setFormData({ ...formData, name: e.target.value })}
              className="input"
              required
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              설명 *
            </label>
            <textarea
              value={formData.description}
              onChange={(e) => setFormData({ ...formData, description: e.target.value })}
              className="input"
              rows={2}
              required
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              문제 정의
            </label>
            <textarea
              value={formData.problemDefinition}
              onChange={(e) => setFormData({ ...formData, problemDefinition: e.target.value })}
              className="input"
              rows={2}
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              타겟 사용자
            </label>
            <input
              type="text"
              value={formData.targetUsers}
              onChange={(e) => setFormData({ ...formData, targetUsers: e.target.value })}
              className="input"
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              기대 결과물
            </label>
            <textarea
              value={formData.expectedOutcome}
              onChange={(e) => setFormData({ ...formData, expectedOutcome: e.target.value })}
              className="input"
              rows={2}
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              기술 스택 (쉼표로 구분)
            </label>
            <input
              type="text"
              value={formData.techStack}
              onChange={(e) => setFormData({ ...formData, techStack: e.target.value })}
              className="input"
              placeholder="React, Spring Boot, PostgreSQL"
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              주차별 목표 (줄바꿈으로 구분)
            </label>
            <textarea
              value={formData.weeklyGoals}
              onChange={(e) => setFormData({ ...formData, weeklyGoals: e.target.value })}
              className="input"
              rows={4}
              placeholder="1주차: 기획 완료&#10;2주차: 디자인 완료&#10;3주차: 개발 시작"
            />
          </div>
          <div className="flex items-center gap-2">
            <input
              type="checkbox"
              id="active"
              checked={formData.active}
              onChange={(e) => setFormData({ ...formData, active: e.target.checked })}
              className="rounded border-gray-300"
            />
            <label htmlFor="active" className="text-sm text-gray-700">
              활성화
            </label>
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

// Guideline Modal Component
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
    objectives: guideline?.objectives?.join('\n') || '',
    tasks: guideline?.tasks?.join('\n') || '',
    deliverables: guideline?.deliverables?.join('\n') || '',
    tips: guideline?.tips || '',
    estimatedHours: guideline?.estimatedHours || 10,
  });
  const [isSaving, setIsSaving] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsSaving(true);

    try {
      const payload = {
        ...formData,
        objectives: formData.objectives.split('\n').map((s) => s.trim()).filter(Boolean),
        tasks: formData.tasks.split('\n').map((s) => s.trim()).filter(Boolean),
        deliverables: formData.deliverables.split('\n').map((s) => s.trim()).filter(Boolean),
      };

      if (guideline) {
        await adminService.updateWeeklyGuideline(guideline.id, payload);
      } else {
        await adminService.createWeeklyGuideline(templateId, payload);
      }
      onSave();
    } catch (error) {
      console.error('Failed to save guideline:', error);
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
                max={12}
                required
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                예상 소요 시간
              </label>
              <input
                type="number"
                value={formData.estimatedHours}
                onChange={(e) => setFormData({ ...formData, estimatedHours: Number(e.target.value) })}
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
              학습 목표 (줄바꿈으로 구분)
            </label>
            <textarea
              value={formData.objectives}
              onChange={(e) => setFormData({ ...formData, objectives: e.target.value })}
              className="input"
              rows={3}
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              과제 목록 (줄바꿈으로 구분)
            </label>
            <textarea
              value={formData.tasks}
              onChange={(e) => setFormData({ ...formData, tasks: e.target.value })}
              className="input"
              rows={3}
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              산출물 (줄바꿈으로 구분)
            </label>
            <textarea
              value={formData.deliverables}
              onChange={(e) => setFormData({ ...formData, deliverables: e.target.value })}
              className="input"
              rows={2}
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              팁/참고사항
            </label>
            <textarea
              value={formData.tips}
              onChange={(e) => setFormData({ ...formData, tips: e.target.value })}
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
