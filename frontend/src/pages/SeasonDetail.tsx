import { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import { Calendar, Users, Clock, Award, ChevronLeft, UserPlus, ClipboardCheck } from 'lucide-react';
import { seasonService, type Season, SEASON_STATUS_LABELS, SEASON_STATUS_COLORS } from '../services/seasonService';
import { useAuthStore } from '../stores/authStore';

export default function SeasonDetailPage() {
  const { seasonId } = useParams<{ seasonId: string }>();
  const { isAuthenticated, user } = useAuthStore();
  const [season, setSeason] = useState<Season | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    if (seasonId) {
      loadSeason(Number(seasonId));
    }
  }, [seasonId]);

  const loadSeason = async (id: number) => {
    try {
      const data = await seasonService.getSeason(id);
      setSeason(data);
    } catch (err) {
      console.error('Failed to load season:', err);
      setError('시즌 정보를 불러오는데 실패했습니다.');
    } finally {
      setIsLoading(false);
    }
  };

  const canParticipate = () => {
    if (!isAuthenticated || !user) return false;
    // 모든 사용자는 회원가입 시 이메일 인증 완료됨
    if (user.certificationStatus !== 'APPROVED') return false;
    return season?.canJoin ?? false;
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
    <div className="max-w-4xl mx-auto px-4 py-12">
      <Link to="/seasons" className="inline-flex items-center gap-2 text-neutral-600 hover:text-neutral-900 mb-6">
        <ChevronLeft className="w-5 h-5" />
        시즌 목록
      </Link>

      <div className="card mb-8">
        <div className="flex items-center gap-3 mb-4">
          <span className={`px-3 py-1 rounded-full text-sm font-medium ${SEASON_STATUS_COLORS[season.status]}`}>
            {SEASON_STATUS_LABELS[season.status]}
          </span>
          <span className="text-neutral-500">{season.currentPhase}</span>
          {season.myTeamId && (
            <span className="px-3 py-1 rounded-full text-sm font-medium bg-blue-100 text-blue-700">
              참여 중
            </span>
          )}
        </div>

        <h1 className="text-2xl font-bold text-neutral-900 mb-4">{season.title}</h1>

        {season.description && (
          <p className="text-neutral-600 mb-6 whitespace-pre-wrap">{season.description}</p>
        )}

        {/* 참여 중인 팀 안내 */}
        {season.myTeamId && (
          <div className="bg-blue-50 border border-blue-200 rounded-xl p-4 mb-4">
            <p className="text-blue-700">
              이 시즌에 <strong>{season.myTeamName}</strong> 팀으로 참여 중입니다.{' '}
              <Link to={`/teams/${season.myTeamId}`} className="font-semibold underline">
                팀 보기
              </Link>
            </p>
          </div>
        )}

        <div className="flex flex-wrap gap-3">
          {canParticipate() && !season.myTeamId && (
            <Link to={`/seasons/${season.id}/teams`} className="btn-primary inline-flex items-center gap-2">
              <UserPlus className="w-5 h-5" />
              팀 찾기 / 팀 만들기
            </Link>
          )}

          {season.myTeamId && (
            <Link to={`/teams/${season.myTeamId}`} className="btn-primary inline-flex items-center gap-2">
              <Users className="w-5 h-5" />
              내 팀 보기
            </Link>
          )}

          {isAuthenticated && season.status === 'REVIEWING' &&
           (user?.role === 'REVIEWER' || user?.role === 'ADMIN' || user?.certificationStatus === 'APPROVED') && (
            <Link to={`/seasons/${season.id}/review`} className="btn-secondary inline-flex items-center gap-2">
              <ClipboardCheck className="w-5 h-5" />
              프로젝트 심사하기
            </Link>
          )}
        </div>

        {!isAuthenticated && season.canJoin && (
          <div className="bg-primary-50 border border-primary-200 rounded-xl p-4 mt-4">
            <p className="text-primary-700">
              이 시즌에 참여하려면 <Link to="/login" className="font-semibold underline">로그인</Link>이 필요합니다.
            </p>
          </div>
        )}

        {isAuthenticated && user?.certificationStatus !== 'APPROVED' && season.canJoin && !season.myTeamId && (
          <div className="bg-amber-50 border border-amber-200 rounded-xl p-4 mt-4">
            <p className="text-amber-700">
              이 시즌에 참여하려면 <Link to="/certification" className="font-semibold underline">수료 인증</Link>이 필요합니다.
            </p>
          </div>
        )}
      </div>

      {/* 일정 */}
      <div className="card mb-8">
        <h2 className="text-lg font-bold text-neutral-900 mb-6 flex items-center gap-2">
          <Calendar className="w-5 h-5 text-primary-500" />
          시즌 일정
        </h2>

        <div className="space-y-6">
          <TimelineItem
            title="모집 기간"
            icon={<Users className="w-5 h-5" />}
            start={season.recruitmentStartAt}
            end={season.recruitmentEndAt}
            isActive={season.status === 'RECRUITING'}
          />
          <TimelineItem
            title="팀빌딩 기간"
            icon={<Users className="w-5 h-5" />}
            start={season.teamBuildingStartAt}
            end={season.teamBuildingEndAt}
            isActive={season.status === 'TEAM_BUILDING'}
          />
          <TimelineItem
            title="프로젝트 기간"
            icon={<Clock className="w-5 h-5" />}
            start={season.projectStartAt}
            end={season.projectEndAt}
            isActive={season.status === 'IN_PROGRESS'}
          />
          <TimelineItem
            title="심사 기간"
            icon={<Award className="w-5 h-5" />}
            start={season.reviewStartAt}
            end={season.reviewEndAt}
            isActive={season.status === 'REVIEWING'}
          />
        </div>
      </div>

      {/* 심사 비중 */}
      <div className="card">
        <h2 className="text-lg font-bold text-neutral-900 mb-6 flex items-center gap-2">
          <Award className="w-5 h-5 text-primary-500" />
          심사 비중
        </h2>

        <div className="grid grid-cols-2 gap-4">
          <div className="bg-neutral-50 rounded-xl p-4 text-center">
            <div className="text-3xl font-bold text-primary-600 mb-1">
              {season.expertReviewWeight}%
            </div>
            <div className="text-sm text-neutral-600">현직자 심사</div>
          </div>
          <div className="bg-neutral-50 rounded-xl p-4 text-center">
            <div className="text-3xl font-bold text-secondary-600 mb-1">
              {season.candidateReviewWeight}%
            </div>
            <div className="text-sm text-neutral-600">예비 참여자 심사</div>
          </div>
        </div>
      </div>
    </div>
  );
}

interface TimelineItemProps {
  title: string;
  icon: React.ReactNode;
  start: string;
  end: string;
  isActive: boolean;
}

function TimelineItem({ title, icon, start, end, isActive }: TimelineItemProps) {
  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString('ko-KR', {
      year: 'numeric',
      month: 'long',
      day: 'numeric',
    });
  };

  return (
    <div className={`flex items-start gap-4 p-4 rounded-xl ${isActive ? 'bg-primary-50 border border-primary-200' : 'bg-neutral-50'}`}>
      <div className={`p-2 rounded-lg ${isActive ? 'bg-primary-500 text-white' : 'bg-neutral-200 text-neutral-600'}`}>
        {icon}
      </div>
      <div className="flex-1">
        <div className="flex items-center gap-2 mb-1">
          <h3 className={`font-semibold ${isActive ? 'text-primary-700' : 'text-neutral-900'}`}>
            {title}
          </h3>
          {isActive && (
            <span className="px-2 py-0.5 bg-primary-500 text-white text-xs rounded-full">
              진행 중
            </span>
          )}
        </div>
        <p className="text-sm text-neutral-600">
          {formatDate(start)} ~ {formatDate(end)}
        </p>
      </div>
    </div>
  );
}
