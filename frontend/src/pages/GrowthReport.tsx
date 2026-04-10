import { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import { growthReportService } from '../services/growthReportService';
import type { GrowthReport } from '../types';

export default function GrowthReportPage() {
  const { projectId } = useParams<{ projectId: string }>();
  const [teamReport, setTeamReport] = useState<GrowthReport | null>(null);
  const [individualReport, setIndividualReport] = useState<GrowthReport | null>(null);
  const [activeTab, setActiveTab] = useState<'team' | 'individual'>('team');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [regenerating, setRegenerating] = useState(false);

  useEffect(() => {
    if (projectId) {
      loadReports();
    }
  }, [projectId]);

  const loadReports = async () => {
    setLoading(true);
    setError(null);

    try {
      const [team, individual] = await Promise.all([
        growthReportService.getTeamReport(Number(projectId)).catch(() => null),
        growthReportService.getMyIndividualReport(Number(projectId)).catch(() => null),
      ]);

      setTeamReport(team);
      setIndividualReport(individual);
    } catch {
      setError('리포트를 불러오는데 실패했습니다.');
    } finally {
      setLoading(false);
    }
  };

  const handleRegenerate = async (reportId: number) => {
    setRegenerating(true);
    try {
      const regenerated = await growthReportService.regenerateReport(reportId);
      if (regenerated.reportType === 'TEAM') {
        setTeamReport(regenerated);
      } else {
        setIndividualReport(regenerated);
      }
    } catch {
      setError('리포트 재생성에 실패했습니다.');
    } finally {
      setRegenerating(false);
    }
  };

  if (loading) {
    return (
      <div className="flex justify-center items-center min-h-screen">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-sky-600"></div>
      </div>
    );
  }

  if (error && !teamReport && !individualReport) {
    return (
      <div className="max-w-4xl mx-auto p-6">
        <div className="bg-red-50 text-red-700 p-4 rounded-lg">{error}</div>
      </div>
    );
  }

  const currentReport = activeTab === 'team' ? teamReport : individualReport;

  return (
    <div className="max-w-4xl mx-auto p-6 space-y-6">
      {/* 헤더 */}
      <div className="bg-white rounded-lg shadow p-6">
        <div className="flex items-center gap-4 mb-4">
          <Link to="/my-team" className="text-gray-500 hover:text-gray-700">
            ← 돌아가기
          </Link>
        </div>
        <h1 className="text-2xl font-bold text-gray-900 mb-2">성장 리포트</h1>
        {currentReport && (
          <div className="flex items-center gap-4 text-sm text-gray-600">
            <span>{currentReport.teamName}</span>
            <span>•</span>
            <span>{currentReport.projectName}</span>
            {currentReport.averageScore && (
              <>
                <span>•</span>
                <span className="text-sky-600 font-medium">
                  평균 {currentReport.averageScore.toFixed(1)}점
                </span>
              </>
            )}
          </div>
        )}
      </div>

      {/* 탭 */}
      <div className="bg-white rounded-lg shadow">
        <div className="border-b border-gray-200">
          <nav className="flex -mb-px">
            <button
              onClick={() => setActiveTab('team')}
              className={`px-6 py-4 text-sm font-medium border-b-2 transition ${
                activeTab === 'team'
                  ? 'border-sky-600 text-sky-600'
                  : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
              }`}
            >
              팀 리포트
            </button>
            <button
              onClick={() => setActiveTab('individual')}
              className={`px-6 py-4 text-sm font-medium border-b-2 transition ${
                activeTab === 'individual'
                  ? 'border-sky-600 text-sky-600'
                  : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
              }`}
            >
              개인 리포트
            </button>
          </nav>
        </div>

        {/* 리포트 내용 */}
        <div className="p-6">
          {!currentReport ? (
            <div className="text-center py-12 text-gray-500">
              {activeTab === 'team' ? '팀 리포트가 아직 생성되지 않았습니다.' : '개인 리포트가 아직 생성되지 않았습니다.'}
            </div>
          ) : !currentReport.generated ? (
            <div className="text-center py-12">
              <p className="text-gray-500 mb-4">리포트 생성이 완료되지 않았습니다.</p>
              <button
                onClick={() => handleRegenerate(currentReport.id)}
                disabled={regenerating}
                className="px-4 py-2 bg-sky-600 text-white rounded-lg hover:bg-sky-700 disabled:opacity-50"
              >
                {regenerating ? '생성 중...' : '리포트 생성하기'}
              </button>
            </div>
          ) : (
            <div className="space-y-8">
              {/* 팀 리포트 내용 */}
              {activeTab === 'team' && teamReport && (
                <>
                  {teamReport.teamStrengths && (
                    <ReportSection
                      title="💪 팀의 강점"
                      content={teamReport.teamStrengths}
                      color="green"
                    />
                  )}
                  {teamReport.teamImprovements && (
                    <ReportSection
                      title="📈 개선할 점"
                      content={teamReport.teamImprovements}
                      color="amber"
                    />
                  )}
                  {teamReport.nextProjectActions && (
                    <ReportSection
                      title="🎯 다음 프로젝트 액션"
                      content={teamReport.nextProjectActions}
                      color="sky"
                    />
                  )}
                  {teamReport.recommendedAreas && (
                    <ReportSection
                      title="🚀 추천 도전 영역"
                      content={teamReport.recommendedAreas}
                      color="purple"
                    />
                  )}
                </>
              )}

              {/* 개인 리포트 내용 */}
              {activeTab === 'individual' && individualReport && (
                <>
                  {individualReport.roleSpecificFeedback && (
                    <ReportSection
                      title="👤 역할별 피드백"
                      content={individualReport.roleSpecificFeedback}
                      color="sky"
                    />
                  )}
                  {individualReport.nextProjectActions && (
                    <ReportSection
                      title="🎯 다음 프로젝트 액션"
                      content={individualReport.nextProjectActions}
                      color="green"
                    />
                  )}
                  {individualReport.portfolioImprovements && (
                    <ReportSection
                      title="📂 포트폴리오 보완 포인트"
                      content={individualReport.portfolioImprovements}
                      color="amber"
                    />
                  )}
                  {individualReport.recommendedAreas && (
                    <ReportSection
                      title="📚 추천 학습 영역"
                      content={individualReport.recommendedAreas}
                      color="purple"
                    />
                  )}
                </>
              )}
            </div>
          )}
        </div>
      </div>

      {/* 리포트 메타 정보 */}
      {currentReport && currentReport.generated && (
        <div className="bg-white rounded-lg shadow p-6">
          <div className="flex justify-between items-center">
            <div className="text-sm text-gray-500">
              생성일: {new Date(currentReport.createdAt).toLocaleDateString('ko-KR')}
            </div>
            <button
              onClick={() => handleRegenerate(currentReport.id)}
              disabled={regenerating}
              className="text-sm text-sky-600 hover:text-sky-700 disabled:opacity-50"
            >
              {regenerating ? '재생성 중...' : '리포트 재생성'}
            </button>
          </div>
        </div>
      )}
    </div>
  );
}

interface ReportSectionProps {
  title: string;
  content: string;
  color: 'green' | 'amber' | 'sky' | 'purple';
}

function ReportSection({ title, content, color }: ReportSectionProps) {
  const colorClasses = {
    green: 'bg-green-50 border-green-200',
    amber: 'bg-amber-50 border-amber-200',
    sky: 'bg-sky-50 border-sky-200',
    purple: 'bg-purple-50 border-purple-200',
  };

  // 마크다운 리스트를 HTML로 간단히 변환
  const formatContent = (text: string) => {
    return text
      .split('\n')
      .map((line, index) => {
        // 리스트 아이템 처리
        if (line.trim().startsWith('- ') || line.trim().startsWith('* ')) {
          return (
            <li key={index} className="ml-4 list-disc">
              {line.trim().substring(2)}
            </li>
          );
        }
        // 빈 줄 처리
        if (line.trim() === '') {
          return <br key={index} />;
        }
        // 일반 텍스트
        return (
          <p key={index} className="mb-2">
            {line}
          </p>
        );
      });
  };

  return (
    <div className={`rounded-lg border p-6 ${colorClasses[color]}`}>
      <h3 className="text-lg font-semibold text-gray-900 mb-4">{title}</h3>
      <div className="text-gray-700 text-sm leading-relaxed">
        {formatContent(content)}
      </div>
    </div>
  );
}
