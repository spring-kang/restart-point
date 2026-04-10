import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import {
  Calendar,
  UserCheck,
  Users,
  FolderKanban,
  Star,
  FileText,
  AlertTriangle,
  ArrowRight,
  ChevronDown,
} from 'lucide-react';
import adminService from '../services/adminService';
import type {
  Season,
  User,
  SeasonDashboard,
  OverallDashboard,
  JobRole,
} from '../types';
import {
  SEASON_STATUS_LABELS,
  SEASON_STATUS_COLORS,
  JOB_ROLE_LABELS,
  RISK_TYPE_LABELS,
  RISK_LEVEL_COLORS,
} from '../types';

interface StatCard {
  title: string;
  value: number | string;
  subValue?: string;
  icon: React.ElementType;
  color: string;
  link?: string;
}

export default function DashboardPage() {
  const [seasons, setSeasons] = useState<Season[]>([]);
  const [pendingCertifications, setPendingCertifications] = useState<User[]>([]);
  const [overallDashboard, setOverallDashboard] = useState<OverallDashboard | null>(null);
  const [selectedSeasonId, setSelectedSeasonId] = useState<number | null>(null);
  const [seasonDashboard, setSeasonDashboard] = useState<SeasonDashboard | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isSeasonLoading, setIsSeasonLoading] = useState(false);

  useEffect(() => {
    loadInitialData();
  }, []);

  useEffect(() => {
    if (selectedSeasonId) {
      loadSeasonDashboard(selectedSeasonId);
    }
  }, [selectedSeasonId]);

  const loadInitialData = async () => {
    try {
      const [seasonsData, certificationsData, dashboardData] = await Promise.all([
        adminService.getSeasons(),
        adminService.getPendingCertifications(),
        adminService.getOverallDashboard(),
      ]);
      setSeasons(seasonsData.content);
      setPendingCertifications(certificationsData);
      setOverallDashboard(dashboardData);

      // 활성 시즌이 있으면 첫 번째 시즌 선택
      const activeSeasons = seasonsData.content.filter(
        (s) => !['DRAFT', 'COMPLETED'].includes(s.status)
      );
      if (activeSeasons.length > 0) {
        setSelectedSeasonId(activeSeasons[0].id);
      }
    } catch (error) {
      console.error('Failed to load dashboard data:', error);
    } finally {
      setIsLoading(false);
    }
  };

  const loadSeasonDashboard = async (seasonId: number) => {
    setIsSeasonLoading(true);
    try {
      const data = await adminService.getSeasonDashboard(seasonId);
      setSeasonDashboard(data);
    } catch (error) {
      console.error('Failed to load season dashboard:', error);
      setSeasonDashboard(null);
    } finally {
      setIsSeasonLoading(false);
    }
  };

  const activeSeasons = seasons.filter(
    (s) => !['DRAFT', 'COMPLETED'].includes(s.status)
  );

  const overallStats: StatCard[] = [
    {
      title: '활성 시즌',
      value: overallDashboard?.activeSeasonCount || activeSeasons.length,
      icon: Calendar,
      color: 'bg-blue-500',
      link: '/seasons',
    },
    {
      title: '인증 대기',
      value: overallDashboard?.pendingCertifications || pendingCertifications.length,
      icon: UserCheck,
      color: 'bg-yellow-500',
      link: '/certifications',
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

      {/* Overall Stats */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        {overallStats.map((stat) => (
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

      {/* Season Selector */}
      {activeSeasons.length > 0 && (
        <div className="card">
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-lg font-semibold text-gray-900">시즌별 현황</h2>
            <div className="relative">
              <select
                value={selectedSeasonId || ''}
                onChange={(e) => setSelectedSeasonId(Number(e.target.value))}
                className="appearance-none bg-white border border-gray-300 rounded-lg px-4 py-2 pr-8 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500"
              >
                {activeSeasons.map((season) => (
                  <option key={season.id} value={season.id}>
                    {season.title}
                  </option>
                ))}
              </select>
              <ChevronDown className="absolute right-2 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-500 pointer-events-none" />
            </div>
          </div>

          {isSeasonLoading ? (
            <div className="flex items-center justify-center h-32">
              <div className="text-gray-500">로딩 중...</div>
            </div>
          ) : seasonDashboard ? (
            <div className="space-y-6">
              {/* Season Stats Grid */}
              <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
                <StatBox
                  icon={Users}
                  color="bg-blue-100 text-blue-600"
                  title="팀 현황"
                  value={`${seasonDashboard.teamStats.completeTeams}/${seasonDashboard.teamStats.totalTeams}`}
                  subtext={`완성 팀 / 전체 팀`}
                />
                <StatBox
                  icon={FolderKanban}
                  color="bg-green-100 text-green-600"
                  title="프로젝트 제출"
                  value={`${seasonDashboard.projectStats.submissionRate}%`}
                  subtext={`${seasonDashboard.projectStats.submittedProjects}/${seasonDashboard.projectStats.totalProjects} 제출`}
                />
                <StatBox
                  icon={Star}
                  color="bg-orange-100 text-orange-600"
                  title="심사 현황"
                  value={seasonDashboard.reviewStats.totalReviews}
                  subtext={`평균 ${seasonDashboard.reviewStats.averageScore.toFixed(1)}점`}
                />
                <StatBox
                  icon={FileText}
                  color="bg-purple-100 text-purple-600"
                  title="리포트 생성"
                  value={`${seasonDashboard.reportStats.generationRate}%`}
                  subtext={`${seasonDashboard.reportStats.generatedReports}/${seasonDashboard.reportStats.totalReports} 생성`}
                />
              </div>

              {/* Detailed Stats */}
              <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                {/* Team Stats Detail */}
                <div className="bg-gray-50 rounded-lg p-4">
                  <h3 className="font-medium text-gray-900 mb-3">팀 상세</h3>
                  <div className="space-y-2 text-sm">
                    <div className="flex justify-between">
                      <span className="text-gray-600">완성 팀</span>
                      <span className="font-medium text-green-600">{seasonDashboard.teamStats.completeTeams}팀</span>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-gray-600">미완성 팀</span>
                      <span className="font-medium text-red-600">{seasonDashboard.teamStats.incompleteTeams}팀</span>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-gray-600">모집 중</span>
                      <span className="font-medium text-blue-600">{seasonDashboard.teamStats.recruitingTeams}팀</span>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-gray-600">총 참가자</span>
                      <span className="font-medium">{seasonDashboard.participantStats.totalParticipants}명</span>
                    </div>
                  </div>
                </div>

                {/* Role Distribution */}
                <div className="bg-gray-50 rounded-lg p-4">
                  <h3 className="font-medium text-gray-900 mb-3">역할 분포</h3>
                  <div className="space-y-2">
                    {Object.entries(seasonDashboard.participantStats.roleDistribution).map(([role, count]) => (
                      <div key={role} className="flex items-center gap-2">
                        <span className="text-sm text-gray-600 w-20">{JOB_ROLE_LABELS[role as JobRole]}</span>
                        <div className="flex-1 bg-gray-200 rounded-full h-2">
                          <div
                            className="bg-primary-500 h-2 rounded-full"
                            style={{
                              width: `${Math.min((count / seasonDashboard.participantStats.totalParticipants) * 100, 100)}%`,
                            }}
                          />
                        </div>
                        <span className="text-sm font-medium w-8 text-right">{count}</span>
                      </div>
                    ))}
                  </div>
                </div>

                {/* Score Distribution */}
                <div className="bg-gray-50 rounded-lg p-4">
                  <h3 className="font-medium text-gray-900 mb-3">점수 분포</h3>
                  <div className="space-y-2 text-sm">
                    <ScoreBar label="우수 (4.5+)" count={seasonDashboard.reviewStats.scoreDistribution.excellent} total={seasonDashboard.reviewStats.totalReviews} color="bg-green-500" />
                    <ScoreBar label="양호 (3.5-4.5)" count={seasonDashboard.reviewStats.scoreDistribution.good} total={seasonDashboard.reviewStats.totalReviews} color="bg-blue-500" />
                    <ScoreBar label="보통 (2.5-3.5)" count={seasonDashboard.reviewStats.scoreDistribution.average} total={seasonDashboard.reviewStats.totalReviews} color="bg-yellow-500" />
                    <ScoreBar label="미흡 (2.5-)" count={seasonDashboard.reviewStats.scoreDistribution.belowAverage} total={seasonDashboard.reviewStats.totalReviews} color="bg-red-500" />
                  </div>
                </div>

                {/* Project Stats Detail */}
                <div className="bg-gray-50 rounded-lg p-4">
                  <h3 className="font-medium text-gray-900 mb-3">프로젝트 상세</h3>
                  <div className="space-y-2 text-sm">
                    <div className="flex justify-between">
                      <span className="text-gray-600">제출 완료</span>
                      <span className="font-medium text-green-600">{seasonDashboard.projectStats.submittedProjects}개</span>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-gray-600">진행 중</span>
                      <span className="font-medium text-blue-600">{seasonDashboard.projectStats.inProgressProjects}개</span>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-gray-600">체크포인트 미제출</span>
                      <span className="font-medium text-red-600">{seasonDashboard.projectStats.checkpointMissingCount}개</span>
                    </div>
                  </div>
                </div>
              </div>

              {/* Risk Teams */}
              {seasonDashboard.riskTeams.length > 0 && (
                <div>
                  <h3 className="font-medium text-gray-900 mb-3 flex items-center gap-2">
                    <AlertTriangle className="w-5 h-5 text-red-500" />
                    위험 팀 목록
                  </h3>
                  <div className="space-y-2">
                    {seasonDashboard.riskTeams.slice(0, 5).map((risk, idx) => (
                      <div
                        key={`${risk.teamId}-${risk.riskType}-${idx}`}
                        className="flex items-center justify-between p-3 bg-gray-50 rounded-lg border border-gray-200"
                      >
                        <div>
                          <p className="font-medium text-gray-900">{risk.teamName}</p>
                          <p className="text-sm text-gray-500">{risk.projectName}</p>
                        </div>
                        <div className="text-right">
                          <span className={`px-2 py-1 rounded text-xs font-medium ${RISK_LEVEL_COLORS[risk.riskLevel]}`}>
                            {RISK_TYPE_LABELS[risk.riskType]}
                          </span>
                          <p className="text-xs text-gray-500 mt-1">{risk.riskDescription}</p>
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
              )}
            </div>
          ) : (
            <div className="text-center py-8 text-gray-500">
              시즌을 선택해주세요.
            </div>
          )}
        </div>
      )}

      {/* Quick Access Cards */}
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

// Helper Components
function StatBox({
  icon: Icon,
  color,
  title,
  value,
  subtext,
}: {
  icon: React.ElementType;
  color: string;
  title: string;
  value: string | number;
  subtext: string;
}) {
  return (
    <div className="bg-white border border-gray-200 rounded-lg p-4">
      <div className="flex items-center gap-2 mb-2">
        <div className={`p-2 rounded-lg ${color}`}>
          <Icon className="w-4 h-4" />
        </div>
        <span className="text-sm text-gray-600">{title}</span>
      </div>
      <p className="text-2xl font-bold text-gray-900">{value}</p>
      <p className="text-xs text-gray-500 mt-1">{subtext}</p>
    </div>
  );
}

function ScoreBar({
  label,
  count,
  total,
  color,
}: {
  label: string;
  count: number;
  total: number;
  color: string;
}) {
  const percentage = total > 0 ? (count / total) * 100 : 0;
  return (
    <div className="flex items-center gap-2">
      <span className="text-gray-600 w-28">{label}</span>
      <div className="flex-1 bg-gray-200 rounded-full h-2">
        <div
          className={`${color} h-2 rounded-full`}
          style={{ width: `${percentage}%` }}
        />
      </div>
      <span className="font-medium w-8 text-right">{count}</span>
    </div>
  );
}
