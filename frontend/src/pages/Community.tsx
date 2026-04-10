import { useState, useEffect } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import {
  MessageSquare,
  Users,
  Megaphone,
  Trophy,
  HelpCircle,
  ChevronRight,
  Eye,
  Heart,
  Pin,
  Search,
  Plus,
} from 'lucide-react';
import { communityService, POST_TYPE_LABELS, POST_TYPE_COLORS } from '../services/communityService';
import { seasonService } from '../services/seasonService';
import { useAuthStore } from '../stores/authStore';
import type { PostListItem, PostType, Season, Page } from '../types';

const TABS: { type: PostType | 'ALL'; label: string; icon: React.ReactNode }[] = [
  { type: 'ALL', label: '전체', icon: <MessageSquare className="w-4 h-4" /> },
  { type: 'ANNOUNCEMENT', label: '공지사항', icon: <Megaphone className="w-4 h-4" /> },
  { type: 'RECRUITMENT', label: '팀원 모집', icon: <Users className="w-4 h-4" /> },
  { type: 'SHOWCASE', label: '쇼케이스', icon: <Trophy className="w-4 h-4" /> },
  { type: 'QNA', label: 'Q&A', icon: <HelpCircle className="w-4 h-4" /> },
];

export default function CommunityPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const { isAuthenticated } = useAuthStore();
  const [activeTab, setActiveTab] = useState<PostType | 'ALL'>(
    (searchParams.get('type') as PostType) || 'ALL'
  );
  const [posts, setPosts] = useState<PostListItem[]>([]);
  const [seasons, setSeasons] = useState<Season[]>([]);
  const [selectedSeasonId, setSelectedSeasonId] = useState<number | undefined>();
  const [keyword, setKeyword] = useState('');
  const [searchInput, setSearchInput] = useState('');
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    loadSeasons();
  }, []);

  useEffect(() => {
    loadPosts();
  }, [activeTab, selectedSeasonId, keyword, page]);

  const loadSeasons = async () => {
    try {
      const activeSeasons = await seasonService.getActiveSeasons();
      setSeasons(activeSeasons);
    } catch (err) {
      console.error('Failed to load seasons:', err);
    }
  };

  const loadPosts = async () => {
    setIsLoading(true);
    try {
      let result: Page<PostListItem>;

      if (activeTab === 'ALL') {
        result = await communityService.getPosts({
          seasonId: selectedSeasonId,
          keyword: keyword || undefined,
          page,
        });
      } else if (activeTab === 'RECRUITMENT') {
        result = await communityService.getRecruitmentPosts(selectedSeasonId, page);
      } else if (activeTab === 'ANNOUNCEMENT') {
        result = await communityService.getAnnouncements(selectedSeasonId, page);
      } else if (activeTab === 'SHOWCASE') {
        result = await communityService.getShowcases(page);
      } else {
        result = await communityService.getQnaPosts(selectedSeasonId, page);
      }

      setPosts(result.content);
      setTotalPages(result.totalPages);
    } catch (err) {
      console.error('Failed to load posts:', err);
    } finally {
      setIsLoading(false);
    }
  };

  const handleTabChange = (type: PostType | 'ALL') => {
    setActiveTab(type);
    setPage(0);
    if (type === 'ALL') {
      searchParams.delete('type');
    } else {
      searchParams.set('type', type);
    }
    setSearchParams(searchParams);
  };

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    setKeyword(searchInput);
    setPage(0);
  };

  const formatDate = (dateString: string) => {
    const date = new Date(dateString);
    const now = new Date();
    const diff = now.getTime() - date.getTime();
    const hours = Math.floor(diff / (1000 * 60 * 60));
    const days = Math.floor(hours / 24);

    if (hours < 1) return '방금 전';
    if (hours < 24) return `${hours}시간 전`;
    if (days < 7) return `${days}일 전`;
    return date.toLocaleDateString('ko-KR');
  };

  return (
    <div className="max-w-5xl mx-auto">
      {/* 헤더 */}
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="text-2xl font-bold text-neutral-900">커뮤니티</h1>
          <p className="text-neutral-600 mt-1">
            팀원을 모집하고, 프로젝트를 공유하고, 함께 성장하세요
          </p>
        </div>
        {isAuthenticated && (
          <Link
            to="/community/write"
            className="flex items-center gap-2 px-4 py-2 bg-primary-500 text-white rounded-lg hover:bg-primary-600 transition-colors"
          >
            <Plus className="w-4 h-4" />
            글쓰기
          </Link>
        )}
      </div>

      {/* 탭 네비게이션 */}
      <div className="flex gap-2 border-b border-neutral-200 mb-6 overflow-x-auto">
        {TABS.map((tab) => (
          <button
            key={tab.type}
            onClick={() => handleTabChange(tab.type)}
            className={`flex items-center gap-2 px-4 py-3 border-b-2 transition-colors whitespace-nowrap ${
              activeTab === tab.type
                ? 'border-primary-500 text-primary-600 font-medium'
                : 'border-transparent text-neutral-500 hover:text-neutral-700'
            }`}
          >
            {tab.icon}
            {tab.label}
          </button>
        ))}
      </div>

      {/* 필터 및 검색 */}
      <div className="flex flex-col sm:flex-row gap-4 mb-6">
        {/* 시즌 필터 */}
        {activeTab !== 'SHOWCASE' && (
          <select
            value={selectedSeasonId || ''}
            onChange={(e) => {
              setSelectedSeasonId(e.target.value ? Number(e.target.value) : undefined);
              setPage(0);
            }}
            className="px-4 py-2 border border-neutral-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-transparent"
          >
            <option value="">전체 시즌</option>
            {seasons.map((season) => (
              <option key={season.id} value={season.id}>
                {season.title}
              </option>
            ))}
          </select>
        )}

        {/* 검색 */}
        {activeTab === 'ALL' && (
          <form onSubmit={handleSearch} className="flex-1 flex gap-2">
            <div className="relative flex-1">
              <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 w-4 h-4 text-neutral-400" />
              <input
                type="text"
                value={searchInput}
                onChange={(e) => setSearchInput(e.target.value)}
                placeholder="검색어를 입력하세요"
                className="w-full pl-10 pr-4 py-2 border border-neutral-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-transparent"
              />
            </div>
            <button
              type="submit"
              className="px-4 py-2 bg-neutral-100 text-neutral-700 rounded-lg hover:bg-neutral-200 transition-colors"
            >
              검색
            </button>
          </form>
        )}
      </div>

      {/* 게시글 목록 */}
      {isLoading ? (
        <div className="text-center py-12">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-500 mx-auto" />
          <p className="mt-4 text-neutral-500">로딩 중...</p>
        </div>
      ) : posts.length === 0 ? (
        <div className="text-center py-12 bg-neutral-50 rounded-lg">
          <MessageSquare className="w-12 h-12 text-neutral-300 mx-auto" />
          <p className="mt-4 text-neutral-500">게시글이 없습니다.</p>
          {isAuthenticated && (
            <Link
              to="/community/write"
              className="inline-block mt-4 px-4 py-2 bg-primary-500 text-white rounded-lg hover:bg-primary-600 transition-colors"
            >
              첫 글 작성하기
            </Link>
          )}
        </div>
      ) : (
        <div className="bg-white rounded-lg border border-neutral-200 divide-y divide-neutral-200">
          {posts.map((post) => (
            <Link
              key={post.id}
              to={`/community/posts/${post.id}`}
              className="flex items-start gap-4 p-4 hover:bg-neutral-50 transition-colors"
            >
              <div className="flex-1 min-w-0">
                <div className="flex items-center gap-2 mb-1">
                  {post.pinned && (
                    <Pin className="w-4 h-4 text-red-500 flex-shrink-0" />
                  )}
                  <span
                    className={`px-2 py-0.5 text-xs font-medium rounded ${
                      POST_TYPE_COLORS[post.postType]
                    }`}
                  >
                    {POST_TYPE_LABELS[post.postType]}
                  </span>
                  <h3 className="font-medium text-neutral-900 truncate">
                    {post.title}
                  </h3>
                </div>
                <div className="flex items-center gap-4 text-sm text-neutral-500">
                  <span>{post.author.name}</span>
                  <span>{formatDate(post.createdAt)}</span>
                  <span className="flex items-center gap-1">
                    <Eye className="w-3.5 h-3.5" />
                    {post.viewCount}
                  </span>
                  <span className="flex items-center gap-1">
                    <Heart className="w-3.5 h-3.5" />
                    {post.likeCount}
                  </span>
                  <span className="flex items-center gap-1">
                    <MessageSquare className="w-3.5 h-3.5" />
                    {post.commentCount}
                  </span>
                </div>
              </div>
              <ChevronRight className="w-5 h-5 text-neutral-400 flex-shrink-0" />
            </Link>
          ))}
        </div>
      )}

      {/* 페이지네이션 */}
      {totalPages > 1 && (
        <div className="flex justify-center gap-2 mt-6">
          <button
            onClick={() => setPage((p) => Math.max(0, p - 1))}
            disabled={page === 0}
            className="px-4 py-2 border border-neutral-300 rounded-lg disabled:opacity-50 disabled:cursor-not-allowed hover:bg-neutral-50 transition-colors"
          >
            이전
          </button>
          <span className="px-4 py-2 text-neutral-600">
            {page + 1} / {totalPages}
          </span>
          <button
            onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
            disabled={page === totalPages - 1}
            className="px-4 py-2 border border-neutral-300 rounded-lg disabled:opacity-50 disabled:cursor-not-allowed hover:bg-neutral-50 transition-colors"
          >
            다음
          </button>
        </div>
      )}
    </div>
  );
}
