import api from './api';
import type {
  Post,
  PostListItem,
  PostCreateRequest,
  PostUpdateRequest,
  PostType,
  Comment,
  CommentCreateRequest,
  CommentUpdateRequest,
  Page,
} from '../types';

export const communityService = {
  // ========== 게시글 API ==========

  // 게시글 작성
  async createPost(request: PostCreateRequest): Promise<Post> {
    const response = await api.post('/community/posts', request);
    return response.data.data;
  },

  // 게시글 상세 조회
  async getPost(postId: number): Promise<Post> {
    const response = await api.get(`/community/posts/${postId}`);
    return response.data.data;
  },

  // 게시글 수정
  async updatePost(postId: number, request: PostUpdateRequest): Promise<Post> {
    const response = await api.put(`/community/posts/${postId}`, request);
    return response.data.data;
  },

  // 게시글 삭제
  async deletePost(postId: number): Promise<void> {
    await api.delete(`/community/posts/${postId}`);
  },

  // 게시글 목록 조회
  async getPosts(params: {
    type?: PostType;
    seasonId?: number;
    keyword?: string;
    page?: number;
    size?: number;
  } = {}): Promise<Page<PostListItem>> {
    const response = await api.get('/community/posts', {
      params: {
        type: params.type,
        seasonId: params.seasonId,
        keyword: params.keyword,
        page: params.page || 0,
        size: params.size || 20,
      },
    });
    return response.data.data;
  },

  // 팀원 모집 게시판
  async getRecruitmentPosts(seasonId?: number, page = 0, size = 20): Promise<Page<PostListItem>> {
    const response = await api.get('/community/recruitment', {
      params: { seasonId, page, size },
    });
    return response.data.data;
  },

  // 공지사항 게시판
  async getAnnouncements(seasonId?: number, page = 0, size = 20): Promise<Page<PostListItem>> {
    const response = await api.get('/community/announcements', {
      params: { seasonId, page, size },
    });
    return response.data.data;
  },

  // 프로젝트 쇼케이스
  async getShowcases(page = 0, size = 20): Promise<Page<PostListItem>> {
    const response = await api.get('/community/showcase', {
      params: { page, size },
    });
    return response.data.data;
  },

  // Q&A 게시판
  async getQnaPosts(seasonId?: number, page = 0, size = 20): Promise<Page<PostListItem>> {
    const response = await api.get('/community/qna', {
      params: { seasonId, page, size },
    });
    return response.data.data;
  },

  // 내가 작성한 게시글
  async getMyPosts(page = 0, size = 20): Promise<Page<PostListItem>> {
    const response = await api.get('/community/posts/my', {
      params: { page, size },
    });
    return response.data.data;
  },

  // ========== 좋아요 API ==========

  // 좋아요 토글
  async toggleLike(postId: number): Promise<boolean> {
    const response = await api.post(`/community/posts/${postId}/like`);
    return response.data.data;
  },

  // ========== 댓글 API ==========

  // 댓글 작성
  async createComment(postId: number, request: CommentCreateRequest): Promise<Comment> {
    const response = await api.post(`/community/posts/${postId}/comments`, request);
    return response.data.data;
  },

  // 댓글 목록 조회
  async getComments(postId: number): Promise<Comment[]> {
    const response = await api.get(`/community/posts/${postId}/comments`);
    return response.data.data;
  },

  // 댓글 수정
  async updateComment(commentId: number, request: CommentUpdateRequest): Promise<Comment> {
    const response = await api.put(`/community/comments/${commentId}`, request);
    return response.data.data;
  },

  // 댓글 삭제
  async deleteComment(commentId: number): Promise<void> {
    await api.delete(`/community/comments/${commentId}`);
  },

  // ========== 관리자 API ==========

  // 게시글 고정
  async pinPost(postId: number): Promise<void> {
    await api.post(`/community/posts/${postId}/pin`);
  },

  // 게시글 고정 해제
  async unpinPost(postId: number): Promise<void> {
    await api.delete(`/community/posts/${postId}/pin`);
  },
};

// 게시글 유형 라벨
export const POST_TYPE_LABELS: Record<PostType, string> = {
  RECRUITMENT: '팀원 모집',
  ANNOUNCEMENT: '공지사항',
  SHOWCASE: '프로젝트 쇼케이스',
  QNA: 'Q&A',
};

// 게시글 유형 색상
export const POST_TYPE_COLORS: Record<PostType, string> = {
  RECRUITMENT: 'bg-green-100 text-green-700',
  ANNOUNCEMENT: 'bg-red-100 text-red-700',
  SHOWCASE: 'bg-purple-100 text-purple-700',
  QNA: 'bg-blue-100 text-blue-700',
};

// 게시글 유형 아이콘 (Heroicons 스타일)
export const POST_TYPE_ICONS: Record<PostType, string> = {
  RECRUITMENT: '👥',
  ANNOUNCEMENT: '📢',
  SHOWCASE: '🏆',
  QNA: '❓',
};
