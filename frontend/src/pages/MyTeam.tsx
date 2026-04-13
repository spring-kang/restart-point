import { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Users, Crown, UserCheck, Clock, ChevronRight, Plus, FolderKanban, Mail, Check, X, Loader2 } from 'lucide-react';
import { teamService, TEAM_STATUS_LABELS, TEAM_STATUS_COLORS, JOB_ROLE_LABELS, JOB_ROLE_COLORS } from '../services/teamService';
import { invitationService, INVITATION_STATUS_LABELS, INVITATION_STATUS_COLORS } from '../services/invitationService';
import { useAuthStore } from '../stores/authStore';
import type { Team, TeamMember, TeamInvitation } from '../types';

type MemberStatus = 'PENDING' | 'ACCEPTED' | 'REJECTED';

const MEMBER_STATUS_LABELS: Record<MemberStatus, string> = {
  PENDING: '대기 중',
  ACCEPTED: '승인됨',
  REJECTED: '거절됨',
};

const MEMBER_STATUS_COLORS: Record<MemberStatus, string> = {
  PENDING: 'bg-yellow-100 text-yellow-700',
  ACCEPTED: 'bg-green-100 text-green-700',
  REJECTED: 'bg-red-100 text-red-700',
};

export default function MyTeamPage() {
  const navigate = useNavigate();
  const { isAuthenticated, user } = useAuthStore();
  const [myTeams, setMyTeams] = useState<Team[]>([]);
  const [memberTeams, setMemberTeams] = useState<Team[]>([]);
  const [applications, setApplications] = useState<TeamMember[]>([]);
  const [invitations, setInvitations] = useState<TeamInvitation[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    if (!isAuthenticated) {
      navigate('/login');
      return;
    }
    loadData();
  }, [isAuthenticated, navigate]);

  const loadData = async () => {
    setIsLoading(true);
    setError('');
    try {
      const [myTeamsData, memberTeamsData, applicationsData, invitationsData] = await Promise.all([
        teamService.getMyTeams(),
        teamService.getTeamsAsMember(),
        teamService.getMyApplications(),
        invitationService.getMyInvitations(),
      ]);
      setMyTeams(myTeamsData);
      setMemberTeams(memberTeamsData);
      setApplications(applicationsData);
      setInvitations(invitationsData);
    } catch (err) {
      console.error('Failed to load data:', err);
      setError('데이터를 불러오는데 실패했습니다.');
    } finally {
      setIsLoading(false);
    }
  };

  if (!isAuthenticated) {
    return null;
  }

  if (isLoading) {
    return (
      <div className="flex items-center justify-center min-h-[50vh]">
        <div className="text-neutral-500">로딩 중...</div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="max-w-3xl mx-auto px-4 py-12">
        <div className="text-center">
          <p className="text-red-500 mb-4">{error}</p>
          <button onClick={loadData} className="btn-secondary">
            다시 시도
          </button>
        </div>
      </div>
    );
  }

  const pendingInvitations = invitations.filter(inv => inv.status === 'PENDING');
  const hasNoData = myTeams.length === 0 && memberTeams.length === 0 && applications.length === 0 && invitations.length === 0;

  return (
    <div className="max-w-4xl mx-auto px-4 py-12">
      <div className="flex justify-between items-center mb-8">
        <div>
          <h1 className="text-2xl font-bold text-neutral-900">내 팀</h1>
          <p className="text-neutral-600 mt-1">내가 속한 팀과 지원 현황을 확인하세요</p>
        </div>
        {user?.certificationStatus === 'APPROVED' && (
          <Link to="/teams" className="btn-primary flex items-center gap-2">
            <Plus className="w-5 h-5" />
            팀 찾기
          </Link>
        )}
      </div>

      {hasNoData ? (
        <div className="card text-center py-12">
          <Users className="w-16 h-16 text-neutral-300 mx-auto mb-4" />
          <h2 className="text-lg font-semibold text-neutral-900 mb-2">아직 팀이 없습니다</h2>
          <p className="text-neutral-500 mb-6">
            {user?.certificationStatus === 'APPROVED'
              ? '팀을 만들거나 다른 팀에 지원해보세요!'
              : '수료 인증을 완료하면 팀에 참여할 수 있습니다.'}
          </p>
          {user?.certificationStatus === 'APPROVED' ? (
            <Link to="/teams" className="btn-primary">
              팀 둘러보기
            </Link>
          ) : (
            <Link to="/certification" className="btn-primary">
              수료 인증하기
            </Link>
          )}
        </div>
      ) : (
        <div className="space-y-8">
          {/* 내가 리더인 팀 */}
          {myTeams.length > 0 && (
            <section>
              <h2 className="text-lg font-semibold text-neutral-900 mb-4 flex items-center gap-2">
                <Crown className="w-5 h-5 text-yellow-500" />
                내가 만든 팀
              </h2>
              <div className="space-y-3">
                {myTeams.map((team) => (
                  <TeamCard key={team.id} team={team} isLeader />
                ))}
              </div>
            </section>
          )}

          {/* 내가 멤버인 팀 */}
          {memberTeams.length > 0 && (
            <section>
              <h2 className="text-lg font-semibold text-neutral-900 mb-4 flex items-center gap-2">
                <UserCheck className="w-5 h-5 text-green-500" />
                소속 팀
              </h2>
              <div className="space-y-3">
                {memberTeams.map((team) => (
                  <TeamCard key={team.id} team={team} />
                ))}
              </div>
            </section>
          )}

          {/* 받은 영입 요청 */}
          {invitations.length > 0 && (
            <section>
              <h2 className="text-lg font-semibold text-neutral-900 mb-4 flex items-center gap-2">
                <Mail className="w-5 h-5 text-purple-500" />
                받은 영입 요청
                {pendingInvitations.length > 0 && (
                  <span className="px-2 py-0.5 bg-purple-100 text-purple-700 text-xs font-medium rounded-full">
                    {pendingInvitations.length}
                  </span>
                )}
              </h2>
              <div className="space-y-3">
                {invitations.map((invitation) => (
                  <InvitationCard
                    key={invitation.id}
                    invitation={invitation}
                    onUpdate={loadData}
                  />
                ))}
              </div>
            </section>
          )}

          {/* 지원 현황 */}
          {applications.length > 0 && (
            <section>
              <h2 className="text-lg font-semibold text-neutral-900 mb-4 flex items-center gap-2">
                <Clock className="w-5 h-5 text-blue-500" />
                지원 현황
              </h2>
              <div className="space-y-3">
                {applications.map((app) => (
                  <ApplicationCard key={app.id} application={app} />
                ))}
              </div>
            </section>
          )}
        </div>
      )}
    </div>
  );
}

