import { useState, useEffect } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import { ChevronLeft, Plus, Users, Search } from 'lucide-react';
import { teamService, TEAM_STATUS_LABELS, TEAM_STATUS_COLORS, JOB_ROLE_LABELS, JOB_ROLE_COLORS } from '../services/teamService';
import { seasonService, type Season } from '../services/seasonService';
import { useAuthStore } from '../stores/authStore';
import type { Team, JobRole } from '../types';

export default function TeamsPage() {
  const { seasonId } = useParams<{ seasonId: string }>();
  const navigate = useNavigate();
  const { isAuthenticated, user } = useAuthStore();
  const [season, setSeason] = useState<Season | null>(null);
  const [teams, setTeams] = useState<Team[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState('');
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [filter, setFilter] = useState<'all' | 'recruiting'>('all');
  const [roleFilter, setRoleFilter] = useState<JobRole | 'ALL'>('ALL');

  useEffect(() => {
    if (seasonId) {
      loadData(Number(seasonId));
    }
  }, [seasonId, filter]);

  const loadData = async (id: number) => {
    setIsLoading(true);
    try {
      const [seasonData, teamsData] = await Promise.all([
        seasonService.getSeason(id),
        filter === 'recruiting'
          ? teamService.getRecruitingTeams(id)
          : teamService.getTeamsBySeason(id),
      ]);
      setSeason(seasonData);
      setTeams(teamsData);
    } catch (err) {
      console.error('Failed to load data:', err);
      setError('데이터를 불러오는데 실패했습니다.');
    } finally {
      setIsLoading(false);
    }
  };

  const filteredTeams = teams.filter((team) => {
    if (roleFilter === 'ALL') return true;
    switch (roleFilter) {
      case 'PLANNER':
        return team.recruitingPlanner;
      case 'UXUI':
        return team.recruitingUxui;
      case 'FRONTEND':
        return team.recruitingFrontend;
      case 'BACKEND':
        return team.recruitingBackend;
      default:
        return true;
    }
  });

  const getRecruitingRoles = (team: Team): JobRole[] => {
    const roles: JobRole[] = [];
    if (team.recruitingPlanner) roles.push('PLANNER');
    if (team.recruitingUxui) roles.push('UXUI');
    if (team.recruitingFrontend) roles.push('FRONTEND');
    if (team.recruitingBackend) roles.push('BACKEND');
    return roles;
  };

  if (isLoading) {
    return (
      <div className="flex items-center justify-center min-h-[50vh]">
        <div className="text-neutral-500">로딩 중...</div>
      </div>
    );
  }

  if (error || !season) {
    return (
      <div className="max-w-3xl mx-auto px-4 py-12">
        <div className="text-center">
          <p className="text-red-500 mb-4">{error || '시즌을 찾을 수 없습니다.'}</p>
          <Link to="/seasons" className="btn-secondary">
            시즌 목록으로
          </Link>
        </div>
      </div>
    );
  }

  return (
    <div className="max-w-6xl mx-auto px-4 py-12">
      <Link
        to={`/seasons/${seasonId}`}
        className="inline-flex items-center gap-2 text-neutral-600 hover:text-neutral-900 mb-6"
      >
        <ChevronLeft className="w-5 h-5" />
        {season.title}
      </Link>

      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 mb-8">
        <div>
          <h1 className="text-2xl font-bold text-neutral-900">팀 찾기</h1>
          <p className="text-neutral-600 mt-1">함께 프로젝트를 진행할 팀을 찾아보세요</p>
        </div>
        {isAuthenticated && user?.certificationStatus === 'APPROVED' && (
          <button onClick={() => setShowCreateModal(true)} className="btn-primary flex items-center gap-2">
            <Plus className="w-5 h-5" />
            팀 만들기
          </button>
        )}
      </div>

      {/* 필터 */}
      <div className="card mb-6">
        <div className="flex flex-col sm:flex-row gap-4">
          <div className="flex gap-2">
            <button
              onClick={() => setFilter('all')}
              className={`px-4 py-2 rounded-lg text-sm font-medium transition-colors ${
                filter === 'all'
                  ? 'bg-primary-500 text-white'
                  : 'bg-neutral-100 text-neutral-600 hover:bg-neutral-200'
              }`}
            >
              전체
            </button>
            <button
              onClick={() => setFilter('recruiting')}
              className={`px-4 py-2 rounded-lg text-sm font-medium transition-colors ${
                filter === 'recruiting'
                  ? 'bg-primary-500 text-white'
                  : 'bg-neutral-100 text-neutral-600 hover:bg-neutral-200'
              }`}
            >
              모집 중
            </button>
          </div>

          <div className="flex gap-2 flex-wrap">
            <button
              onClick={() => setRoleFilter('ALL')}
              className={`px-3 py-1.5 rounded-lg text-sm font-medium transition-colors ${
                roleFilter === 'ALL'
                  ? 'bg-neutral-800 text-white'
                  : 'bg-neutral-100 text-neutral-600 hover:bg-neutral-200'
              }`}
            >
              전체 역할
            </button>
            {(['PLANNER', 'UXUI', 'FRONTEND', 'BACKEND'] as JobRole[]).map((role) => (
              <button
                key={role}
                onClick={() => setRoleFilter(role)}
                className={`px-3 py-1.5 rounded-lg text-sm font-medium transition-colors ${
                  roleFilter === role ? JOB_ROLE_COLORS[role] : 'bg-neutral-100 text-neutral-600 hover:bg-neutral-200'
                }`}
              >
                {JOB_ROLE_LABELS[role]}
              </button>
            ))}
          </div>
        </div>
      </div>

      {/* 팀 목록 */}
      {filteredTeams.length === 0 ? (
        <div className="card text-center py-12">
          <Search className="w-12 h-12 text-neutral-300 mx-auto mb-4" />
          <p className="text-neutral-500 mb-2">
            {filter === 'recruiting' ? '모집 중인 팀이 없습니다.' : '등록된 팀이 없습니다.'}
          </p>
          {isAuthenticated && user?.certificationStatus === 'APPROVED' && (
            <p className="text-neutral-400 text-sm">직접 팀을 만들어보세요!</p>
          )}
        </div>
      ) : (
        <div className="grid gap-4 md:grid-cols-2">
          {filteredTeams.map((team) => (
            <Link key={team.id} to={`/teams/${team.id}`} className="card hover:shadow-lg transition-shadow">
              <div className="flex items-start justify-between mb-3">
                <h3 className="text-lg font-semibold text-neutral-900">{team.name}</h3>
                <span className={`px-2 py-1 rounded-full text-xs font-medium ${TEAM_STATUS_COLORS[team.status]}`}>
                  {TEAM_STATUS_LABELS[team.status]}
                </span>
              </div>

              {team.description && (
                <p className="text-neutral-600 text-sm mb-4 line-clamp-2">{team.description}</p>
              )}

              <div className="flex items-center gap-4 text-sm text-neutral-500 mb-4">
                <span className="flex items-center gap-1">
                  <Users className="w-4 h-4" />
                  {team.memberCount}/{team.maxMemberCount}명
                </span>
                <span>리더: {team.leaderName}</span>
              </div>

              {getRecruitingRoles(team).length > 0 && (
                <div className="flex flex-wrap gap-2">
                  <span className="text-xs text-neutral-500">모집 중:</span>
                  {getRecruitingRoles(team).map((role) => (
                    <span key={role} className={`px-2 py-0.5 rounded text-xs font-medium ${JOB_ROLE_COLORS[role]}`}>
                      {JOB_ROLE_LABELS[role]}
                    </span>
                  ))}
                </div>
              )}
            </Link>
          ))}
        </div>
      )}

      {/* 팀 생성 모달 */}
      {showCreateModal && (
        <CreateTeamModal
          seasonId={Number(seasonId)}
          onClose={() => setShowCreateModal(false)}
          onCreated={(teamId) => {
            setShowCreateModal(false);
            navigate(`/teams/${teamId}`);
          }}
        />
      )}
    </div>
  );
}

interface CreateTeamModalProps {
  seasonId: number;
  onClose: () => void;
  onCreated: (teamId: number) => void;
}

function CreateTeamModal({ seasonId, onClose, onCreated }: CreateTeamModalProps) {
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [leaderRole, setLeaderRole] = useState<JobRole | ''>('');
  const [recruitingPlanner, setRecruitingPlanner] = useState(false);
  const [recruitingUxui, setRecruitingUxui] = useState(false);
  const [recruitingFrontend, setRecruitingFrontend] = useState(false);
  const [recruitingBackend, setRecruitingBackend] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState('');

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!name.trim()) {
      setError('팀 이름을 입력해주세요.');
      return;
    }
    if (!leaderRole) {
      setError('본인의 역할을 선택해주세요.');
      return;
    }

    setIsSubmitting(true);
    setError('');

    try {
      const team = await teamService.createTeam({
        name: name.trim(),
        description: description.trim() || undefined,
        seasonId,
        leaderRole,
        recruitingPlanner,
        recruitingUxui,
        recruitingFrontend,
        recruitingBackend,
      });
      onCreated(team.id);
    } catch (err: unknown) {
      console.error('Failed to create team:', err);
      const error = err as { response?: { data?: { message?: string } } };
      setError(error.response?.data?.message || '팀 생성에 실패했습니다.');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-2xl max-w-lg w-full max-h-[90vh] overflow-y-auto">
        <div className="p-6">
          <h2 className="text-xl font-bold text-neutral-900 mb-6">팀 만들기</h2>

          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-neutral-700 mb-1">
                팀 이름 <span className="text-red-500">*</span>
              </label>
              <input
                type="text"
                value={name}
                onChange={(e) => setName(e.target.value)}
                className="input-field"
                placeholder="팀 이름을 입력하세요"
                maxLength={50}
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-neutral-700 mb-1">팀 소개</label>
              <textarea
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                className="input-field min-h-[100px]"
                placeholder="팀의 목표나 프로젝트 아이디어를 소개해주세요"
                maxLength={2000}
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-neutral-700 mb-2">
                본인 역할 <span className="text-red-500">*</span>
              </label>
              <p className="text-xs text-neutral-500 mb-3">팀에서 맡을 본인의 역할을 선택해주세요</p>
              <div className="grid grid-cols-2 gap-3">
                {(['PLANNER', 'UXUI', 'FRONTEND', 'BACKEND'] as JobRole[]).map((role) => (
                  <label
                    key={role}
                    className={`flex items-center gap-2 p-3 rounded-lg border cursor-pointer transition-colors ${
                      leaderRole === role
                        ? 'border-primary-500 bg-primary-50'
                        : 'border-neutral-200 hover:bg-neutral-50'
                    }`}
                  >
                    <input
                      type="radio"
                      name="leaderRole"
                      value={role}
                      checked={leaderRole === role}
                      onChange={(e) => setLeaderRole(e.target.value as JobRole)}
                      className="w-4 h-4 text-primary-500"
                    />
                    <span className="text-sm">{JOB_ROLE_LABELS[role]}</span>
                  </label>
                ))}
              </div>
            </div>

            <div>
              <label className="block text-sm font-medium text-neutral-700 mb-3">모집할 역할</label>
              <div className="grid grid-cols-2 gap-3">
                <label className="flex items-center gap-2 p-3 rounded-lg border border-neutral-200 cursor-pointer hover:bg-neutral-50">
                  <input
                    type="checkbox"
                    checked={recruitingPlanner}
                    onChange={(e) => setRecruitingPlanner(e.target.checked)}
                    className="w-4 h-4 text-primary-500"
                  />
                  <span className="text-sm">기획자</span>
                </label>
                <label className="flex items-center gap-2 p-3 rounded-lg border border-neutral-200 cursor-pointer hover:bg-neutral-50">
                  <input
                    type="checkbox"
                    checked={recruitingUxui}
                    onChange={(e) => setRecruitingUxui(e.target.checked)}
                    className="w-4 h-4 text-primary-500"
                  />
                  <span className="text-sm">UX/UI 디자이너</span>
                </label>
                <label className="flex items-center gap-2 p-3 rounded-lg border border-neutral-200 cursor-pointer hover:bg-neutral-50">
                  <input
                    type="checkbox"
                    checked={recruitingFrontend}
                    onChange={(e) => setRecruitingFrontend(e.target.checked)}
                    className="w-4 h-4 text-primary-500"
                  />
                  <span className="text-sm">프론트엔드</span>
                </label>
                <label className="flex items-center gap-2 p-3 rounded-lg border border-neutral-200 cursor-pointer hover:bg-neutral-50">
                  <input
                    type="checkbox"
                    checked={recruitingBackend}
                    onChange={(e) => setRecruitingBackend(e.target.checked)}
                    className="w-4 h-4 text-primary-500"
                  />
                  <span className="text-sm">백엔드</span>
                </label>
              </div>
            </div>

            {error && <p className="text-red-500 text-sm">{error}</p>}

            <div className="flex gap-3 pt-4">
              <button type="button" onClick={onClose} className="btn-secondary flex-1" disabled={isSubmitting}>
                취소
              </button>
              <button type="submit" className="btn-primary flex-1" disabled={isSubmitting}>
                {isSubmitting ? '생성 중...' : '팀 만들기'}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
}
