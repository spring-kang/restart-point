import { useState, useEffect } from 'react';
import { useNavigate, useSearchParams, Link } from 'react-router-dom';
import { ChevronLeft, Save, AlertCircle } from 'lucide-react';
import { communityService, POST_TYPE_LABELS } from '../services/communityService';
import { seasonService } from '../services/seasonService';
import { useAuthStore } from '../stores/authStore';
import type { PostType, Season, Project } from '../types';

export default function PostWritePage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const editId = searchParams.get('edit');
  const { isAuthenticated, user } = useAuthStore();

  const [postType, setPostType] = useState<PostType>('QNA');
  const [title, setTitle] = useState('');
  const [content, setContent] = useState('');
  const [seasonId, setSeasonId] = useState<number | undefined>();
  const [projectId, setProjectId] = useState<number | undefined>();
  const [seasons, setSeasons] = useState<Season[]>([]);
  const [projects, setProjects] = useState<Project[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState('');

  const isEditing = !!editId;

  useEffect(() => {
    if (!isAuthenticated) {
      navigate('/login');
      return;
    }
    loadSeasons();
    if (editId) {
      loadPost(Number(editId));
    }
  }, [isAuthenticated, editId]);

  useEffect(() => {
    if (postType === 'SHOWCASE') {
      loadMyProjects();
    }
  }, [postType]);

  const loadSeasons = async () => {
    try {
      const activeSeasons = await seasonService.getActiveSeasons();
      setSeasons(activeSeasons);
    } catch (err) {
      console.error('Failed to load seasons:', err);
    }
  };

  const loadMyProjects = async () => {
    try {
      // 프로젝트 워크스페이스에서 내 팀의 프로젝트 목록 가져오기
      // 간단하게 처리: 사용자의 팀 프로젝트를 가져올 수 있는 API가 있다면 사용
      // 없다면 빈 배열로 처리 (쇼케이스는 프로젝트 연결이 필수)
      setProjects([]);
    } catch (err) {
      console.error('Failed to load projects:', err);
    }
  };

  const loadPost = async (id: number) => {
    try {
      const post = await communityService.getPost(id);
      setPostType(post.postType);
      setTitle(post.title);
      setContent(post.content);
      setSeasonId(post.season?.id);
      setProjectId(post.project?.id);
    } catch (err) {
      console.error('Failed to load post:', err);
      setError('게시글을 불러오는데 실패했습니다.');
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!title.trim()) {
      setError('제목을 입력해주세요.');
      return;
    }
    if (!content.trim()) {
      setError('내용을 입력해주세요.');
      return;
    }
    if (postType === 'SHOWCASE' && !projectId) {
      setError('쇼케이스는 프로젝트를 선택해야 합니다.');
      return;
    }
    if (postType === 'ANNOUNCEMENT' && user?.role !== 'ADMIN') {
      setError('공지사항은 관리자만 작성할 수 있습니다.');
      return;
    }

    setIsLoading(true);
    setError('');

    try {
      if (isEditing) {
        await communityService.updatePost(Number(editId), { title, content });
        navigate(`/community/posts/${editId}`);
      } else {
        const post = await communityService.createPost({
          postType,
          title,
          content,
          seasonId,
          projectId,
        });
        navigate(`/community/posts/${post.id}`);
      }
    } catch (err: unknown) {
      console.error('Failed to save post:', err);
      const axiosError = err as { response?: { data?: { message?: string } } };
      setError(axiosError.response?.data?.message || '저장에 실패했습니다.');
    } finally {
      setIsLoading(false);
    }
  };

  if (!isAuthenticated) {
    return null;
  }

  return (
    <div className="max-w-3xl mx-auto">
      {/* 헤더 */}
      <div className="mb-6">
        <Link
          to="/community"
          className="inline-flex items-center gap-1 text-neutral-500 hover:text-neutral-700 mb-4"
        >
          <ChevronLeft className="w-4 h-4" />
          목록으로
        </Link>
        <h1 className="text-2xl font-bold text-neutral-900">
          {isEditing ? '게시글 수정' : '글쓰기'}
        </h1>
      </div>

      {/* 에러 메시지 */}
      {error && (
        <div className="mb-6 p-4 bg-red-50 border border-red-200 rounded-lg flex items-center gap-2 text-red-700">
          <AlertCircle className="w-5 h-5 flex-shrink-0" />
          {error}
        </div>
      )}

      {/* 폼 */}
      <form onSubmit={handleSubmit} className="bg-white rounded-lg border border-neutral-200 p-6">
        {/* 게시판 유형 (수정 시에는 변경 불가) */}
        {!isEditing && (
          <div className="mb-6">
            <label className="block text-sm font-medium text-neutral-700 mb-2">
              게시판 선택
            </label>
            <div className="flex flex-wrap gap-2">
              {(Object.keys(POST_TYPE_LABELS) as PostType[])
                .filter((type) => type !== 'ANNOUNCEMENT' || user?.role === 'ADMIN')
                .map((type) => (
                  <button
                    key={type}
                    type="button"
                    onClick={() => setPostType(type)}
                    className={`px-4 py-2 rounded-lg border transition-colors ${
                      postType === type
                        ? 'bg-primary-500 text-white border-primary-500'
                        : 'bg-white text-neutral-700 border-neutral-300 hover:border-primary-300'
                    }`}
                  >
                    {POST_TYPE_LABELS[type]}
                  </button>
                ))}
            </div>
          </div>
        )}

        {/* 시즌 선택 (쇼케이스 제외) */}
        {postType !== 'SHOWCASE' && !isEditing && (
          <div className="mb-6">
            <label className="block text-sm font-medium text-neutral-700 mb-2">
              시즌 선택 (선택사항)
            </label>
            <select
              value={seasonId || ''}
              onChange={(e) => setSeasonId(e.target.value ? Number(e.target.value) : undefined)}
              className="w-full px-4 py-2 border border-neutral-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-transparent"
            >
              <option value="">시즌 선택 안 함</option>
              {seasons.map((season) => (
                <option key={season.id} value={season.id}>
                  {season.title}
                </option>
              ))}
            </select>
          </div>
        )}

        {/* 프로젝트 선택 (쇼케이스인 경우) */}
        {postType === 'SHOWCASE' && !isEditing && (
          <div className="mb-6">
            <label className="block text-sm font-medium text-neutral-700 mb-2">
              프로젝트 선택 <span className="text-red-500">*</span>
            </label>
            {projects.length === 0 ? (
              <div className="p-4 bg-yellow-50 border border-yellow-200 rounded-lg text-yellow-700">
                <p className="text-sm">
                  쇼케이스를 작성하려면 제출 완료된 프로젝트가 필요합니다.
                </p>
              </div>
            ) : (
              <select
                value={projectId || ''}
                onChange={(e) => setProjectId(e.target.value ? Number(e.target.value) : undefined)}
                className="w-full px-4 py-2 border border-neutral-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-transparent"
                required
              >
                <option value="">프로젝트를 선택하세요</option>
                {projects.map((project) => (
                  <option key={project.id} value={project.id}>
                    {project.name}
                  </option>
                ))}
              </select>
            )}
          </div>
        )}

        {/* 제목 */}
        <div className="mb-6">
          <label className="block text-sm font-medium text-neutral-700 mb-2">
            제목 <span className="text-red-500">*</span>
          </label>
          <input
            type="text"
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            placeholder="제목을 입력하세요"
            maxLength={200}
            className="w-full px-4 py-2 border border-neutral-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-transparent"
            required
          />
          <p className="mt-1 text-sm text-neutral-500">{title.length}/200</p>
        </div>

        {/* 내용 */}
        <div className="mb-6">
          <label className="block text-sm font-medium text-neutral-700 mb-2">
            내용 <span className="text-red-500">*</span>
          </label>
          <textarea
            value={content}
            onChange={(e) => setContent(e.target.value)}
            placeholder={
              postType === 'RECRUITMENT'
                ? '팀 소개, 모집하는 역할, 프로젝트 방향 등을 작성해주세요.'
                : postType === 'QNA'
                ? '궁금한 점을 자세히 작성해주세요.'
                : '내용을 입력하세요.'
            }
            rows={15}
            className="w-full px-4 py-2 border border-neutral-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-transparent resize-none"
            required
          />
        </div>

        {/* 버튼 */}
        <div className="flex justify-end gap-3">
          <Link
            to="/community"
            className="px-6 py-2 border border-neutral-300 rounded-lg text-neutral-700 hover:bg-neutral-50 transition-colors"
          >
            취소
          </Link>
          <button
            type="submit"
            disabled={isLoading}
            className="flex items-center gap-2 px-6 py-2 bg-primary-500 text-white rounded-lg hover:bg-primary-600 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
          >
            {isLoading ? (
              <>
                <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-white" />
                저장 중...
              </>
            ) : (
              <>
                <Save className="w-4 h-4" />
                {isEditing ? '수정하기' : '등록하기'}
              </>
            )}
          </button>
        </div>
      </form>
    </div>
  );
}
