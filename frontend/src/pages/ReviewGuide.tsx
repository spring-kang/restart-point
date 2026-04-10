import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { reviewService } from '../services/reviewService';
import type { ReviewGuide, ReviewGuideStatus, RubricGuide, ExampleComparison } from '../types';

export default function ReviewGuidePage() {
  const [guide, setGuide] = useState<ReviewGuide | null>(null);
  const [activeStep, setActiveStep] = useState(0);
  const [loading, setLoading] = useState(true);
  const [completing, setCompleting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    loadGuide();
  }, []);

  const loadGuide = async () => {
    try {
      const data = await reviewService.getReviewGuide();
      setGuide(data);

      // 이미 완료된 경우 완료 단계로
      if (data.completionStatus.fullyCompleted) {
        setActiveStep(3);
      } else if (data.completionStatus.exampleComparisonCompleted) {
        setActiveStep(2);
      } else if (data.completionStatus.rubricLearningCompleted) {
        setActiveStep(1);
      }
    } catch {
      setError('가이드를 불러오는데 실패했습니다.');
    } finally {
      setLoading(false);
    }
  };

  const handleCompleteStep = async (step: number) => {
    setCompleting(true);
    try {
      let status: ReviewGuideStatus;
      if (step === 0) {
        status = await reviewService.completeRubricLearning();
      } else if (step === 1) {
        status = await reviewService.completeExampleComparison();
      } else {
        status = await reviewService.completePracticeEvaluation();
      }

      if (guide) {
        setGuide({ ...guide, completionStatus: status });
      }
      setActiveStep(step + 1);
    } catch {
      setError('완료 처리에 실패했습니다.');
    } finally {
      setCompleting(false);
    }
  };

  const handleSkipAll = async () => {
    setCompleting(true);
    try {
      const status = await reviewService.completeGuide();
      if (guide) {
        setGuide({ ...guide, completionStatus: status });
      }
      setActiveStep(3);
    } catch {
      setError('완료 처리에 실패했습니다.');
    } finally {
      setCompleting(false);
    }
  };

  if (loading) {
    return (
      <div className="flex justify-center items-center min-h-screen">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-sky-600"></div>
      </div>
    );
  }

  if (error || !guide) {
    return (
      <div className="max-w-4xl mx-auto p-6">
        <div className="bg-red-50 text-red-700 p-4 rounded-lg">{error}</div>
      </div>
    );
  }

  const steps = ['루브릭 학습', '사례 비교', '연습 평가', '완료'];

  return (
    <div className="max-w-4xl mx-auto p-6 space-y-6">
      {/* 헤더 */}
      <div className="bg-white rounded-lg shadow p-6">
        <div className="flex items-center justify-between mb-4">
          <div>
            <h1 className="text-2xl font-bold text-gray-900">심사 가이드 학습</h1>
            <p className="text-gray-600 mt-1">
              심사 전에 루브릭과 평가 기준을 학습합니다.
            </p>
          </div>
          {!guide.completionStatus.fullyCompleted && (
            <button
              onClick={handleSkipAll}
              disabled={completing}
              className="text-sm text-gray-500 hover:text-gray-700"
            >
              건너뛰기
            </button>
          )}
        </div>

        {/* 진행 단계 */}
        <div className="flex items-center gap-2">
          {steps.map((step, index) => (
            <div key={step} className="flex items-center">
              <div
                className={`w-8 h-8 rounded-full flex items-center justify-center text-sm font-medium ${
                  index < activeStep
                    ? 'bg-green-500 text-white'
                    : index === activeStep
                    ? 'bg-sky-600 text-white'
                    : 'bg-gray-200 text-gray-500'
                }`}
              >
                {index < activeStep ? '✓' : index + 1}
              </div>
              <span
                className={`ml-2 text-sm ${
                  index <= activeStep ? 'text-gray-900' : 'text-gray-400'
                }`}
              >
                {step}
              </span>
              {index < steps.length - 1 && (
                <div className={`w-8 h-0.5 mx-2 ${index < activeStep ? 'bg-green-500' : 'bg-gray-200'}`} />
              )}
            </div>
          ))}
        </div>
      </div>

      {/* 단계별 내용 */}
      {activeStep === 0 && (
        <RubricLearningSection
          rubricGuides={guide.rubricGuides}
          onComplete={() => handleCompleteStep(0)}
          completing={completing}
        />
      )}

      {activeStep === 1 && (
        <ExampleComparisonSection
          examples={guide.exampleComparisons}
          onComplete={() => handleCompleteStep(1)}
          completing={completing}
        />
      )}

      {activeStep === 2 && (
        <PracticeEvaluationSection
          onComplete={() => handleCompleteStep(2)}
          completing={completing}
        />
      )}

      {activeStep === 3 && (
        <CompletionSection />
      )}
    </div>
  );
}

