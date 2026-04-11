import { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Users, Crown, UserCheck, Clock, ChevronRight, Plus, FolderKanban } from 'lucide-react';
import { teamService, TEAM_STATUS_LABELS, TEAM_STATUS_COLORS, JOB_ROLE_LABELS, JOB_ROLE_COLORS } from '../services/teamService';
import { useAuthStore } from '../stores/authStore';
import type { Team, TeamMember } from '../types';

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
      const [myTeamsData, memberTeamsData, applicationsData] = await Promise.all([
        teamService.getMyTeams(),
        teamService.getTeamsAsMember(),
        teamService.getMyApplications(),
      ]);
      setMyTeams(myTeamsData);
      setMemberTeams(memberTeamsData);
      setApplications(applicationsData);
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

  const hasNoData = myTeams.length === 0 && memberTeams.length === 0 && applications.length === 0;

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
