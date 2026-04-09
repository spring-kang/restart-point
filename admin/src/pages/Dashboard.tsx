import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { Calendar, UserCheck, Clock, ArrowRight } from 'lucide-react';
import adminService from '../services/adminService';
import type { Season, User } from '../types';
import { SEASON_STATUS_LABELS, SEASON_STATUS_COLORS } from '../types';

interface StatCard {
  title: string;
  value: number | string;
  icon: React.ElementType;
  color: string;
  link?: string;
}

export default function DashboardPage() {
  const [seasons, setSeasons] = useState<Season[]>([]);
  const [pendingCertifications, setPendingCertifications] = useState<User[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    loadData();
  }, []);

  const loadData = async () => {
    try {
      const [seasonsData, certificationsData] = await Promise.all([
        adminService.getSeasons(),
        adminService.getPendingCertifications(),
      ]);
      setSeasons(seasonsData.content);
      setPendingCertifications(certificationsData);
    } catch (error) {
      console.error('Failed to load dashboard data:', error);
    } finally {
      setIsLoading(false);
    }
  };

  const activeSeasons = seasons.filter(
    (s) => !['DRAFT', 'COMPLETED'].includes(s.status)
  );

  const stats: StatCard[] = [
    {
      title: '활성 시즌',
      value: activeSeasons.length,
      icon: Calendar,
      color: 'bg-blue-500',
      link: '/seasons',
    },
    {
      title: '인증 대기',
      value: pendingCertifications.length,
      icon: UserCheck,
      color: 'bg-yellow-500',
      link: '/certifications',
    },
    {
      title: '전체 시즌',
      value: seasons.length,
      icon: Clock,
      color: 'bg-gray-500',
      link: '/seasons',
    },
  ];

  if (isLoading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="text-gray-500">로딩 중...</div>
      </div>
    );
  }

  return (
    <div className="space-y-8">
      <div>
        <h1 className="text-2xl font-bold text-gray-900">대시보드</h1>
        <p className="text-gray-500 mt-1">Re:Start Point 관리 현황</p>
      </div>

      {/* Stats */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        {stats.map((stat) => (
          <Link
            key={stat.title}
            to={stat.link || '#'}
            className="card flex items-center gap-4 hover:shadow-md transition-shadow"
          >
            <div className={`p-3 rounded-lg ${stat.color}`}>
              <stat.icon className="w-6 h-6 text-white" />
            </div>
            <div>
              <p className="text-sm text-gray-500">{stat.title}</p>
              <p className="text-2xl font-bold text-gray-900">{stat.value}</p>
            </div>
          </Link>
        ))}
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
        {/* Active Seasons */}
        <div className="card">
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-lg font-semibold text-gray-900">활성 시즌</h2>
            <Link
              to="/seasons"
              className="text-sm text-primary-600 hover:text-primary-700 flex items-center gap-1"
            >
              전체 보기 <ArrowRight className="w-4 h-4" />
            </Link>
          </div>

          {activeSeasons.length === 0 ? (
            <p className="text-gray-500 text-sm py-4 text-center">
              활성화된 시즌이 없습니다.
            </p>
          ) : (
            <div className="space-y-3">
              {activeSeasons.slice(0, 3).map((season) => (
                <Link
                  key={season.id}
                  to="/seasons"
                  className="block p-3 rounded-lg border border-gray-200 hover:border-primary-300 transition-colors"
                >
                  <div className="flex items-center justify-between">
                    <div>
                      <p className="font-medium text-gray-900">{season.title}</p>
                      <p className="text-sm text-gray-500">
                        {new Date(season.projectStartAt).toLocaleDateString('ko-KR')} ~{' '}
                        {new Date(season.projectEndAt).toLocaleDateString('ko-KR')}
                      </p>
                    </div>
                    <span
                      className={`px-2 py-1 rounded text-xs font-medium ${
                        SEASON_STATUS_COLORS[season.status]
                      }`}
                    >
                      {SEASON_STATUS_LABELS[season.status]}
                    </span>
                  </div>
                </Link>
              ))}
            </div>
          )}
        </div>

        {/* Pending Certifications */}
        <div className="card">
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-lg font-semibold text-gray-900">인증 대기</h2>
            <Link
              to="/certifications"
              className="text-sm text-primary-600 hover:text-primary-700 flex items-center gap-1"
            >
              전체 보기 <ArrowRight className="w-4 h-4" />
            </Link>
          </div>

          {pendingCertifications.length === 0 ? (
            <p className="text-gray-500 text-sm py-4 text-center">
              대기 중인 인증 요청이 없습니다.
            </p>
          ) : (
            <div className="space-y-3">
              {pendingCertifications.slice(0, 5).map((user) => (
                <div
                  key={user.id}
                  className="flex items-center justify-between p-3 rounded-lg border border-gray-200"
                >
                  <div>
                    <p className="font-medium text-gray-900">{user.name}</p>
                    <p className="text-sm text-gray-500">
                      {user.bootcampName || '부트캠프 미입력'} {user.bootcampGeneration || ''}
                    </p>
                  </div>
                  <span className="text-xs text-gray-500">
                    {new Date(user.createdAt).toLocaleDateString('ko-KR')}
                  </span>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
