import { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import { reviewService, RUBRIC_ITEM_LABELS, RUBRIC_ITEM_DESCRIPTIONS, SCORE_LABELS } from '../services/reviewService';
import { PROJECT_STATUS_LABELS } from '../services/projectService';
import type { Project, RubricItem, ReviewCreateRequest, ReviewScoreRequest, Review } from '../types';

const RUBRIC_ITEMS: RubricItem[] = [
  'PROBLEM_DEFINITION',
  'USER_VALUE',
  'AI_USAGE',
  'UX_COMPLETENESS',
  'TECHNICAL_FEASIBILITY',
  'COLLABORATION',
];

export default function ReviewProjects() {
  const { seasonId } = useParams<{ seasonId: string }>();
  const [projects, setProjects] = useState<Project[]>([]);
  const [myReviews, setMyReviews] = useState<Review[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // 심사 모달 상태
  const [selectedProject, setSelectedProject] = useState<Project | null>(null);
  const [showReviewModal, setShowReviewModal] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [reviewError, setReviewError] = useState<string | null>(null);

  // 심사 폼 상태
  const [scores, setScores] = useState<Record<RubricItem, number>>({
    PROBLEM_DEFINITION: 0,
    USER_VALUE: 0,
    AI_USAGE: 0,
    UX_COMPLETENESS: 0,
    TECHNICAL_FEASIBILITY: 0,
    COLLABORATION: 0,
  });
  const [comments, setComments] = useState<Record<RubricItem, string>>({
    PROBLEM_DEFINITION: '',
    USER_VALUE: '',
    AI_USAGE: '',
    UX_COMPLETENESS: '',
    TECHNICAL_FEASIBILITY: '',
    COLLABORATION: '',
  });
  const [overallComment, setOverallComment] = useState('');

  useEffect(() => {
    if (seasonId) {
      loadData();
    }
  }, [seasonId]);

  const loadData = async () => {
    setLoading(true);
    setError(null);

    try {
      const [projectsData, reviewsData] = await Promise.all([
        reviewService.getReviewableProjects(Number(seasonId)),
        reviewService.getMyReviews(),
      ]);
      setProjects(projectsData);
      setMyReviews(reviewsData);
    } catch {
      setError('데이터를 불러오는데 실패했습니다.');
    } finally {
      setLoading(false);
    }
  };

  const openReviewModal = (project: Project) => {
    // 목록에서 이미 전체 프로젝트 정보를 가져오므로 바로 사용
    setSelectedProject(project);
    setShowReviewModal(true);
    resetForm();
  };

  const resetForm = () => {
    setScores({
      PROBLEM_DEFINITION: 0,
      USER_VALUE: 0,
      AI_USAGE: 0,
      UX_COMPLETENESS: 0,
      TECHNICAL_FEASIBILITY: 0,
      COLLABORATION: 0,
    });
    setComments({
      PROBLEM_DEFINITION: '',
      USER_VALUE: '',
      AI_USAGE: '',
      UX_COMPLETENESS: '',
      TECHNICAL_FEASIBILITY: '',
      COLLABORATION: '',
    });
    setOverallComment('');
    setReviewError(null);
  };

  const handleScoreChange = (item: RubricItem, score: number) => {
    setScores(prev => ({ ...prev, [item]: score }));
  };

  const handleCommentChange = (item: RubricItem, comment: string) => {
    setComments(prev => ({ ...prev, [item]: comment }));
  };

  const handleSubmitReview = async () => {
    if (!selectedProject) return;

    // 모든 항목에 점수가 입력되었는지 확인
    const allScored = RUBRIC_ITEMS.every(item => scores[item] >= 1 && scores[item] <= 5);
    if (!allScored) {
      setReviewError('모든 항목에 점수를 입력해주세요.');
      return;
    }

    setSubmitting(true);
    setReviewError(null);

    try {
      const scoreRequests: ReviewScoreRequest[] = RUBRIC_ITEMS.map(item => ({
        rubricItem: item,
        score: scores[item],
        comment: comments[item] || undefined,
      }));

      const request: ReviewCreateRequest = {
        scores: scoreRequests,
        overallComment: overallComment || undefined,
      };

      await reviewService.createReview(selectedProject.id, request);
      setShowReviewModal(false);
      await loadData(); // 목록 새로고침
    } catch (err: unknown) {
      if (
        typeof err === 'object' &&
        err !== null &&
        'response' in err &&
        typeof (err as { response?: { data?: { message?: string } } }).response?.data?.message === 'string'
      ) {
        setReviewError((err as { response: { data: { message: string } } }).response.data.message);
      } else {
        setReviewError('심사 제출에 실패했습니다.');
      }
    } finally {
      setSubmitting(false);
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

  return (
    <div className="max-w-6xl mx-auto p-6 space-y-6">
      {/* 헤더 */}
      <div className="bg-white rounded-lg shadow p-6">
        <h1 className="text-2xl font-bold text-gray-900 mb-2">프로젝트 심사</h1>
        <p className="text-gray-600">
          제출된 프로젝트를 심사하고 피드백을 남겨주세요.
        </p>
        <div className="flex gap-3 mt-4">
          <Link
            to="/review-guide"
            className="inline-flex items-center px-4 py-2 bg-sky-50 text-sky-700 rounded-lg hover:bg-sky-100 transition"
          >
            <svg className="w-5 h-5 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253" />
            </svg>
            심사 가이드
          </Link>
          <Link
            to="/my-reviews/pattern"
            className="inline-flex items-center px-4 py-2 bg-purple-50 text-purple-700 rounded-lg hover:bg-purple-100 transition"
          >
            <svg className="w-5 h-5 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z" />
            </svg>
            내 심사 패턴 분석
          </Link>
        </div>
      </div>

      {/* 심사 대기 프로젝트 */}
      <div className="bg-white rounded-lg shadow p-6">
        <h2 className="text-lg font-semibold text-gray-900 mb-4">심사 대기 프로젝트</h2>

        {projects.length === 0 ? (
          <p className="text-gray-500 text-center py-8">
            심사할 프로젝트가 없습니다.
          </p>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {projects.map((project) => (
              <div
                key={project.id}
                className="border border-gray-200 rounded-lg p-4 hover:border-sky-300 transition"
              >
                <div className="flex justify-between items-start mb-2">
                  <h3 className="font-semibold text-gray-900">{project.name}</h3>
                  <span className="px-2 py-1 bg-green-100 text-green-700 text-xs rounded-full">
                    {PROJECT_STATUS_LABELS[project.status]}
                  </span>
                </div>
                <p className="text-sm text-gray-600 mb-2">{project.teamName}</p>
                {project.problemDefinition && (
                  <p className="text-sm text-gray-500 line-clamp-2 mb-4">
                    {project.problemDefinition}
                  </p>
                )}
                <button
                  onClick={() => openReviewModal(project)}
                  className="w-full px-4 py-2 bg-sky-600 text-white rounded-lg hover:bg-sky-700 transition"
                >
                  심사하기
                </button>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* 완료된 심사 */}
      {myReviews.length > 0 && (
        <div className="bg-white rounded-lg shadow p-6">
          <h2 className="text-lg font-semibold text-gray-900 mb-4">완료된 심사</h2>
          <div className="space-y-3">
            {myReviews.map((review) => (
              <div
                key={review.id}
                className="border border-gray-200 rounded-lg p-4 flex justify-between items-center"
              >
                <div>
                  <h3 className="font-medium text-gray-900">{review.projectName}</h3>
                  <p className="text-sm text-gray-500">
                    평균 점수: {review.averageScore.toFixed(1)}점
                  </p>
                </div>
                <span className="text-sm text-gray-400">
                  {new Date(review.submittedAt).toLocaleDateString('ko-KR')}
                </span>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* 심사 모달 */}
      {showReviewModal && selectedProject && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 overflow-y-auto">
          <div className="bg-white rounded-lg shadow-xl p-6 w-full max-w-3xl mx-4 my-8 max-h-[90vh] overflow-y-auto">
            <h3 className="text-lg font-semibold text-gray-900 mb-2">
              프로젝트 심사: {selectedProject.name}
            </h3>
            <p className="text-sm text-gray-600 mb-6">{selectedProject.teamName}</p>

            {/* 프로젝트 정보 */}
            <div className="bg-gray-50 rounded-lg p-4 mb-6">
              <h4 className="font-medium text-gray-900 mb-2">프로젝트 정보</h4>
              <div className="space-y-2 text-sm">
                {selectedProject.problemDefinition && (
                  <div>
                    <span className="font-medium text-gray-700">문제 정의: </span>
                    <span className="text-gray-600">{selectedProject.problemDefinition}</span>
                  </div>
                )}
                {selectedProject.targetUsers && (
                  <div>
                    <span className="font-medium text-gray-700">타깃 사용자: </span>
                    <span className="text-gray-600">{selectedProject.targetUsers}</span>
                  </div>
                )}
                {selectedProject.solution && (
                  <div>
                    <span className="font-medium text-gray-700">핵심 솔루션: </span>
                    <span className="text-gray-600">{selectedProject.solution}</span>
                  </div>
                )}
                {selectedProject.aiUsage && (
                  <div>
                    <span className="font-medium text-gray-700">AI 활용: </span>
                    <span className="text-gray-600">{selectedProject.aiUsage}</span>
                  </div>
                )}
              </div>

              {/* 외부 링크 */}
              <div className="flex flex-wrap gap-2 mt-3">
                {selectedProject.figmaUrl && (
                  <a href={selectedProject.figmaUrl} target="_blank" rel="noopener noreferrer"
                     className="text-xs px-3 py-1 bg-purple-100 text-purple-700 rounded-full hover:bg-purple-200">
                    Figma
                  </a>
                )}
                {selectedProject.githubUrl && (
                  <a href={selectedProject.githubUrl} target="_blank" rel="noopener noreferrer"
                     className="text-xs px-3 py-1 bg-gray-800 text-white rounded-full hover:bg-gray-900">
                    GitHub
                  </a>
                )}
                {selectedProject.demoUrl && (
                  <a href={selectedProject.demoUrl} target="_blank" rel="noopener noreferrer"
                     className="text-xs px-3 py-1 bg-sky-100 text-sky-700 rounded-full hover:bg-sky-200">
                    Demo
                  </a>
                )}
              </div>
            </div>

            {/* 루브릭별 점수 입력 */}
            <div className="space-y-6 mb-6">
              {RUBRIC_ITEMS.map((item) => (
                <div key={item} className="border border-gray-200 rounded-lg p-4">
                  <div className="mb-3">
                    <h4 className="font-medium text-gray-900">{RUBRIC_ITEM_LABELS[item]}</h4>
                    <p className="text-sm text-gray-500">{RUBRIC_ITEM_DESCRIPTIONS[item]}</p>
                  </div>

                  {/* 점수 선택 */}
                  <div className="flex gap-2 mb-3">
                    {[1, 2, 3, 4, 5].map((score) => (
                      <button
                        key={score}
                        onClick={() => handleScoreChange(item, score)}
                        className={`flex-1 py-2 rounded-lg border transition text-sm ${
                          scores[item] === score
                            ? 'bg-sky-600 text-white border-sky-600'
                            : 'bg-white text-gray-700 border-gray-300 hover:border-sky-400'
                        }`}
                      >
                        <div className="font-medium">{score}</div>
                        <div className="text-xs opacity-75">{SCORE_LABELS[score]}</div>
                      </button>
                    ))}
                  </div>

                  {/* 코멘트 */}
                  <textarea
                    value={comments[item]}
                    onChange={(e) => handleCommentChange(item, e.target.value)}
                    placeholder="코멘트 (선택)"
                    className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm resize-none h-16"
                  />
                </div>
              ))}
            </div>

            {/* 총평 */}
            <div className="mb-6">
              <label className="block text-sm font-medium text-gray-700 mb-1">총평 (선택)</label>
              <textarea
                value={overallComment}
                onChange={(e) => setOverallComment(e.target.value)}
                placeholder="프로젝트에 대한 전체적인 피드백을 작성해주세요."
                className="w-full border border-gray-300 rounded-lg px-3 py-2 h-24 resize-none"
              />
            </div>

            {reviewError && (
              <div className="mb-4 p-3 bg-red-50 text-red-700 rounded-lg text-sm">
                {reviewError}
              </div>
            )}

            {/* 버튼 */}
            <div className="flex justify-end gap-3">
              <button
                onClick={() => setShowReviewModal(false)}
                className="px-4 py-2 bg-gray-200 text-gray-700 rounded-lg hover:bg-gray-300 transition"
                disabled={submitting}
              >
                취소
              </button>
              <button
                onClick={handleSubmitReview}
                className="px-4 py-2 bg-sky-600 text-white rounded-lg hover:bg-sky-700 transition disabled:opacity-50"
                disabled={submitting}
              >
                {submitting ? '제출 중...' : '심사 제출'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
