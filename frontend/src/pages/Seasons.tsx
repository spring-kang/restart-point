import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { Calendar, Users, Clock, ChevronRight } from 'lucide-react';
import { seasonService, type Season, SEASON_STATUS_LABELS, SEASON_STATUS_COLORS } from '../services/seasonService';

export default function SeasonsPage() {
  const [seasons, setSeasons] = useState<Season[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    loadSeasons();
  }, []);

  const loadSeasons = async () => {
    try {
      const data = await seasonService.getPublicSeasons();
      setSeasons(data);
    } catch (err) {
      console.error('Failed to load seasons:', err);
      setError('시즌 목록을 불러오는데 실패했습니다.');
    } finally {
      setIsLoading(false);
    }
  };

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString('ko-KR', {
      year: 'numeric',
      month: 'long',
      day: 'numeric',
    });
  };

  const formatDateRange = (start: string, end: string) => {
    return `${formatDate(start)} ~ ${formatDate(end)}`;
  };

  if (isLoading) {
    return (
      <div className="flex items-center justify-center min-h-[50vh]">
        <div className="text-neutral-500">로딩 중...</div>
      </div>
    );
  }

  return (
    <div className="max-w-5xl mx-auto px-4 py-12">
      <div className="text-center mb-12">
        <h1 className="text-3xl font-bold text-neutral-900 mb-4">
          시즌 공모전
        </h1>
        <p className="text-neutral-600 max-w-2xl mx-auto">
          팀을 구성하고 프로젝트를 완성하여 성장하세요.
          AI 기반 팀 매칭과 프로젝트 코칭을 제공합니다.
        </p>
      </div>

      {error && (
        <div className="bg-red-50 text-red-600 px-4 py-3 rounded-xl text-sm mb-6">
          {error}
        </div>
      )}

      {seasons.length === 0 ? (
        <div className="text-center py-16">
          <Calendar className="w-16 h-16 text-neutral-300 mx-auto mb-4" />
          <h3 className="text-lg font-medium text-neutral-900 mb-2">
            진행 중인 시즌이 없습니다
          </h3>
          <p className="text-neutral-500">
            새로운 시즌이 시작되면 알려드릴게요.
          </p>
        </div>
      ) : (
        <div className="space-y-6">
          {seasons.map((season) => (
            <Link
              key={season.id}
              to={`/seasons/${season.id}`}
              className="card block hover:shadow-lg transition-shadow"
            >
              <div className="flex items-start justify-between">
                <div className="flex-1">
                  <div className="flex items-center gap-3 mb-2">
                    <span className={`px-3 py-1 rounded-full text-sm font-medium ${SEASON_STATUS_COLORS[season.status]}`}>
                      {SEASON_STATUS_LABELS[season.status]}
                    </span>
                    {season.myTeamId ? (
                      <span className="px-3 py-1 rounded-full text-sm font-medium bg-blue-100 text-blue-700">
                        참여 중: {season.myTeamName}
                      </span>
                    ) : season.canJoin && (
                      <span className="px-3 py-1 rounded-full text-sm font-medium bg-primary-100 text-primary-700">
                        참여 가능
                      </span>
                    )}
                  </div>
                  <h2 className="text-xl font-bold text-neutral-900 mb-2">
                    {season.title}
                  </h2>
                  {season.description && (
                    <p className="text-neutral-600 mb-4 line-clamp-2">
                      {season.description}
                    </p>
                  )}

                  <div className="grid grid-cols-1 md:grid-cols-3 gap-4 text-sm">
                    <div className="flex items-center gap-2 text-neutral-600">
                      <Users className="w-4 h-4" />
                      <span>모집: {formatDateRange(season.recruitmentStartAt, season.recruitmentEndAt)}</span>
                    </div>
                    <div className="flex items-center gap-2 text-neutral-600">
                      <Clock className="w-4 h-4" />
                      <span>프로젝트: {formatDateRange(season.projectStartAt, season.projectEndAt)}</span>
                    </div>
                    <div className="flex items-center gap-2 text-neutral-600">
                      <Calendar className="w-4 h-4" />
                      <span>현재: {season.currentPhase}</span>
                    </div>
                  </div>
                </div>
                <ChevronRight className="w-6 h-6 text-neutral-400 flex-shrink-0 ml-4" />
              </div>
            </Link>
          ))}
        </div>
      )}
    </div>
  );
}