interface TeamCardProps {
  team: Team;
  isLeader?: boolean;
}

function TeamCard({ team, isLeader }: TeamCardProps) {
  const showProjectButton = team.status === 'IN_PROGRESS' || team.status === 'COMPLETE' || team.status === 'SUBMITTED' || team.status === 'REVIEWED';

  return (
    <div className="card hover:shadow-lg transition-shadow">
      <div className="flex items-center justify-between">
        <Link to={`/teams/${team.id}`} className="flex-1">
          <div className="flex items-center gap-3 mb-2">
            <h3 className="font-semibold text-neutral-900">{team.name}</h3>
            {isLeader && (
              <span className="px-2 py-0.5 bg-yellow-100 text-yellow-700 text-xs font-medium rounded">
                리더
              </span>
            )}
            <span className={`px-2 py-0.5 rounded text-xs font-medium ${TEAM_STATUS_COLORS[team.status]}`}>
              {TEAM_STATUS_LABELS[team.status]}
            </span>
          </div>
          {team.description && (
            <p className="text-sm text-neutral-600 line-clamp-1 mb-2">{team.description}</p>
          )}
          <div className="flex items-center gap-4 text-sm text-neutral-500">
            <span className="flex items-center gap-1">
              <Users className="w-4 h-4" />
              {team.memberCount}/{team.maxMemberCount}명
            </span>
          </div>
        </Link>
        <div className="flex items-center gap-2 ml-4">
          {showProjectButton && (
            <Link
              to={`/teams/${team.id}/project`}
              className="btn-primary text-sm flex items-center gap-1.5"
              onClick={(e) => e.stopPropagation()}
            >
              <FolderKanban className="w-4 h-4" />
              프로젝트
            </Link>
          )}
          <Link to={`/teams/${team.id}`} className="p-2 hover:bg-neutral-100 rounded-lg">
            <ChevronRight className="w-5 h-5 text-neutral-400" />
          </Link>
        </div>
      </div>
    </div>
  );
}

interface ApplicationCardProps {
  application: TeamMember;
}

function ApplicationCard({ application }: ApplicationCardProps) {
  const status = application.status as MemberStatus;

  return (
    <div className="card">
      <div className="flex items-center justify-between">
        <div>
          <div className="flex items-center gap-3 mb-1">
            <span className={`px-2 py-0.5 rounded text-xs font-medium ${MEMBER_STATUS_COLORS[status]}`}>
              {MEMBER_STATUS_LABELS[status]}
            </span>
            <span className={`px-2 py-0.5 rounded text-xs ${JOB_ROLE_COLORS[application.role]}`}>
              {JOB_ROLE_LABELS[application.role]}
            </span>
          </div>
          {application.applicationMessage && (
            <p className="text-sm text-neutral-500 mt-1 line-clamp-1">
              {application.applicationMessage}
            </p>
          )}
        </div>
        {application.teamId && (
          <Link to={`/teams/${application.teamId}`} className="btn-secondary text-sm">
            팀 보기
          </Link>
        )}
      </div>
    </div>
  );
}

