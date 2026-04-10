import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { reviewService, RUBRIC_ITEM_LABELS } from '../services/reviewService';
import type { ReviewPatternAnalysis, RubricItem } from '../types';

export default function ReviewPatternPage() {
  const [analysis, setAnalysis] = useState<ReviewPatternAnalysis | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    loadAnalysis();
  }, []);

  const loadAnalysis = async () => {
    try {
      const data = await reviewService.getMyReviewPattern();
      setAnalysis(data);
    } catch {
      setError('패턴 분석을 불러오는데 실패했습니다.');
    } finally {
      setLoading(false);
    }
  };

  if (loading) {
    return (
      <div className="flex justify-center items-center min-h-screen">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-sky-600"></div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="max-w-4xl mx-auto p-6">
        <div className="bg-red-50 text-red-700 p-4 rounded-lg">{error}</div>
      </div>
    );
  }

  if (!analysis) {
    return null;
  }

  return (
    <div className="max-w-4xl mx-auto p-6 space-y-6">
      {/* 헤더 */}
      <div className="bg-white rounded-lg shadow p-6">
        <div className="flex items-center gap-4 mb-4">
          <Link to="/seasons" className="text-gray-500 hover:text-gray-700">
            ← 돌아가기
          </Link>
        </div>
        <h1 className="text-2xl font-bold text-gray-900 mb-2">내 심사 패턴 분석</h1>
        <p className="text-gray-600">
          AI가 분석한 나의 심사 경향과 개선점을 확인하세요.
        </p>
      </div>

      {analysis.totalReviewCount === 0 ? (
        <NoReviewsCard />
      ) : (
        <>
          {/* 요약 통계 */}
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            <StatCard
              label="총 심사 횟수"
              value={`${analysis.totalReviewCount}회`}
              color="sky"
            />
            <StatCard
              label="평균 점수"
              value={`${analysis.averageScore.toFixed(1)}점`}
              color="green"
            />
            <StatCard
              label="점수 분포"
              value={getMostCommonScore(analysis.scoreDistribution)}
              color="purple"
            />
          </div>

          {/* AI 분석 결과 */}
          <div className="bg-white rounded-lg shadow p-6 space-y-6">
            <h2 className="text-lg font-semibold text-gray-900">AI 분석 결과</h2>

            {/* 전반적 경향 */}
            <div className="bg-sky-50 border border-sky-200 rounded-lg p-4">
              <h3 className="font-medium text-sky-900 mb-2">전반적 평가 경향</h3>
              <p className="text-sky-800">{analysis.overallTendency}</p>
            </div>

            <div className="grid md:grid-cols-2 gap-4">
              {/* 강점 */}
              {analysis.strengths && (
                <div className="bg-green-50 border border-green-200 rounded-lg p-4">
                  <h3 className="font-medium text-green-900 mb-2">잘하는 점</h3>
                  <p className="text-green-800 text-sm whitespace-pre-line">{analysis.strengths}</p>
                </div>
              )}

              {/* 개선점 */}
              {analysis.areasForImprovement && (
                <div className="bg-amber-50 border border-amber-200 rounded-lg p-4">
                  <h3 className="font-medium text-amber-900 mb-2">개선할 점</h3>
                  <p className="text-amber-800 text-sm whitespace-pre-line">{analysis.areasForImprovement}</p>
                </div>
              )}
            </div>

            {/* 전문가 비교 */}
            {analysis.comparisonWithExperts && (
              <div className="bg-purple-50 border border-purple-200 rounded-lg p-4">
                <h3 className="font-medium text-purple-900 mb-2">전문가 심사와 비교</h3>
                <p className="text-purple-800 text-sm">{analysis.comparisonWithExperts}</p>
              </div>
            )}

            {/* 추천 */}
            {analysis.recommendations && (
              <div className="bg-gray-50 border border-gray-200 rounded-lg p-4">
                <h3 className="font-medium text-gray-900 mb-2">다음 심사를 위한 조언</h3>
                <p className="text-gray-700 text-sm">{analysis.recommendations}</p>
              </div>
            )}
          </div>

          {/* 루브릭별 평균 점수 */}
          <div className="bg-white rounded-lg shadow p-6">
            <h2 className="text-lg font-semibold text-gray-900 mb-4">루브릭별 평균 점수</h2>
            <div className="space-y-4">
              {Object.entries(analysis.rubricAverages).map(([item, score]) => (
                <div key={item}>
                  <div className="flex justify-between text-sm mb-1">
                    <span className="text-gray-700">
                      {RUBRIC_ITEM_LABELS[item as RubricItem] || item}
                    </span>
                    <span className="font-medium text-gray-900">{score.toFixed(1)}점</span>
                  </div>
                  <div className="h-2 bg-gray-200 rounded-full overflow-hidden">
                    <div
                      className={`h-full rounded-full transition-all ${getScoreColor(score)}`}
                      style={{ width: `${(score / 5) * 100}%` }}
                    />
                  </div>
                </div>
              ))}
            </div>
          </div>

          {/* 점수 분포 */}
          <div className="bg-white rounded-lg shadow p-6">
            <h2 className="text-lg font-semibold text-gray-900 mb-4">점수 분포</h2>
            <div className="flex items-end gap-2 h-40">
              {[1, 2, 3, 4, 5].map((score) => {
                const count = getScoreCount(analysis.scoreDistribution, score);
                const total = getTotalCount(analysis.scoreDistribution);
                const percentage = total > 0 ? (count / total) * 100 : 0;

                return (
                  <div key={score} className="flex-1 flex flex-col items-center">
                    <div className="w-full flex flex-col items-center">
                      <span className="text-xs text-gray-500 mb-1">{count}회</span>
                      <div
                        className={`w-full rounded-t ${getBarColor(score)}`}
                        style={{ height: `${Math.max(percentage * 1.2, 4)}px` }}
                      />
                    </div>
                    <span className="text-sm font-medium text-gray-700 mt-2">{score}점</span>
                  </div>
                );
              })}
            </div>
          </div>
        </>
      )}
    </div>
  );
}

