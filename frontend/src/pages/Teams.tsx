import { useState, useEffect } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import { ChevronLeft, Plus, Users, Search, Sparkles, X, CheckCircle, AlertTriangle, Clock } from 'lucide-react';
import { teamService, TEAM_STATUS_LABELS, TEAM_STATUS_COLORS, JOB_ROLE_LABELS, JOB_ROLE_COLORS } from '../services/teamService';
import { seasonService, type Season } from '../services/seasonService';
import { matchingService, SCHEDULE_RISK_LABELS, SCHEDULE_RISK_COLORS } from '../services/matchingService';
import { useAuthStore } from '../stores/authStore';
import type { Team, JobRole, TeamRecommendation } from '../types';

export default function TeamsPage() {
  const { seasonId } = useParams<{ seasonId: string }>();
  const navigate = useNavigate();
  const { isAuthenticated, user } = useAuthStore();
  const [season, setSeason] = useState<Season | null>(null);
  const [seasons, setSeasons] = useState<Season[]>([]);
  const [selectedSeasonId, setSelectedSeasonId] = useState<number | null>(seasonId ? Number(seasonId) : null);
  const [teams, setTeams] = useState<Team[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState('');
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [showAIRecommendation, setShowAIRecommendation] = useState(false);
  const [filter, setFilter] = useState<'all' | 'recruiting'>('all');
  const [roleFilter, setRoleFilter] = useState<JobRole | 'ALL'>('ALL');

  // seasonId가 URL에 없을 때 활성 시즌 목록을 가져옴
  useEffect(() => {
    if (!seasonId) {
      loadActiveSeasons();
    } else {
      setSelectedSeasonId(Number(seasonId));
    }
  }, [seasonId]);

  useEffect(() => {
    if (selectedSeasonId) {
      loadData(selectedSeasonId);
    }
  }, [selectedSeasonId, filter]);

  const loadActiveSeasons = async () => {
    setIsLoading(true);
    try {
      const activeSeasons = await seasonService.getActiveSeasons();
      setSeasons(activeSeasons);
      // 활성 시즌이 있으면 첫 번째 시즌을 선택
      if (activeSeasons.length > 0) {
        setSelectedSeasonId(activeSeasons[0].id);
      } else {
        setIsLoading(false);
      }
    } catch (err) {
      console.error('Failed to load active seasons:', err);
      setError('시즌 정보를 불러오는데 실패했습니다.');
      setIsLoading(false);
    }
  };

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

  // 활성 시즌이 없는 경우
  if (!isLoading && !seasonId && seasons.length === 0 && !season) {
    return (
      <div className="max-w-3xl mx-auto px-4 py-12">
        <div className="text-center">
          <Users className="w-16 h-16 text-neutral-300 mx-auto mb-4" />
          <h2 className="text-xl font-bold text-neutral-900 mb-2">현재 진행 중인 시즌이 없습니다</h2>
          <p className="text-neutral-500 mb-6">새로운 시즌이 시작되면 팀을 찾을 수 있습니다.</p>
          <Link to="/seasons" className="btn-secondary">
            시즌 목록 보기
          </Link>
        </div>
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
      {seasonId ? (
        <Link
          to={`/seasons/${seasonId}`}
          className="inline-flex items-center gap-2 text-neutral-600 hover:text-neutral-900 mb-6"
        >
          <ChevronLeft className="w-5 h-5" />
          {season.title}
        </Link>
      ) : (
        <Link
          to="/seasons"
          className="inline-flex items-center gap-2 text-neutral-600 hover:text-neutral-900 mb-6"
        >
          <ChevronLeft className="w-5 h-5" />
          시즌 목록
        </Link>
      )}

      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 mb-8">
        <div>
          <h1 className="text-2xl font-bold text-neutral-900">팀 찾기</h1>
          <p className="text-neutral-600 mt-1">함께 프로젝트를 진행할 팀을 찾아보세요</p>
          {/* 시즌 선택 드롭다운 (URL에 seasonId가 없을 때만) */}
          {!seasonId && seasons.length > 1 && (
            <select
              value={selectedSeasonId || ''}
              onChange={(e) => setSelectedSeasonId(Number(e.target.value))}
              className="mt-2 px-3 py-1.5 border border-neutral-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-primary-500"
            >
              {seasons.map((s) => (
                <option key={s.id} value={s.id}>
                  {s.title}
                </option>
              ))}
            </select>
          )}
          {/* 현재 선택된 시즌 표시 */}
          {!seasonId && season && seasons.length <= 1 && (
            <p className="text-sm text-primary-600 mt-1">{season.title}</p>
          )}
        </div>
        <div className="flex gap-3">
          {isAuthenticated && user?.certificationStatus === 'APPROVED' && (
            <>
              <button
                onClick={() => setShowAIRecommendation(true)}
                className="btn-secondary flex items-center gap-2"
              >
                <Sparkles className="w-5 h-5" />
                AI 추천
              </button>
              <button onClick={() => setShowCreateModal(true)} className="btn-primary flex items-center gap-2">
                <Plus className="w-5 h-5" />
                팀 만들기
              </button>
            </>
          )}
        </div>
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
      {showCreateModal && selectedSeasonId && (
        <CreateTeamModal
          seasonId={selectedSeasonId}
          onClose={() => setShowCreateModal(false)}
          onCreated={(teamId) => {
            setShowCreateModal(false);
            navigate(`/teams/${teamId}`);
          }}
        />
      )}

      {/* AI 추천 모달 */}
      {showAIRecommendation && selectedSeasonId && (
        <AIRecommendationModal
          seasonId={selectedSeasonId}
          onClose={() => setShowAIRecommendation(false)}
          onSelectTeam={(teamId) => {
            setShowAIRecommendation(false);
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

interface AIRecommendationModalProps {
  seasonId: number;
  onClose: () => void;
  onSelectTeam: (teamId: number) => void;
}

function AIRecommendationModal({ seasonId, onClose, onSelectTeam }: AIRecommendationModalProps) {
  const [recommendations, setRecommendations] = useState<TeamRecommendation[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    loadRecommendations();
  }, [seasonId]);

  const loadRecommendations = async () => {
    setIsLoading(true);
    setError('');
    try {
      const data = await matchingService.getTeamRecommendations(seasonId, 5);
      setRecommendations(data);
    } catch (err: unknown) {
      console.error('Failed to load recommendations:', err);
      const error = err as { response?: { data?: { message?: string; errorCode?: string } } };
      if (error.response?.data?.errorCode === 'PROFILE_001') {
        setError('프로필을 먼저 등록해주세요. AI가 당신에게 맞는 팀을 추천해드립니다.');
      } else if (error.response?.data?.errorCode === 'TEAM_003') {
        setError('이미 팀에 소속되어 있습니다.');
      } else if (error.response?.data?.errorCode === 'AI_002') {
        setError('현재 추천 가능한 팀이 없습니다. 나중에 다시 시도해주세요.');
      } else {
        setError(error.response?.data?.message || 'AI 추천을 불러오는데 실패했습니다.');
      }
    } finally {
      setIsLoading(false);
    }
  };

  const getScoreColor = (score: number) => {
    if (score >= 80) return 'text-green-600';
    if (score >= 60) return 'text-yellow-600';
    return 'text-red-600';
  };

  const getScoreBgColor = (score: number) => {
    if (score >= 80) return 'bg-green-100';
    if (score >= 60) return 'bg-yellow-100';
    return 'bg-red-100';
  };

  const getRiskIcon = (risk: string) => {
    switch (risk) {
      case 'LOW':
        return <CheckCircle className="w-4 h-4 text-green-600" />;
      case 'MEDIUM':
        return <Clock className="w-4 h-4 text-yellow-600" />;
      case 'HIGH':
        return <AlertTriangle className="w-4 h-4 text-red-600" />;
      default:
        return null;
    }
  };

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-2xl max-w-2xl w-full max-h-[90vh] overflow-hidden flex flex-col">
        <div className="p-6 border-b border-neutral-200">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 bg-primary-100 rounded-full flex items-center justify-center">
                <Sparkles className="w-5 h-5 text-primary-600" />
              </div>
              <div>
                <h2 className="text-xl font-bold text-neutral-900">AI 팀 추천</h2>
                <p className="text-sm text-neutral-500">당신의 프로필에 맞는 팀을 찾아드립니다</p>
              </div>
            </div>
            <button onClick={onClose} className="text-neutral-400 hover:text-neutral-600">
              <X className="w-6 h-6" />
            </button>
          </div>
        </div>

        <div className="flex-1 overflow-y-auto p-6">
          {isLoading ? (
            <div className="flex flex-col items-center justify-center py-12">
              <div className="w-12 h-12 border-4 border-primary-200 border-t-primary-500 rounded-full animate-spin mb-4" />
              <p className="text-neutral-500">AI가 최적의 팀을 분석 중입니다...</p>
            </div>
          ) : error ? (
            <div className="text-center py-12">
              <p className="text-neutral-600 mb-4">{error}</p>
              <button onClick={loadRecommendations} className="btn-secondary">
                다시 시도
              </button>
            </div>
          ) : recommendations.length === 0 ? (
            <div className="text-center py-12">
              <p className="text-neutral-500">추천 가능한 팀이 없습니다.</p>
            </div>
          ) : (
            <div className="space-y-4">
              {recommendations.map((rec, index) => (
                <div
                  key={rec.team.id}
                  className="border border-neutral-200 rounded-xl p-4 hover:border-primary-300 hover:shadow-md transition-all cursor-pointer"
                  onClick={() => onSelectTeam(rec.team.id)}
                >
                  <div className="flex items-start gap-4">
                    {/* 순위 표시 */}
                    <div className={`w-8 h-8 rounded-full flex items-center justify-center text-sm font-bold ${getScoreBgColor(rec.matchScore)} ${getScoreColor(rec.matchScore)}`}>
                      {index + 1}
                    </div>

                    <div className="flex-1">
                      {/* 팀 기본 정보 */}
                      <div className="flex items-start justify-between mb-2">
                        <div>
                          <h3 className="font-semibold text-neutral-900">{rec.team.name}</h3>
                          <p className="text-sm text-neutral-500">
                            리더: {rec.team.leaderName} | {rec.team.memberCount}/{rec.team.maxMemberCount}명
                          </p>
                        </div>
                        <div className={`px-3 py-1 rounded-full text-sm font-bold ${getScoreBgColor(rec.matchScore)} ${getScoreColor(rec.matchScore)}`}>
                          {rec.matchScore}점
                        </div>
                      </div>

                      {/* 추천 이유 */}
                      <div className="mb-3">
                        <p className="text-xs font-medium text-neutral-500 mb-1">추천 이유</p>
                        <ul className="text-sm text-neutral-700 space-y-1">
                          {rec.reasons.slice(0, 3).map((reason, idx) => (
                            <li key={idx} className="flex items-start gap-2">
                              <span className="text-primary-500">•</span>
                              {reason}
                            </li>
                          ))}
                        </ul>
                      </div>

                      {/* 팀 밸런스 분석 */}
                      <div className="bg-neutral-50 rounded-lg p-3 mb-3">
                        <p className="text-xs font-medium text-neutral-500 mb-1">팀 밸런스 분석</p>
                        <p className="text-sm text-neutral-700">{rec.balanceAnalysis}</p>
                      </div>

                      {/* 일정 위험도 & 부족한 역할 */}
                      <div className="flex flex-wrap gap-2 items-center">
                        <div className={`flex items-center gap-1 px-2 py-1 rounded text-xs ${SCHEDULE_RISK_COLORS[rec.scheduleRisk]}`}>
                          {getRiskIcon(rec.scheduleRisk)}
                          일정 충돌 위험: {SCHEDULE_RISK_LABELS[rec.scheduleRisk]}
                        </div>
                        {rec.missingRoles && rec.missingRoles.length > 0 && (
                          <div className="text-xs text-neutral-500">
                            부족한 역할: {rec.missingRoles.join(', ')}
                          </div>
                        )}
                      </div>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>

        <div className="p-6 border-t border-neutral-200">
          <button onClick={onClose} className="btn-secondary w-full">
            닫기
          </button>
        </div>
      </div>
    </div>
  );
}
