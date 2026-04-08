import { useState, useEffect } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import { ChevronLeft, Users, Settings, Check, X, Clock, UserPlus } from 'lucide-react';
import { teamService, TEAM_STATUS_LABELS, TEAM_STATUS_COLORS, JOB_ROLE_LABELS, JOB_ROLE_COLORS } from '../services/teamService';
import { useAuthStore } from '../stores/authStore';
import type { Team, TeamMember, JobRole } from '../types';

export default function TeamDetailPage() {
  const { teamId } = useParams<{ teamId: string }>();
  const navigate = useNavigate();
  const { isAuthenticated, user } = useAuthStore();
  const [team, setTeam] = useState<Team | null>(null);
  const [applications, setApplications] = useState<TeamMember[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState('');
  const [showApplyModal, setShowApplyModal] = useState(false);
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
            <button className="btn-secondary flex items-center gap-2">
              <Settings className="w-4 h-4" />
              팀 설정
            </button>
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
                      <X className="w-4 h-4" />
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