function NoReviewsCard() {
  return (
    <div className="bg-white rounded-lg shadow p-8 text-center">
      <div className="w-16 h-16 bg-gray-100 rounded-full flex items-center justify-center mx-auto mb-4">
        <svg className="w-8 h-8 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2" />
        </svg>
      </div>
      <h2 className="text-lg font-semibold text-gray-900 mb-2">아직 심사 기록이 없습니다</h2>
      <p className="text-gray-600 mb-6">
        프로젝트 심사에 참여하면 평가 패턴 분석을 받을 수 있습니다.
      </p>
      <Link
        to="/seasons"
        className="inline-block px-6 py-2 bg-sky-600 text-white rounded-lg hover:bg-sky-700"
      >
        심사하러 가기
      </Link>
    </div>
  );
}

function StatCard({ label, value, color }: { label: string; value: string; color: 'sky' | 'green' | 'purple' }) {
  const colorClasses = {
    sky: 'bg-sky-50 border-sky-200',
    green: 'bg-green-50 border-green-200',
    purple: 'bg-purple-50 border-purple-200',
  };

  return (
    <div className={`rounded-lg border p-4 ${colorClasses[color]}`}>
      <p className="text-sm text-gray-600">{label}</p>
      <p className="text-2xl font-bold text-gray-900 mt-1">{value}</p>
    </div>
  );
}

function getScoreColor(score: number): string {
  if (score >= 4) return 'bg-green-500';
  if (score >= 3) return 'bg-yellow-500';
  return 'bg-red-500';
}

function getBarColor(score: number): string {
  if (score >= 4) return 'bg-green-400';
  if (score >= 3) return 'bg-yellow-400';
  if (score >= 2) return 'bg-orange-400';
  return 'bg-red-400';
}

function getScoreCount(dist: ReviewPatternAnalysis['scoreDistribution'], score: number): number {
  const key = `score${score}Count` as keyof typeof dist;
  return dist[key] || 0;
}

function getTotalCount(dist: ReviewPatternAnalysis['scoreDistribution']): number {
  return dist.score1Count + dist.score2Count + dist.score3Count + dist.score4Count + dist.score5Count;
}

function getMostCommonScore(dist: ReviewPatternAnalysis['scoreDistribution']): string {
  const scores = [
    { score: 1, count: dist.score1Count },
    { score: 2, count: dist.score2Count },
    { score: 3, count: dist.score3Count },
    { score: 4, count: dist.score4Count },
    { score: 5, count: dist.score5Count },
  ];

  const max = scores.reduce((prev, curr) => (curr.count > prev.count ? curr : prev));
  return max.count > 0 ? `${max.score}점이 가장 많음` : '-';
}