interface InvitationCardProps {
  invitation: TeamInvitation;
  onUpdate: () => void;
}

function InvitationCard({ invitation, onUpdate }: InvitationCardProps) {
  const [isProcessing, setIsProcessing] = useState(false);

  const handleAccept = async () => {
    if (!confirm(`'${invitation.teamName}' 팀의 영입 요청을 수락하시겠습니까?`)) return;

    setIsProcessing(true);
    try {
      await invitationService.acceptInvitation(invitation.id);
      alert('영입 요청을 수락했습니다. 팀에 합류되었습니다!');
      onUpdate();
    } catch (err: unknown) {
      const error = err as { response?: { data?: { message?: string } } };
      alert(error.response?.data?.message || '수락 처리에 실패했습니다.');
    } finally {
      setIsProcessing(false);
    }
  };

  const handleReject = async () => {
    if (!confirm(`'${invitation.teamName}' 팀의 영입 요청을 거절하시겠습니까?`)) return;

    setIsProcessing(true);
    try {
      await invitationService.rejectInvitation(invitation.id);
      alert('영입 요청을 거절했습니다.');
      onUpdate();
    } catch (err: unknown) {
      const error = err as { response?: { data?: { message?: string } } };
      alert(error.response?.data?.message || '거절 처리에 실패했습니다.');
    } finally {
      setIsProcessing(false);
    }
  };

  const isExpired = new Date(invitation.expiresAt) < new Date();
  const isPending = invitation.status === 'PENDING' && !isExpired;

  const formatDate = (dateStr: string) => {
    const date = new Date(dateStr);
    return `${date.getMonth() + 1}/${date.getDate()}`;
  };

  const getDaysLeft = () => {
    const now = new Date();
    const expires = new Date(invitation.expiresAt);
    const diff = Math.ceil((expires.getTime() - now.getTime()) / (1000 * 60 * 60 * 24));
    return diff;
  };

  return (
    <div className="card">
      <div className="flex items-start justify-between">
        <div className="flex-1">
          <div className="flex items-center gap-3 mb-2">
            <Link to={`/teams/${invitation.teamId}`} className="font-semibold text-neutral-900 hover:text-primary-600">
              {invitation.teamName}
            </Link>
            <span className={`px-2 py-0.5 rounded text-xs font-medium ${
              isExpired ? INVITATION_STATUS_COLORS['EXPIRED'] :
              INVITATION_STATUS_COLORS[invitation.status]
            }`}>
              {isExpired ? '만료됨' : INVITATION_STATUS_LABELS[invitation.status]}
            </span>
            <span className={`px-2 py-0.5 rounded text-xs ${JOB_ROLE_COLORS[invitation.suggestedRole]}`}>
              {JOB_ROLE_LABELS[invitation.suggestedRole]}
            </span>
            {invitation.matchScore && (
              <span className="px-2 py-0.5 bg-primary-100 text-primary-700 rounded text-xs">
                AI 매칭 {invitation.matchScore}점
              </span>
            )}
          </div>

          <div className="text-sm text-neutral-600 mb-2">
            <span className="font-medium">{invitation.invitedByName}</span>님이 영입 요청을 보냈습니다
          </div>

          {invitation.message && (
            <p className="text-sm text-neutral-500 mb-2 line-clamp-2">
              "{invitation.message}"
            </p>
          )}

          <div className="flex items-center gap-4 text-xs text-neutral-400">
            <span>시즌: {invitation.seasonName}</span>
            <span>받은 날짜: {formatDate(invitation.createdAt)}</span>
            {isPending && (
              <span className={getDaysLeft() <= 2 ? 'text-red-500' : ''}>
                마감: {getDaysLeft()}일 남음
              </span>
            )}
          </div>
        </div>

        {isPending && (
          <div className="flex items-center gap-2 ml-4">
            <button
              onClick={handleAccept}
              disabled={isProcessing}
              className="flex items-center gap-1 px-3 py-1.5 bg-green-500 hover:bg-green-600 text-white rounded-lg text-sm font-medium transition-colors disabled:opacity-50"
            >
              {isProcessing ? <Loader2 className="w-4 h-4 animate-spin" /> : <Check className="w-4 h-4" />}
              수락
            </button>
            <button
              onClick={handleReject}
              disabled={isProcessing}
              className="flex items-center gap-1 px-3 py-1.5 bg-neutral-200 hover:bg-neutral-300 text-neutral-700 rounded-lg text-sm font-medium transition-colors disabled:opacity-50"
            >
              <X className="w-4 h-4" />
              거절
            </button>
          </div>
        )}

        {!isPending && (
          <Link to={`/teams/${invitation.teamId}`} className="btn-secondary text-sm ml-4">
            팀 보기
          </Link>
        )}
      </div>
    </div>
  );
}