// 루브릭 학습 섹션
function RubricLearningSection({
  rubricGuides,
  onComplete,
  completing,
}: {
  rubricGuides: RubricGuide[];
  onComplete: () => void;
  completing: boolean;
}) {
  const [expandedItem, setExpandedItem] = useState<string | null>(null);

  return (
    <div className="bg-white rounded-lg shadow p-6 space-y-6">
      <div>
        <h2 className="text-xl font-semibold text-gray-900 mb-2">루브릭 항목 학습</h2>
        <p className="text-gray-600">
          각 평가 항목의 기준과 점수별 예시를 확인하세요.
        </p>
      </div>

      <div className="space-y-4">
        {rubricGuides.map((guide) => (
          <div key={guide.rubricItem} className="border border-gray-200 rounded-lg">
            <button
              onClick={() =>
                setExpandedItem(expandedItem === guide.rubricItem ? null : guide.rubricItem)
              }
              className="w-full px-4 py-3 flex items-center justify-between text-left hover:bg-gray-50"
            >
              <div>
                <h3 className="font-medium text-gray-900">{guide.label}</h3>
                <p className="text-sm text-gray-500">{guide.description}</p>
              </div>
              <svg
                className={`w-5 h-5 text-gray-400 transition-transform ${
                  expandedItem === guide.rubricItem ? 'rotate-180' : ''
                }`}
                fill="none"
                stroke="currentColor"
                viewBox="0 0 24 24"
              >
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
              </svg>
            </button>

            {expandedItem === guide.rubricItem && (
              <div className="px-4 pb-4 border-t border-gray-100">
                <div className="mt-3 p-3 bg-sky-50 rounded-lg">
                  <p className="text-sm text-sky-800">
                    <strong>평가 팁:</strong> {guide.evaluationTips}
                  </p>
                </div>

                <div className="mt-4 space-y-3">
                  <h4 className="text-sm font-medium text-gray-700">점수별 예시</h4>
                  {guide.scoreExamples.map((example) => (
                    <div
                      key={example.score}
                      className={`p-3 rounded-lg ${
                        example.score >= 4
                          ? 'bg-green-50'
                          : example.score >= 3
                          ? 'bg-yellow-50'
                          : 'bg-red-50'
                      }`}
                    >
                      <div className="flex items-center gap-2 mb-1">
                        <span
                          className={`px-2 py-0.5 rounded text-sm font-medium ${
                            example.score >= 4
                              ? 'bg-green-200 text-green-800'
                              : example.score >= 3
                              ? 'bg-yellow-200 text-yellow-800'
                              : 'bg-red-200 text-red-800'
                          }`}
                        >
                          {example.score}점
                        </span>
                        <span className="text-sm font-medium text-gray-700">{example.description}</span>
                      </div>
                      <p className="text-sm text-gray-600">{example.example}</p>
                    </div>
                  ))}
                </div>
              </div>
            )}
          </div>
        ))}
      </div>

      <div className="flex justify-end pt-4 border-t">
        <button
          onClick={onComplete}
          disabled={completing}
          className="px-6 py-2 bg-sky-600 text-white rounded-lg hover:bg-sky-700 disabled:opacity-50"
        >
          {completing ? '처리 중...' : '다음 단계로'}
        </button>
      </div>
    </div>
  );
}

// 사례 비교 섹션
function ExampleComparisonSection({
  examples,
  onComplete,
  completing,
}: {
  examples: ExampleComparison[];
  onComplete: () => void;
  completing: boolean;
}) {
  return (
    <div className="bg-white rounded-lg shadow p-6 space-y-6">
      <div>
        <h2 className="text-xl font-semibold text-gray-900 mb-2">우수/보통 사례 비교</h2>
        <p className="text-gray-600">
          우수한 프로젝트와 보통 프로젝트의 차이점을 비교해보세요.
        </p>
      </div>

      <div className="space-y-8">
        {examples.map((comparison, index) => (
          <div key={index} className="border border-gray-200 rounded-lg p-4">
            <h3 className="text-lg font-semibold text-gray-900 mb-4">{comparison.category}</h3>

            <div className="grid md:grid-cols-2 gap-4">
              {/* 우수 사례 */}
              <div className="bg-green-50 border border-green-200 rounded-lg p-4">
                <div className="flex items-center gap-2 mb-3">
                  <span className="px-2 py-1 bg-green-500 text-white text-xs font-medium rounded">우수</span>
                  <span className="text-sm text-green-800">{comparison.excellentExample.expectedScore}점 예상</span>
                </div>
                <h4 className="font-medium text-gray-900 mb-2">{comparison.excellentExample.name}</h4>
                <div className="space-y-2 text-sm text-gray-700">
                  <p><strong>문제 정의:</strong> {comparison.excellentExample.problemDefinition}</p>
                  <p><strong>솔루션:</strong> {comparison.excellentExample.solution}</p>
                  <p><strong>AI 활용:</strong> {comparison.excellentExample.aiUsage}</p>
                </div>
                <div className="mt-3 p-2 bg-green-100 rounded text-sm text-green-800">
                  <strong>점수 이유:</strong> {comparison.excellentExample.reasonForScore}
                </div>
              </div>

              {/* 보통 사례 */}
              <div className="bg-yellow-50 border border-yellow-200 rounded-lg p-4">
                <div className="flex items-center gap-2 mb-3">
                  <span className="px-2 py-1 bg-yellow-500 text-white text-xs font-medium rounded">보통</span>
                  <span className="text-sm text-yellow-800">{comparison.averageExample.expectedScore}점 예상</span>
                </div>
                <h4 className="font-medium text-gray-900 mb-2">{comparison.averageExample.name}</h4>
                <div className="space-y-2 text-sm text-gray-700">
                  <p><strong>문제 정의:</strong> {comparison.averageExample.problemDefinition}</p>
                  <p><strong>솔루션:</strong> {comparison.averageExample.solution}</p>
                  <p><strong>AI 활용:</strong> {comparison.averageExample.aiUsage}</p>
                </div>
                <div className="mt-3 p-2 bg-yellow-100 rounded text-sm text-yellow-800">
                  <strong>점수 이유:</strong> {comparison.averageExample.reasonForScore}
                </div>
              </div>
            </div>

            <div className="mt-4 p-3 bg-sky-50 border border-sky-200 rounded-lg">
              <p className="text-sm text-sky-800">
                <strong>비교 포인트:</strong> {comparison.comparisonNotes}
              </p>
            </div>
          </div>
        ))}
      </div>

      <div className="flex justify-end pt-4 border-t">
        <button
          onClick={onComplete}
          disabled={completing}
          className="px-6 py-2 bg-sky-600 text-white rounded-lg hover:bg-sky-700 disabled:opacity-50"
        >
          {completing ? '처리 중...' : '다음 단계로'}
        </button>
      </div>
    </div>
  );
}

