import { useState, useEffect } from 'react';
import { Plus, Edit, Trash2, Play, CheckCircle } from 'lucide-react';
import adminService from '../services/adminService';
import type { Season, SeasonCreateRequest, SeasonStatus } from '../types';
import { SEASON_STATUS_LABELS, SEASON_STATUS_COLORS } from '../types';

const INITIAL_FORM_DATA: SeasonCreateRequest = {
  title: '',
  description: '',
  recruitmentStartAt: '',
  recruitmentEndAt: '',
  teamBuildingStartAt: '',
  teamBuildingEndAt: '',
  projectStartAt: '',
  projectEndAt: '',
  reviewStartAt: '',
  reviewEndAt: '',
  expertReviewWeight: 70,
  candidateReviewWeight: 30,
};

// LocalDateTime 형식으로 변환 (YYYY-MM-DDTHH:mm:ss)
const toLocalDateTime = (dateStr: string): string => {
  if (!dateStr) return '';
  return `${dateStr}T00:00:00`;
};

// date input용 형식으로 변환 (YYYY-MM-DD)
const toDateInput = (dateTimeStr: string): string => {
  if (!dateTimeStr) return '';
  return dateTimeStr.split('T')[0];
};

export default function SeasonsPage() {
  const [seasons, setSeasons] = useState<Season[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [showModal, setShowModal] = useState(false);
  const [editingSeason, setEditingSeason] = useState<Season | null>(null);
  const [formData, setFormData] = useState<SeasonCreateRequest>(INITIAL_FORM_DATA);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [filter, setFilter] = useState<'all' | SeasonStatus>('all');

  useEffect(() => {
    loadSeasons();
  }, []);

  const loadSeasons = async () => {
    try {
      const pageData = await adminService.getSeasons();
      setSeasons(pageData.content);
    } catch (error) {
      console.error('Failed to load seasons:', error);
    } finally {
      setIsLoading(false);
    }
  };

  const openCreateModal = () => {
    setEditingSeason(null);
    setFormData(INITIAL_FORM_DATA);
    setShowModal(true);
  };

  const openEditModal = (season: Season) => {
    setEditingSeason(season);
    setFormData({
      title: season.title,
      description: season.description || '',
      recruitmentStartAt: toDateInput(season.recruitmentStartAt),
      recruitmentEndAt: toDateInput(season.recruitmentEndAt),
      teamBuildingStartAt: toDateInput(season.teamBuildingStartAt),
      teamBuildingEndAt: toDateInput(season.teamBuildingEndAt),
      projectStartAt: toDateInput(season.projectStartAt),
      projectEndAt: toDateInput(season.projectEndAt),
      reviewStartAt: toDateInput(season.reviewStartAt),
      reviewEndAt: toDateInput(season.reviewEndAt),
      expertReviewWeight: season.expertReviewWeight,
      candidateReviewWeight: season.candidateReviewWeight,
    });
    setShowModal(true);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsSubmitting(true);

    // Convert date inputs to LocalDateTime format
    const requestData: SeasonCreateRequest = {
      ...formData,
      recruitmentStartAt: toLocalDateTime(formData.recruitmentStartAt),
      recruitmentEndAt: toLocalDateTime(formData.recruitmentEndAt),
      teamBuildingStartAt: toLocalDateTime(formData.teamBuildingStartAt),
      teamBuildingEndAt: toLocalDateTime(formData.teamBuildingEndAt),
      projectStartAt: toLocalDateTime(formData.projectStartAt),
      projectEndAt: toLocalDateTime(formData.projectEndAt),
      reviewStartAt: toLocalDateTime(formData.reviewStartAt),
      reviewEndAt: toLocalDateTime(formData.reviewEndAt),
    };

    try {
      if (editingSeason) {
        await adminService.updateSeason(editingSeason.id, requestData);
      } else {
        await adminService.createSeason(requestData);
      }
      setShowModal(false);
      loadSeasons();
    } catch (error) {
      console.error('Failed to save season:', error);
      alert('시즌 저장에 실패했습니다.');
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleDelete = async (season: Season) => {
    if (!confirm(`"${season.title}" 시즌을 삭제하시겠습니까?`)) return;

    try {
      await adminService.deleteSeason(season.id);
      loadSeasons();
    } catch (error) {
      console.error('Failed to delete season:', error);
      alert('시즌 삭제에 실패했습니다.');
    }
  };

  const handleStatusChange = async (season: Season, status: SeasonStatus) => {
    try {
      await adminService.updateSeasonStatus(season.id, status);
      loadSeasons();
    } catch (error) {
      console.error('Failed to update status:', error);
      alert('상태 변경에 실패했습니다.');
    }
  };

  const filteredSeasons =
    filter === 'all' ? seasons : seasons.filter((s) => s.status === filter);

  const getNextStatus = (status: SeasonStatus): SeasonStatus | null => {
    const flow: Record<SeasonStatus, SeasonStatus | null> = {
      DRAFT: 'RECRUITING',
      RECRUITING: 'TEAM_BUILDING',
      TEAM_BUILDING: 'IN_PROGRESS',
      IN_PROGRESS: 'REVIEWING',
      REVIEWING: 'COMPLETED',
      COMPLETED: null,
    };
    return flow[status];
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
          <h1 className="text-2xl font-bold text-gray-900">시즌 관리</h1>
          <p className="text-gray-500 mt-1">프로젝트 시즌을 생성하고 관리합니다.</p>
        </div>
        <button onClick={openCreateModal} className="btn-primary flex items-center gap-2">
          <Plus className="w-5 h-5" />
          새 시즌
        </button>
      </div>

      {/* Filters */}
      <div className="flex gap-2 flex-wrap">
        <button
          onClick={() => setFilter('all')}
          className={`px-3 py-1.5 rounded-lg text-sm font-medium transition-colors ${
            filter === 'all'
              ? 'bg-gray-900 text-white'
              : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
          }`}
        >
          전체 ({seasons.length})
        </button>
        {(['DRAFT', 'RECRUITING', 'IN_PROGRESS', 'COMPLETED'] as SeasonStatus[]).map(
          (status) => {
            const count = seasons.filter((s) => s.status === status).length;
            return (
              <button
                key={status}
                onClick={() => setFilter(status)}
                className={`px-3 py-1.5 rounded-lg text-sm font-medium transition-colors ${
                  filter === status
                    ? 'bg-gray-900 text-white'
                    : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
                }`}
              >
                {SEASON_STATUS_LABELS[status]} ({count})
              </button>
            );
          }
        )}
      </div>

      {/* Season List */}
      {filteredSeasons.length === 0 ? (
        <div className="card text-center py-12">
          <p className="text-gray-500">시즌이 없습니다.</p>
        </div>
      ) : (
        <div className="space-y-4">
          {filteredSeasons.map((season) => {
            const nextStatus = getNextStatus(season.status);
            return (
              <div key={season.id} className="card">
                <div className="flex items-start justify-between">
                  <div className="flex-1">
                    <div className="flex items-center gap-3 mb-2">
                      <h3 className="text-lg font-semibold text-gray-900">{season.title}</h3>
                      <span
                        className={`px-2 py-1 rounded text-xs font-medium ${
                          SEASON_STATUS_COLORS[season.status]
                        }`}
                      >
                        {SEASON_STATUS_LABELS[season.status]}
                      </span>
                      {season.currentPhase && (
                        <span className="text-sm text-gray-500">({season.currentPhase})</span>
                      )}
                    </div>
                    {season.description && (
                      <p className="text-gray-600 mb-3">{season.description}</p>
                    )}
                    <div className="grid grid-cols-2 md:grid-cols-4 gap-4 text-sm">
                      <div>
                        <p className="text-gray-500">모집 기간</p>
                        <p className="text-gray-900">
                          {new Date(season.recruitmentStartAt).toLocaleDateString('ko-KR')} ~{' '}
                          {new Date(season.recruitmentEndAt).toLocaleDateString('ko-KR')}
                        </p>
                      </div>
                      <div>
                        <p className="text-gray-500">프로젝트 기간</p>
                        <p className="text-gray-900">
                          {new Date(season.projectStartAt).toLocaleDateString('ko-KR')} ~{' '}
                          {new Date(season.projectEndAt).toLocaleDateString('ko-KR')}
                        </p>
                      </div>
                      <div>
                        <p className="text-gray-500">심사 기간</p>
                        <p className="text-gray-900">
                          {new Date(season.reviewStartAt).toLocaleDateString('ko-KR')} ~{' '}
                          {new Date(season.reviewEndAt).toLocaleDateString('ko-KR')}
                        </p>
                      </div>
                      <div>
                        <p className="text-gray-500">심사 비중</p>
                        <p className="text-gray-900">
                          현직자 {season.expertReviewWeight}% / 참여자 {season.candidateReviewWeight}%
                        </p>
                      </div>
                    </div>
                  </div>

                  <div className="flex items-center gap-2 ml-4">
                    {nextStatus && (
                      <button
                        onClick={() => handleStatusChange(season, nextStatus)}
                        className="btn-success flex items-center gap-1 text-sm"
                        title={`${SEASON_STATUS_LABELS[nextStatus]}(으)로 변경`}
                      >
                        {nextStatus === 'COMPLETED' ? (
                          <CheckCircle className="w-4 h-4" />
                        ) : (
                          <Play className="w-4 h-4" />
                        )}
                        {SEASON_STATUS_LABELS[nextStatus]}
                      </button>
                    )}
                    <button
                      onClick={() => openEditModal(season)}
                      className="btn-secondary p-2"
                      title="수정"
                    >
                      <Edit className="w-4 h-4" />
                    </button>
                    {season.status === 'DRAFT' && (
                      <button
                        onClick={() => handleDelete(season)}
                        className="btn-danger p-2"
                        title="삭제"
                      >
                        <Trash2 className="w-4 h-4" />
                      </button>
                    )}
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/* Create/Edit Modal */}
      {showModal && (
        <div className="fixed inset-0 bg-gray-900/50 flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-xl shadow-xl w-full max-w-2xl max-h-[90vh] overflow-y-auto">
            <div className="sticky top-0 bg-white border-b border-gray-200 px-6 py-4">
              <h2 className="text-xl font-semibold text-gray-900">
                {editingSeason ? '시즌 수정' : '새 시즌 만들기'}
              </h2>
            </div>

            <form onSubmit={handleSubmit} className="p-6 space-y-6">
              {/* Basic Info */}
              <div className="space-y-4">
                <h3 className="font-medium text-gray-900">기본 정보</h3>
                <div>
                  <label className="label">시즌 제목 *</label>
                  <input
                    type="text"
                    value={formData.title}
                    onChange={(e) => setFormData({ ...formData, title: e.target.value })}
                    className="input"
                    placeholder="예: 2024 봄 시즌"
                    required
                  />
                </div>
                <div>
                  <label className="label">설명</label>
                  <textarea
                    value={formData.description}
                    onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                    className="input"
                    rows={3}
                    placeholder="시즌에 대한 설명을 입력하세요"
                  />
                </div>
              </div>

              {/* Dates */}
              <div className="space-y-4">
                <h3 className="font-medium text-gray-900">기간 설정</h3>
                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <label className="label">모집 시작일 *</label>
                    <input
                      type="date"
                      value={formData.recruitmentStartAt}
                      onChange={(e) =>
                        setFormData({ ...formData, recruitmentStartAt: e.target.value })
                      }
                      className="input"
                      required
                    />
                  </div>
                  <div>
                    <label className="label">모집 종료일 *</label>
                    <input
                      type="date"
                      value={formData.recruitmentEndAt}
                      onChange={(e) =>
                        setFormData({ ...formData, recruitmentEndAt: e.target.value })
                      }
                      className="input"
                      required
                    />
                  </div>
                  <div>
                    <label className="label">팀빌딩 시작일 *</label>
                    <input
                      type="date"
                      value={formData.teamBuildingStartAt}
                      onChange={(e) =>
                        setFormData({ ...formData, teamBuildingStartAt: e.target.value })
                      }
                      className="input"
                      required
                    />
                  </div>
                  <div>
                    <label className="label">팀빌딩 종료일 *</label>
                    <input
                      type="date"
                      value={formData.teamBuildingEndAt}
                      onChange={(e) =>
                        setFormData({ ...formData, teamBuildingEndAt: e.target.value })
                      }
                      className="input"
                      required
                    />
                  </div>
                  <div>
                    <label className="label">프로젝트 시작일 *</label>
                    <input
                      type="date"
                      value={formData.projectStartAt}
                      onChange={(e) =>
                        setFormData({ ...formData, projectStartAt: e.target.value })
                      }
                      className="input"
                      required
                    />
                  </div>
                  <div>
                    <label className="label">프로젝트 종료일 *</label>
                    <input
                      type="date"
                      value={formData.projectEndAt}
                      onChange={(e) =>
                        setFormData({ ...formData, projectEndAt: e.target.value })
                      }
                      className="input"
                      required
                    />
                  </div>
                  <div>
                    <label className="label">심사 시작일 *</label>
                    <input
                      type="date"
                      value={formData.reviewStartAt}
                      onChange={(e) =>
                        setFormData({ ...formData, reviewStartAt: e.target.value })
                      }
                      className="input"
                      required
                    />
                  </div>
                  <div>
                    <label className="label">심사 종료일 *</label>
                    <input
                      type="date"
                      value={formData.reviewEndAt}
                      onChange={(e) =>
                        setFormData({ ...formData, reviewEndAt: e.target.value })
                      }
                      className="input"
                      required
                    />
                  </div>
                </div>
              </div>

              {/* Review Weight */}
              <div className="space-y-4">
                <h3 className="font-medium text-gray-900">심사 비중</h3>
                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <label className="label">현직자 심사 비중 (%) *</label>
                    <input
                      type="number"
                      value={formData.expertReviewWeight}
                      onChange={(e) => {
                        const expert = parseInt(e.target.value);
                        setFormData({
                          ...formData,
                          expertReviewWeight: expert,
                          candidateReviewWeight: 100 - expert,
                        });
                      }}
                      className="input"
                      min={0}
                      max={100}
                      required
                    />
                  </div>
                  <div>
                    <label className="label">예비 참여자 심사 비중 (%) *</label>
                    <input
                      type="number"
                      value={formData.candidateReviewWeight}
                      onChange={(e) => {
                        const candidate = parseInt(e.target.value);
                        setFormData({
                          ...formData,
                          candidateReviewWeight: candidate,
                          expertReviewWeight: 100 - candidate,
                        });
                      }}
                      className="input"
                      min={0}
                      max={100}
                      required
                    />
                  </div>
                </div>
              </div>

              {/* Actions */}
              <div className="flex justify-end gap-3 pt-4 border-t border-gray-200">
                <button
                  type="button"
                  onClick={() => setShowModal(false)}
                  className="btn-secondary"
                >
                  취소
                </button>
                <button type="submit" disabled={isSubmitting} className="btn-primary">
                  {isSubmitting ? '저장 중...' : editingSeason ? '수정' : '생성'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
