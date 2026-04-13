import { useState, useEffect } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import { ChevronLeft, Users, Settings, Check, X as XIcon, Clock, UserPlus, Sparkles, CheckCircle, AlertTriangle, FolderKanban, Mail, Loader2 } from 'lucide-react';
import { teamService, TEAM_STATUS_LABELS, TEAM_STATUS_COLORS, JOB_ROLE_LABELS, JOB_ROLE_COLORS } from '../services/teamService';
import { matchingService, SCHEDULE_RISK_LABELS, SCHEDULE_RISK_COLORS } from '../services/matchingService';
import { invitationService } from '../services/invitationService';
import { useAuthStore } from '../stores/authStore';
import type { Team, TeamMember, JobRole, MemberRecommendation } from '../types';

export default function TeamDetailPage() {
  const { teamId } = useParams<{ teamId: string }>();
  const navigate = useNavigate();
  const { isAuthenticated, user } = useAuthStore();
  const [team, setTeam] = useState<Team | null>(null);
  const [applications, setApplications] = useState<TeamMember[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState('');
  const [showApplyModal, setShowApplyModal] = useState(false);
  const [showAIRecommendation, setShowAIRecommendation] = useState(false);
  const [activeTab, setActiveTab] = useState<'info' | 'members' | 'applications'>('info');

  const isLeader = user && team && user.id === team.leaderId;

  useEffect(() => {
    if (teamId) {
      loadTeam(Number(teamId));
    }
  }, [teamId]);

  const loadTeam = async (id: number) => {
    setIsLoading(true);
    try {
      const teamData = await teamService.getTeam(id);
      setTeam(teamData);

      // 리더인 경우 지원 목록도 조회
      if (user && teamData.leaderId === user.id) {
        const apps = await teamService.getTeamApplications(id);
        setApplications(apps);
      }
    } catch (err) {
      console.error('Failed to load team:', err);
      setError('팀 정보를 불러오는데 실패했습니다.');
    } finally {
      setIsLoading(false);
    }
  };

  const handleAcceptApplication = async (memberId: number) => {
    if (!teamId) return;
    try {
      await teamService.acceptApplication(Number(teamId), memberId);
      // 팀 정보와 지원 목록 다시 로드
      await loadTeam(Number(teamId));
    } catch (err: unknown) {
      console.error('Failed to accept application:', err);
      const error = err as { response?: { data?: { message?: string } } };
      alert(error.response?.data?.message || '지원 수락에 실패했습니다.');
    }
  };

  const handleRejectApplication = async (memberId: number) => {
    if (!teamId) return;
    try {
      await teamService.rejectApplication(Number(teamId), memberId);
      setApplications(applications.filter((a) => a.id !== memberId));
    } catch (err: unknown) {
      console.error('Failed to reject application:', err);
      const error = err as { response?: { data?: { message?: string } } };
      alert(error.response?.data?.message || '지원 거절에 실패했습니다.');
    }
  };

  const getRecruitingRoles = (team: Team): JobRole[] => {
    const roles: JobRole[] = [];
    if (team.recruitingPlanner) roles.push('PLANNER');
    if (team.recruitingUxui) roles.push('UXUI');
    if (team.recruitingFrontend) roles.push('FRONTEND');
    if (team.recruitingBackend) roles.push('BACKEND');
    return roles;
  };

  const canApply = () => {
    if (!isAuthenticated || !user || !team) return false;
    if (user.certificationStatus !== 'APPROVED') return false;
    if (team.status !== 'RECRUITING') return false;
    if (team.leaderId === user.id) return false;
    // 이미 멤버인지 확인
    if (team.members?.some((m) => m.userId === user.id)) return false;
    return getRecruitingRoles(team).length > 0;
  };

  if (isLoading) {
    return (
      <div className="flex items-center justify-center min-h-[50vh]">
        <div className="text-neutral-500">로딩 중...</div>
      </div>
    );
  }

  if (error || !team) {
    return (
      <div className="max-w-3xl mx-auto px-4 py-12">
        <div className="text-center">
          <p className="text-red-500 mb-4">{error || '팀을 찾을 수 없습니다.'}</p>
          <Link to="/seasons" className="btn-secondary">
            시즌 목록으로
          </Link>
        </div>
      </div>
    );
  }

  return (
    <div className="max-w-4xl mx-auto px-4 py-12">
      <Link
        to={`/seasons/${team.seasonId}/teams`}
        className="inline-flex items-center gap-2 text-neutral-600 hover:text-neutral-900 mb-6"
      >
        <ChevronLeft className="w-5 h-5" />
        팀 목록
      </Link>

      {/* 팀 헤더 */}
      <div className="card mb-6">
        <div className="flex items-start justify-between mb-4">
          <div>
            <div className="flex items-center gap-3 mb-2">
              <h1 className="text-2xl font-bold text-neutral-900">{team.name}</h1>
              <span className={`px-3 py-1 rounded-full text-sm font-medium ${TEAM_STATUS_COLORS[team.status]}`}>
                {TEAM_STATUS_LABELS[team.status]}
              </span>
            </div>
            <p className="text-neutral-500">
              {team.seasonTitle} | 리더: {team.leaderName}
            </p>
          </div>

          {isLeader && (
            <div className="flex gap-2">
              {team.status === 'RECRUITING' && getRecruitingRoles(team).length > 0 && (
                <button
                  onClick={() => setShowAIRecommendation(true)}
                  className="btn-secondary flex items-center gap-2"
                >
                  <Sparkles className="w-4 h-4" />
                  AI 추천
                </button>
              )}
              <button className="btn-secondary flex items-center gap-2">
                <Settings className="w-4 h-4" />
                팀 설정
              </button>
            </div>
          )}
        </div>

        <div className="flex items-center gap-6 text-sm text-neutral-600 mb-4">
          <span className="flex items-center gap-1">
            <Users className="w-4 h-4" />
            {team.memberCount}/{team.maxMemberCount}명
          </span>
        </div>

        {team.description && <p className="text-neutral-600 whitespace-pre-wrap">{team.description}</p>}

        {getRecruitingRoles(team).length > 0 && (
          <div className="mt-4 pt-4 border-t border-neutral-100">
            <div className="flex items-center gap-2 flex-wrap">
              <span className="text-sm text-neutral-500">모집 중인 역할:</span>
              {getRecruitingRoles(team).map((role) => (
                <span key={role} className={`px-3 py-1 rounded-full text-sm font-medium ${JOB_ROLE_COLORS[role]}`}>
                  {JOB_ROLE_LABELS[role]}
                </span>
              ))}
            </div>
          </div>
        )}

        {/* 프로젝트 워크스페이스 버튼 - 팀원만 접근 가능 */}
        {(team.status === 'IN_PROGRESS' || team.status === 'COMPLETE' || team.status === 'SUBMITTED' || team.status === 'REVIEWED') &&
         team.members?.some((m) => m.userId === user?.id) && (
          <div className="mt-6">
            <Link
              to={`/teams/${team.id}/project`}
              className="btn-primary inline-flex items-center gap-2"
            >
              <FolderKanban className="w-5 h-5" />
              프로젝트 워크스페이스
            </Link>
          </div>
        )}

        {canApply() && (
          <div className="mt-6">
            <button onClick={() => setShowApplyModal(true)} className="btn-primary flex items-center gap-2">
              <UserPlus className="w-5 h-5" />
              팀에 지원하기
            </button>
          </div>
        )}

        {!isAuthenticated && team.status === 'RECRUITING' && (
          <div className="mt-4 bg-primary-50 border border-primary-200 rounded-xl p-4">
            <p className="text-primary-700">
              팀에 지원하려면 <Link to="/login" className="font-semibold underline">로그인</Link>이 필요합니다.
            </p>
          </div>
        )}
      </div>

      {/* 탭 */}
      <div className="flex gap-2 mb-6">
        <button
          onClick={() => setActiveTab('info')}
          className={`px-4 py-2 rounded-lg text-sm font-medium transition-colors ${
            activeTab === 'info' ? 'bg-primary-500 text-white' : 'bg-neutral-100 text-neutral-600 hover:bg-neutral-200'
          }`}
        >
          팀 정보
        </button>
        <button
          onClick={() => setActiveTab('members')}
          className={`px-4 py-2 rounded-lg text-sm font-medium transition-colors ${
            activeTab === 'members'
              ? 'bg-primary-500 text-white'
              : 'bg-neutral-100 text-neutral-600 hover:bg-neutral-200'
          }`}
        >
          팀원 ({team.memberCount})
        </button>
        {isLeader && (
          <button
            onClick={() => setActiveTab('applications')}
            className={`px-4 py-2 rounded-lg text-sm font-medium transition-colors ${
              activeTab === 'applications'
                ? 'bg-primary-500 text-white'
                : 'bg-neutral-100 text-neutral-600 hover:bg-neutral-200'
            }`}
          >
            지원자 ({applications.length})
          </button>
        )}
      </div>

      {/* 탭 컨텐츠 */}
      {activeTab === 'info' && (
        <div className="card">
          <h2 className="text-lg font-bold text-neutral-900 mb-4">팀 소개</h2>
          <p className="text-neutral-600 whitespace-pre-wrap">
            {team.description || '팀 소개가 아직 작성되지 않았습니다.'}
          </p>
        </div>
      )}

      {activeTab === 'members' && (
        <div className="card">
          <h2 className="text-lg font-bold text-neutral-900 mb-4">팀원</h2>
          {team.members && team.members.length > 0 ? (
            <div className="space-y-3">
              {team.members.map((member) => (
                <div key={member.id} className="flex items-center justify-between p-3 bg-neutral-50 rounded-lg">
                  <div className="flex items-center gap-3">
                    <div className="w-10 h-10 bg-primary-100 rounded-full flex items-center justify-center">
                      <span className="text-primary-600 font-semibold">{member.userName.charAt(0)}</span>
                    </div>
                    <div>
                      <div className="flex items-center gap-2">
                        <span className="font-medium text-neutral-900">{member.userName}</span>
                        {team.leaderId === member.userId && (
                          <span className="px-2 py-0.5 bg-primary-100 text-primary-700 text-xs rounded-full">리더</span>
                        )}
                      </div>
                      <span className={`text-xs ${JOB_ROLE_COLORS[member.role]} px-2 py-0.5 rounded`}>
                        {JOB_ROLE_LABELS[member.role]}
                      </span>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          ) : (
            <p className="text-neutral-500 text-center py-8">아직 팀원이 없습니다.</p>
          )}
        </div>
      )}

      {activeTab === 'applications' && isLeader && (
        <div className="card">
          <h2 className="text-lg font-bold text-neutral-900 mb-4">지원자 관리</h2>
          {applications.length > 0 ? (
            <div className="space-y-4">
              {applications.map((app) => (
                <div key={app.id} className="p-4 border border-neutral-200 rounded-xl">
                  <div className="flex items-start justify-between mb-3">
                    <div className="flex items-center gap-3">
                      <div className="w-10 h-10 bg-neutral-100 rounded-full flex items-center justify-center">
                        <span className="text-neutral-600 font-semibold">{app.userName.charAt(0)}</span>
                      </div>
                      <div>
                        <div className="font-medium text-neutral-900">{app.userName}</div>
                        <span className={`text-xs ${JOB_ROLE_COLORS[app.role]} px-2 py-0.5 rounded`}>
                          {JOB_ROLE_LABELS[app.role]}
                        </span>
                      </div>
                    </div>
                    <div className="flex items-center gap-2">
                      <span className="flex items-center gap-1 text-xs text-neutral-500">
                        <Clock className="w-3 h-3" />
                        {new Date(app.createdAt).toLocaleDateString('ko-KR')}
                      </span>
                    </div>
                  </div>

                  {app.applicationMessage && (
                    <div className="bg-neutral-50 rounded-lg p-3 mb-3">
                      <p className="text-sm text-neutral-600">{app.applicationMessage}</p>
                    </div>
                  )}

                  <div className="flex gap-2">
                    <button
                      onClick={() => handleAcceptApplication(app.id)}
                      className="btn-primary flex-1 flex items-center justify-center gap-2 text-sm"
                    >
                      <Check className="w-4 h-4" />
                      수락
                    </button>
                    <button
                      onClick={() => handleRejectApplication(app.id)}
                      className="btn-secondary flex-1 flex items-center justify-center gap-2 text-sm"
                    >
                      <XIcon className="w-4 h-4" />
                      거절
                    </button>
                  </div>
                </div>
              ))}
            </div>
          ) : (
            <p className="text-neutral-500 text-center py-8">아직 지원자가 없습니다.</p>
          )}
        </div>
      )}

      {/* 지원 모달 */}
      {showApplyModal && team && (
        <ApplyModal
          teamId={team.id}
          recruitingRoles={getRecruitingRoles(team)}
          onClose={() => setShowApplyModal(false)}
          onApplied={() => {
            setShowApplyModal(false);
            navigate(`/seasons/${team.seasonId}/teams`);
          }}
        />
      )}

      {/* AI 멤버 추천 모달 */}
      {showAIRecommendation && team && isLeader && (
        <AIMemberRecommendationModal
          teamId={team.id}
          onClose={() => setShowAIRecommendation(false)}
        />
      )}
    </div>
  );
}

interface ApplyModalProps {
  teamId: number;
  recruitingRoles: JobRole[];
  onClose: () => void;
  onApplied: () => void;
}

function ApplyModal({ teamId, recruitingRoles, onClose, onApplied }: ApplyModalProps) {
  const [role, setRole] = useState<JobRole | ''>('');
  const [message, setMessage] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState('');

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!role) {
      setError('지원할 역할을 선택해주세요.');
      return;
    }

    setIsSubmitting(true);
    setError('');

    try {
      await teamService.applyToTeam(teamId, {
        role,
        applicationMessage: message.trim() || undefined,
      });
      alert('지원이 완료되었습니다. 팀 리더의 수락을 기다려주세요.');
      onApplied();
    } catch (err: unknown) {
      console.error('Failed to apply:', err);
      const error = err as { response?: { data?: { message?: string } } };
      setError(error.response?.data?.message || '지원에 실패했습니다.');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-2xl max-w-lg w-full">
        <div className="p-6">
          <h2 className="text-xl font-bold text-neutral-900 mb-6">팀 지원하기</h2>

          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-neutral-700 mb-2">
                지원 역할 <span className="text-red-500">*</span>
              </label>
              <div className="grid grid-cols-2 gap-3">
                {recruitingRoles.map((r) => (
                  <label
                    key={r}
                    className={`flex items-center gap-2 p-3 rounded-lg border cursor-pointer transition-colors ${
                      role === r
                        ? 'border-primary-500 bg-primary-50'
                        : 'border-neutral-200 hover:bg-neutral-50'
                    }`}
                  >
                    <input
                      type="radio"
                      name="role"
                      value={r}
                      checked={role === r}
                      onChange={(e) => setRole(e.target.value as JobRole)}
                      className="w-4 h-4 text-primary-500"
                    />
                    <span className="text-sm">{JOB_ROLE_LABELS[r]}</span>
                  </label>
                ))}
              </div>
            </div>

            <div>
              <label className="block text-sm font-medium text-neutral-700 mb-1">자기소개 (선택)</label>
              <textarea
                value={message}
                onChange={(e) => setMessage(e.target.value)}
                className="input-field min-h-[100px]"
                placeholder="팀에 본인을 소개해주세요 (기술 스택, 경험, 참여 동기 등)"
                maxLength={500}
              />
            </div>

            {error && <p className="text-red-500 text-sm">{error}</p>}

            <div className="flex gap-3 pt-4">
              <button type="button" onClick={onClose} className="btn-secondary flex-1" disabled={isSubmitting}>
                취소
              </button>
              <button type="submit" className="btn-primary flex-1" disabled={isSubmitting}>
                {isSubmitting ? '지원 중...' : '지원하기'}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
}

interface AIMemberRecommendationModalProps {
  teamId: number;
  onClose: () => void;
}

function AIMemberRecommendationModal({ teamId, onClose }: AIMemberRecommendationModalProps) {
  const [recommendations, setRecommendations] = useState<MemberRecommendation[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState('');
  const [sendingInvitation, setSendingInvitation] = useState<number | null>(null);
  const [sentInvitations, setSentInvitations] = useState<Set<number>>(new Set());

  useEffect(() => {
    loadRecommendations();
  }, [teamId]);

  const handleSendInvitation = async (rec: MemberRecommendation) => {
    if (!rec.profile.userId) return;

    setSendingInvitation(rec.profile.userId);
    try {
      await invitationService.sendInvitation(teamId, {
        userId: rec.profile.userId,
        role: rec.profile.jobRole,
        message: `AI 매칭 점수 ${rec.matchScore}점을 기반으로 영입 요청을 드립니다.`,
        matchScore: rec.matchScore
      });
      setSentInvitations(prev => new Set(prev).add(rec.profile.userId!));
      alert('영입 요청을 보냈습니다!');
    } catch (err: unknown) {
      const error = err as { response?: { data?: { message?: string; errorCode?: string } } };
      if (error.response?.data?.errorCode === 'TEAM_009') {
        alert('이미 영입 요청을 보낸 사용자입니다.');
        setSentInvitations(prev => new Set(prev).add(rec.profile.userId!));
      } else if (error.response?.data?.errorCode === 'TEAM_003') {
        alert('해당 사용자는 이미 다른 팀에 소속되어 있습니다.');
      } else {
        alert(error.response?.data?.message || '영입 요청 발송에 실패했습니다.');
      }
    } finally {
      setSendingInvitation(null);
    }
  };

  const loadRecommendations = async () => {
    setIsLoading(true);
    setError('');
    try {
      const data = await matchingService.getMemberRecommendations(teamId, 5);
      setRecommendations(data);
    } catch (err: unknown) {
      console.error('Failed to load recommendations:', err);
      const error = err as { response?: { data?: { message?: string; errorCode?: string } } };
      if (error.response?.data?.errorCode === 'TEAM_002') {
        setError('팀 정원이 가득 찼습니다.');
      } else if (error.response?.data?.errorCode === 'AI_002') {
        setError('현재 추천 가능한 멤버가 없습니다. 나중에 다시 시도해주세요.');
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
                <h2 className="text-xl font-bold text-neutral-900">AI 멤버 추천</h2>
                <p className="text-sm text-neutral-500">팀에 맞는 멤버를 찾아드립니다</p>
              </div>
            </div>
            <button onClick={onClose} className="text-neutral-400 hover:text-neutral-600">
              <XIcon className="w-6 h-6" />
            </button>
          </div>
        </div>

        <div className="flex-1 overflow-y-auto p-6">
          {isLoading ? (
            <div className="flex flex-col items-center justify-center py-12">
              <div className="w-12 h-12 border-4 border-primary-200 border-t-primary-500 rounded-full animate-spin mb-4" />
              <p className="text-neutral-500">AI가 최적의 팀원을 분석 중입니다...</p>
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
              <p className="text-neutral-500">추천 가능한 멤버가 없습니다.</p>
            </div>
          ) : (
            <div className="space-y-4">
              {recommendations.map((rec, index) => (
                <div
                  key={rec.profile.id}
                  className="border border-neutral-200 rounded-xl p-4"
                >
                  <div className="flex items-start gap-4">
                    {/* 순위 표시 */}
                    <div className={`w-8 h-8 rounded-full flex items-center justify-center text-sm font-bold ${getScoreBgColor(rec.matchScore)} ${getScoreColor(rec.matchScore)}`}>
                      {index + 1}
                    </div>

                    <div className="flex-1">
                      {/* 프로필 기본 정보 */}
                      <div className="flex items-start justify-between mb-2">
                        <div>
                          <h3 className="font-semibold text-neutral-900">
                            {rec.profile.userName || '익명 사용자'}
                          </h3>
                          <div className="flex items-center gap-2 mt-1">
                            <span className={`text-xs ${JOB_ROLE_COLORS[rec.profile.jobRole]} px-2 py-0.5 rounded`}>
                              {JOB_ROLE_LABELS[rec.profile.jobRole]}
                            </span>
                            {rec.profile.availableHoursPerWeek && (
                              <span className="text-xs text-neutral-500">
                                주 {rec.profile.availableHoursPerWeek}시간 가능
                              </span>
                            )}
                          </div>
                        </div>
                        <div className={`px-3 py-1 rounded-full text-sm font-bold ${getScoreBgColor(rec.matchScore)} ${getScoreColor(rec.matchScore)}`}>
                          {rec.matchScore}점
                        </div>
                      </div>

                      {/* 기술 스택 */}
                      {rec.profile.techStacks && rec.profile.techStacks.length > 0 && (
                        <div className="mb-3">
                          <p className="text-xs font-medium text-neutral-500 mb-1">기술 스택</p>
                          <div className="flex flex-wrap gap-1">
                            {rec.profile.techStacks.slice(0, 5).map((tech, idx) => (
                              <span key={idx} className="px-2 py-0.5 bg-neutral-100 text-neutral-600 rounded text-xs">
                                {tech}
                              </span>
                            ))}
                            {rec.profile.techStacks.length > 5 && (
                              <span className="text-xs text-neutral-400">+{rec.profile.techStacks.length - 5}</span>
                            )}
                          </div>
                        </div>
                      )}

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
                        <p className="text-xs font-medium text-neutral-500 mb-1">팀 밸런스 영향</p>
                        <p className="text-sm text-neutral-700">{rec.balanceAnalysis}</p>
                      </div>

                      {/* 일정 위험도 & 보완 가능한 스킬 */}
                      <div className="flex flex-wrap gap-2 items-center mb-3">
                        <div className={`flex items-center gap-1 px-2 py-1 rounded text-xs ${SCHEDULE_RISK_COLORS[rec.scheduleRisk]}`}>
                          {getRiskIcon(rec.scheduleRisk)}
                          일정 충돌 위험: {SCHEDULE_RISK_LABELS[rec.scheduleRisk]}
                        </div>
                        {rec.complementarySkills && rec.complementarySkills.length > 0 && (
                          <div className="text-xs text-neutral-500">
                            보완 스킬: {rec.complementarySkills.slice(0, 3).join(', ')}
                          </div>
                        )}
                      </div>

                      {/* 영입 요청 버튼 */}
                      <div className="pt-3 border-t border-neutral-100">
                        {sentInvitations.has(rec.profile.userId!) ? (
                          <button
                            disabled
                            className="w-full flex items-center justify-center gap-2 px-4 py-2 bg-green-100 text-green-700 rounded-lg text-sm font-medium"
                          >
                            <Check className="w-4 h-4" />
                            영입 요청 전송됨
                          </button>
                        ) : (
                          <button
                            onClick={() => handleSendInvitation(rec)}
                            disabled={sendingInvitation === rec.profile.userId}
                            className="w-full flex items-center justify-center gap-2 px-4 py-2 bg-primary-500 hover:bg-primary-600 text-white rounded-lg text-sm font-medium transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
                          >
                            {sendingInvitation === rec.profile.userId ? (
                              <>
                                <Loader2 className="w-4 h-4 animate-spin" />
                                전송 중...
                              </>
                            ) : (
                              <>
                                <Mail className="w-4 h-4" />
                                영입 요청 보내기
                              </>
                            )}
                          </button>
                        )}
                      </div>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>

        <div className="p-6 border-t border-neutral-200 bg-neutral-50">
          <p className="text-xs text-neutral-500 text-center mb-4">
            AI 추천은 참고용입니다. 실제 채용은 지원자의 자기소개와 포트폴리오를 확인하세요.
          </p>
          <button onClick={onClose} className="btn-secondary w-full">
            닫기
          </button>
        </div>
      </div>
    </div>
  );
}