// 연습 평가 섹션
function PracticeEvaluationSection({
  onComplete,
  completing,
}: {
  onComplete: () => void;
  completing: boolean;
}) {
  const [acknowledged, setAcknowledged] = useState(false);

  return (
    <div className="bg-white rounded-lg shadow p-6 space-y-6">
      <div>
        <h2 className="text-xl font-semibold text-gray-900 mb-2">학습 완료 확인</h2>
        <p className="text-gray-600">
          루브릭 항목과 사례 비교를 학습하셨습니다. 이제 실제 심사를 진행할 준비가 되었습니다.
        </p>
      </div>

      <div className="bg-sky-50 border border-sky-200 rounded-lg p-6">
        <h3 className="font-medium text-sky-900 mb-3">심사 시 유의사항</h3>
        <ul className="space-y-2 text-sm text-sky-800">
          <li className="flex items-start gap-2">
            <span className="text-sky-600 mt-0.5">1.</span>
            <span>각 루브릭 항목을 객관적으로 평가해주세요.</span>
          </li>
          <li className="flex items-start gap-2">
            <span className="text-sky-600 mt-0.5">2.</span>
            <span>점수만큼이나 코멘트도 중요합니다. 구체적인 피드백을 남겨주세요.</span>
          </li>
          <li className="flex items-start gap-2">
            <span className="text-sky-600 mt-0.5">3.</span>
            <span>개인적인 선호보다 평가 기준에 따라 판단해주세요.</span>
          </li>
          <li className="flex items-start gap-2">
            <span className="text-sky-600 mt-0.5">4.</span>
            <span>심사 후에는 평가 패턴 분석을 통해 자신의 심사 경향을 확인할 수 있습니다.</span>
          </li>
        </ul>
      </div>

      <label className="flex items-center gap-3 cursor-pointer">
        <input
          type="checkbox"
          checked={acknowledged}
          onChange={(e) => setAcknowledged(e.target.checked)}
          className="w-5 h-5 rounded border-gray-300 text-sky-600 focus:ring-sky-500"
        />
        <span className="text-gray-700">
          위 내용을 확인했으며, 공정한 심사를 진행하겠습니다.
        </span>
      </label>

      <div className="flex justify-end pt-4 border-t">
        <button
          onClick={onComplete}
          disabled={completing || !acknowledged}
          className="px-6 py-2 bg-sky-600 text-white rounded-lg hover:bg-sky-700 disabled:opacity-50"
        >
          {completing ? '처리 중...' : '학습 완료'}
        </button>
      </div>
    </div>
  );
}

// 완료 섹션
function CompletionSection() {
  return (
    <div className="bg-white rounded-lg shadow p-8 text-center space-y-6">
      <div className="w-16 h-16 bg-green-100 rounded-full flex items-center justify-center mx-auto">
        <svg className="w-8 h-8 text-green-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
        </svg>
      </div>

      <div>
        <h2 className="text-xl font-semibold text-gray-900 mb-2">학습 완료!</h2>
        <p className="text-gray-600">
          심사 가이드 학습을 완료했습니다. 이제 프로젝트 심사를 진행할 수 있습니다.
        </p>
      </div>

      <div className="flex justify-center gap-4">
        <Link
          to="/seasons"
          className="px-6 py-2 bg-sky-600 text-white rounded-lg hover:bg-sky-700"
        >
          심사하러 가기
        </Link>
        <Link
          to="/my-reviews/pattern"
          className="px-6 py-2 bg-gray-200 text-gray-700 rounded-lg hover:bg-gray-300"
        >
          내 심사 패턴 보기
        </Link>
      </div>
    </div>
  );
}
