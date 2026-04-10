import { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import { ArrowLeft, AlertTriangle, TrendingUp, TrendingDown, BarChart3 } from 'lucide-react';
import { adminService } from '../services/adminService';
import type { ReviewAnalysis, RubricItem } from '../types';
import { RUBRIC_ITEM_LABELS } from '../types';

export default function ReviewAnalysisPage() {
  const { seasonId } = useParams<{ seasonId: string }>();
  const [analyses, setAnalyses] = useState<ReviewAnalysis[]>([]);
  const [selectedAnalysis, setSelectedAnalysis] = useState<ReviewAnalysis | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (seasonId) {
      loadAnalyses();
    }
  }, [seasonId]);

  const loadAnalyses = async () => {
    try {
      setLoading(true);
      setError(null);
      const data = await adminService.getSeasonReviewAnalysis(Number(seasonId));
      setAnalyses(data);
      if (data.length > 0) {
        setSelectedAnalysis(data[0]);
      }
    } catch (err) {
      console.error('Failed to load review analysis:', err);
      setError('심사 분석을 불러오는데 실패했습니다.');
    } finally {
      setLoading(false);
    }
  };

  if (loading) {
    return (
      <div className="flex justify-center items-center min-h-[400px]">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="bg-red-50 text-red-700 p-4 rounded-lg">{error}</div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center gap-4">
        <Link
          to="/seasons"
          className="p-2 hover:bg-gray-100 rounded-lg transition"
        >
          <ArrowLeft className="w-5 h-5" />
        </Link>
        <div>
          <h1 className="text-2xl font-bold text-gray-900">AI 심사 분석</h1>
          <p className="text-gray-600">시즌 {seasonId}의 심사 데이터 분석 결과</p>
        </div>
      </div>

      {analyses.length === 0 ? (
        <div className="bg-white rounded-lg shadow p-8 text-center">
          <BarChart3 className="w-12 h-12 text-gray-400 mx-auto mb-4" />
          <p className="text-gray-500">분석할 심사 데이터가 없습니다.</p>
        </div>
      ) : (
        <div className="grid grid-cols-1 lg:grid-cols-4 gap-6">
          {/* Project List Sidebar */}
          <div className="lg:col-span-1">
            <div className="bg-white rounded-lg shadow p-4 sticky top-4">
              <h2 className="font-semibold text-gray-900 mb-3">프로젝트 목록</h2>
              <div className="space-y-2 max-h-[calc(100vh-200px)] overflow-y-auto">
                {analyses.map((analysis) => (
                  <button
                    key={analysis.projectId}
                    onClick={() => setSelectedAnalysis(analysis)}
                    className={`w-full text-left p-3 rounded-lg transition ${
                      selectedAnalysis?.projectId === analysis.projectId
                        ? 'bg-primary-50 border border-primary-300'
                        : 'bg-gray-50 hover:bg-gray-100'
                    }`}
                  >
                    <div className="font-medium text-gray-900 truncate">
                      {analysis.projectName}
                    </div>
                    <div className="text-sm text-gray-500">{analysis.teamName}</div>
                    <div className="text-sm text-gray-600 mt-1">
                      평균 {analysis.overallAverageScore.toFixed(1)}점 · {analysis.totalReviewCount}명
                    </div>
                  </button>
                ))}
              </div>
            </div>
          </div>

          {/* Detail Analysis */}
          <div className="lg:col-span-3 space-y-6">
            {selectedAnalysis && (
              <>
                {/* Stats Cards */}
                <div className="bg-white rounded-lg shadow p-6">
                  <h2 className="text-lg font-semibold text-gray-900 mb-4">
                    {selectedAnalysis.projectName}
                  </h2>
                  <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
                    <StatCard
                      label="전체 평균"
                      value={`${selectedAnalysis.overallAverageScore.toFixed(1)}점`}
                      subValue={`${selectedAnalysis.totalReviewCount}명 심사`}
                    />
                    <StatCard
                      label="현직자 평균"
                      value={`${selectedAnalysis.expertAverageScore.toFixed(1)}점`}
                      subValue={`${selectedAnalysis.expertReviewCount}명`}
                      color="blue"
                    />
                    <StatCard
                      label="예비참여자 평균"
                      value={`${selectedAnalysis.candidateAverageScore.toFixed(1)}점`}
                      subValue={`${selectedAnalysis.candidateReviewCount}명`}
                      color="purple"
                    />
                    <StatCard
                      label="점수 차이"
                      value={`${selectedAnalysis.scoreDifference >= 0 ? '+' : ''}${selectedAnalysis.scoreDifference.toFixed(1)}점`}
                      icon={selectedAnalysis.scoreDifference > 0 ? TrendingUp : TrendingDown}
                      color={Math.abs(selectedAnalysis.scoreDifference) > 0.5 ? 'red' : 'gray'}
                    />
                  </div>
                </div>

                {/* AI Comment Summary */}
                <div className="bg-white rounded-lg shadow p-6">
                  <h3 className="font-semibold text-gray-900 mb-3">AI 코멘트 요약</h3>
                  <p className="text-gray-700 bg-gray-50 p-4 rounded-lg">
                    {selectedAnalysis.commentSummary}
                  </p>
                </div>

                {/* Strengths & Weaknesses */}
                <div className="grid md:grid-cols-2 gap-4">
                  <div className="bg-green-50 rounded-lg p-4 border border-green-200">
                    <h3 className="font-semibold text-green-900 mb-3 flex items-center gap-2">
                      <TrendingUp className="w-5 h-5" />
                      주요 강점
                    </h3>
                    {selectedAnalysis.strengths.length > 0 ? (
                      <ul className="space-y-2">
                        {selectedAnalysis.strengths.map((strength, idx) => (
                          <li key={idx} className="text-green-800 text-sm flex items-start gap-2">
                            <span className="text-green-500 mt-0.5">✓</span>
                            {strength}
                          </li>
                        ))}
                      </ul>
                    ) : (
                      <p className="text-green-700 text-sm">분석된 강점이 없습니다.</p>
                    )}
                  </div>
                  <div className="bg-amber-50 rounded-lg p-4 border border-amber-200">
                    <h3 className="font-semibold text-amber-900 mb-3 flex items-center gap-2">
                      <AlertTriangle className="w-5 h-5" />
                      개선 필요 사항
                    </h3>
                    {selectedAnalysis.weaknesses.length > 0 ? (
                      <ul className="space-y-2">
                        {selectedAnalysis.weaknesses.map((weakness, idx) => (
                          <li key={idx} className="text-amber-800 text-sm flex items-start gap-2">
                            <span className="text-amber-500 mt-0.5">!</span>
                            {weakness}
                          </li>
                        ))}
                      </ul>
                    ) : (
                      <p className="text-amber-700 text-sm">분석된 개선사항이 없습니다.</p>
                    )}
                  </div>
                </div>

                {/* Expert vs Candidate Analysis */}
                <div className="bg-purple-50 rounded-lg p-4 border border-purple-200">
                  <h3 className="font-semibold text-purple-900 mb-3">현직자 vs 예비참여자 분석</h3>
                  <p className="text-purple-800 text-sm">{selectedAnalysis.expertVsCandidateAnalysis}</p>
                </div>

                {/* Rubric Analysis */}
                <div className="bg-white rounded-lg shadow p-6">
                  <h3 className="font-semibold text-gray-900 mb-4">루브릭별 분석</h3>
                  <div className="space-y-4">
                    {selectedAnalysis.rubricAnalyses.map((rubric) => (
                      <div key={rubric.rubricItem} className="border border-gray-200 rounded-lg p-4">
                        <div className="flex justify-between items-start mb-2">
                          <span className="font-medium text-gray-900">
                            {RUBRIC_ITEM_LABELS[rubric.rubricItem as RubricItem] || rubric.label}
                          </span>
                          <div className="text-right text-sm">
                            <div className="text-gray-900 font-medium">{rubric.averageScore.toFixed(1)}점</div>
                            <div className="text-gray-500">
                              현직자 {rubric.expertAverageScore.toFixed(1)} / 예비 {rubric.candidateAverageScore.toFixed(1)}
                            </div>
                          </div>
                        </div>
                        <div className="h-2 bg-gray-200 rounded-full overflow-hidden mb-2">
                          <div
                            className={`h-full rounded-full ${getScoreColor(rubric.averageScore)}`}
                            style={{ width: `${(rubric.averageScore / 5) * 100}%` }}
                          />
                        </div>
                        <p className="text-sm text-gray-600">{rubric.aiInsight}</p>
                      </div>
                    ))}
                  </div>
                </div>

                {/* Outliers */}
                {selectedAnalysis.outliers.length > 0 && (
                  <div className="bg-white rounded-lg shadow p-6">
                    <h3 className="font-semibold text-gray-900 mb-4 flex items-center gap-2">
                      <AlertTriangle className="w-5 h-5 text-amber-500" />
                      이상치 감지
                      <span className="text-sm font-normal text-gray-500">
                        (평균 대비 1.5점 이상 편차)
                      </span>
                    </h3>
                    <div className="space-y-3">
                      {selectedAnalysis.outliers.map((outlier, idx) => (
                        <div
                          key={idx}
                          className={`p-3 rounded-lg border ${
                            outlier.deviation > 0
                              ? 'bg-blue-50 border-blue-200'
                              : 'bg-red-50 border-red-200'
                          }`}
                        >
                          <div className="flex justify-between items-start mb-1">
                            <div>
                              <span className="font-medium text-gray-900">
                                {RUBRIC_ITEM_LABELS[outlier.rubricItem as RubricItem]}
                              </span>
                              <span className={`ml-2 text-xs px-2 py-0.5 rounded ${
                                outlier.reviewType === 'EXPERT'
                                  ? 'bg-blue-100 text-blue-700'
                                  : 'bg-purple-100 text-purple-700'
                              }`}>
                                {outlier.reviewType === 'EXPERT' ? '현직자' : '예비참여자'}
                              </span>
                            </div>
                            <div className="text-right">
                              <span className="font-bold">{outlier.score}점</span>
                              <span className="text-gray-500 text-sm ml-1">
                                (평균 {outlier.averageScore.toFixed(1)}, 편차 {outlier.deviation >= 0 ? '+' : ''}{outlier.deviation.toFixed(1)})
                              </span>
                            </div>
                          </div>
                          <p className="text-sm text-gray-600">
                            <span className="font-medium">{outlier.reviewerName}</span>: {outlier.possibleReason}
                          </p>
                        </div>
                      ))}
                    </div>
                  </div>
                )}
              </>
            )}
          </div>
        </div>
      )}
    </div>
  );
}

interface StatCardProps {
  label: string;
  value: string;
  subValue?: string;
  color?: 'gray' | 'blue' | 'purple' | 'green' | 'red';
  icon?: React.ComponentType<{ className?: string }>;
}

function StatCard({ label, value, subValue, color = 'gray', icon: Icon }: StatCardProps) {
  const colorClasses = {
    gray: 'bg-gray-50 border-gray-200',
    blue: 'bg-blue-50 border-blue-200',
    purple: 'bg-purple-50 border-purple-200',
    green: 'bg-green-50 border-green-200',
    red: 'bg-red-50 border-red-200',
  };

  return (
    <div className={`rounded-lg border p-3 ${colorClasses[color]}`}>
      <p className="text-sm text-gray-600 flex items-center gap-1">
        {Icon && <Icon className="w-4 h-4" />}
        {label}
      </p>
      <p className="text-xl font-bold text-gray-900">{value}</p>
      {subValue && <p className="text-xs text-gray-500">{subValue}</p>}
    </div>
  );
}

function getScoreColor(score: number): string {
  if (score >= 4) return 'bg-green-500';
  if (score >= 3) return 'bg-yellow-500';
  return 'bg-red-500';
}
