import { useState, useEffect } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import {
  ChevronLeft,
  Heart,
  Eye,
  MessageSquare,
  Edit2,
  Trash2,
  Send,
  Pin,
  MoreVertical,
} from 'lucide-react';
import { communityService, POST_TYPE_LABELS, POST_TYPE_COLORS } from '../services/communityService';
import { useAuthStore } from '../stores/authStore';
import type { Post, Comment as CommentType } from '../types';

export default function PostDetailPage() {
  const { postId } = useParams<{ postId: string }>();
  const navigate = useNavigate();
  const { isAuthenticated, user } = useAuthStore();
  const [post, setPost] = useState<Post | null>(null);
  const [comments, setComments] = useState<CommentType[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState('');
  const [commentContent, setCommentContent] = useState('');
  const [replyTo, setReplyTo] = useState<number | null>(null);
  const [replyContent, setReplyContent] = useState('');
  const [editingCommentId, setEditingCommentId] = useState<number | null>(null);
  const [editingContent, setEditingContent] = useState('');
  const [showMenu, setShowMenu] = useState(false);

  useEffect(() => {
    if (postId) {
      loadPost();
      loadComments();
    }
  }, [postId]);

  const loadPost = async () => {
    setIsLoading(true);
    try {
      const data = await communityService.getPost(Number(postId));
      setPost(data);
    } catch (err) {
      console.error('Failed to load post:', err);
      setError('게시글을 불러오는데 실패했습니다.');
    } finally {
      setIsLoading(false);
    }
  };

  const loadComments = async () => {
    try {
      const data = await communityService.getComments(Number(postId));
      setComments(data);
    } catch (err) {
      console.error('Failed to load comments:', err);
    }
  };

  const handleLike = async () => {
    if (!isAuthenticated) {
      navigate('/login');
      return;
    }
    try {
      const liked = await communityService.toggleLike(Number(postId));
      setPost((prev) =>
        prev
          ? {
              ...prev,
              liked,
              likeCount: liked ? prev.likeCount + 1 : prev.likeCount - 1,
            }
          : null
      );
    } catch (err) {
      console.error('Failed to toggle like:', err);
    }
  };

  const handleDelete = async () => {
    if (!confirm('정말 삭제하시겠습니까?')) return;
    try {
      await communityService.deletePost(Number(postId));
      navigate('/community');
    } catch (err) {
      console.error('Failed to delete post:', err);
      alert('삭제에 실패했습니다.');
    }
  };

  const handleCommentSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!commentContent.trim()) return;
    if (!isAuthenticated) {
      navigate('/login');
      return;
    }

    try {
      await communityService.createComment(Number(postId), { content: commentContent });
      setCommentContent('');
      loadComments();
      setPost((prev) => (prev ? { ...prev, commentCount: prev.commentCount + 1 } : null));
    } catch (err) {
      console.error('Failed to create comment:', err);
      alert('댓글 작성에 실패했습니다.');
    }
  };

  const handleReplySubmit = async (parentId: number) => {
    if (!replyContent.trim()) return;

    try {
      await communityService.createComment(Number(postId), {
        content: replyContent,
        parentId,
      });
      setReplyTo(null);
      setReplyContent('');
      loadComments();
      setPost((prev) => (prev ? { ...prev, commentCount: prev.commentCount + 1 } : null));
    } catch (err) {
      console.error('Failed to create reply:', err);
      alert('답글 작성에 실패했습니다.');
    }
  };

  const handleCommentEdit = async (commentId: number) => {
    if (!editingContent.trim()) return;

    try {
      await communityService.updateComment(commentId, { content: editingContent });
      setEditingCommentId(null);
      setEditingContent('');
      loadComments();
    } catch (err) {
      console.error('Failed to update comment:', err);
      alert('댓글 수정에 실패했습니다.');
    }
  };

  const handleCommentDelete = async (commentId: number) => {
    if (!confirm('댓글을 삭제하시겠습니까?')) return;

    try {
      await communityService.deleteComment(commentId);
      loadComments();
      setPost((prev) => (prev ? { ...prev, commentCount: prev.commentCount - 1 } : null));
    } catch (err) {
      console.error('Failed to delete comment:', err);
      alert('댓글 삭제에 실패했습니다.');
    }
  };

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString('ko-KR', {
      year: 'numeric',
      month: 'long',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  };

  const renderComment = (comment: CommentType, isReply = false) => (
    <div
      key={comment.id}
      className={`${isReply ? 'ml-8 border-l-2 border-neutral-200 pl-4' : ''}`}
    >
      <div className="py-4">
        <div className="flex items-start justify-between">
          <div className="flex-1">
            <div className="flex items-center gap-2 mb-2">
              <span className="font-medium text-neutral-900">
                {comment.deleted ? '(삭제됨)' : comment.author.name}
              </span>
              <span className="text-sm text-neutral-500">{formatDate(comment.createdAt)}</span>
            </div>

            {editingCommentId === comment.id ? (
              <div className="flex gap-2">
                <input
                  type="text"
                  value={editingContent}
                  onChange={(e) => setEditingContent(e.target.value)}
                  className="flex-1 px-3 py-2 border border-neutral-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-transparent"
                />
                <button
                  onClick={() => handleCommentEdit(comment.id)}
                  className="px-3 py-2 bg-primary-500 text-white rounded-lg hover:bg-primary-600 transition-colors"
                >
                  저장
                </button>
                <button
                  onClick={() => setEditingCommentId(null)}
                  className="px-3 py-2 border border-neutral-300 rounded-lg hover:bg-neutral-50 transition-colors"
                >
                  취소
                </button>
              </div>
            ) : (
              <p className={`text-neutral-700 ${comment.deleted ? 'italic text-neutral-400' : ''}`}>
                {comment.content}
              </p>
            )}

            {!comment.deleted && !isReply && (
              <div className="flex items-center gap-4 mt-2">
                <button
                  onClick={() => {
                    setReplyTo(replyTo === comment.id ? null : comment.id);
                    setReplyContent('');
                  }}
                  className="text-sm text-neutral-500 hover:text-primary-500"
                >
                  답글
                </button>
                {user?.id === comment.author.id && (
                  <>
                    <button
                      onClick={() => {
                        setEditingCommentId(comment.id);
                        setEditingContent(comment.content);
                      }}
                      className="text-sm text-neutral-500 hover:text-primary-500"
                    >
                      수정
                    </button>
                    <button
                      onClick={() => handleCommentDelete(comment.id)}
                      className="text-sm text-neutral-500 hover:text-red-500"
                    >
                      삭제
                    </button>
                  </>
                )}
              </div>
            )}
          </div>
        </div>

        {/* 답글 입력 */}
        {replyTo === comment.id && (
          <div className="mt-4 ml-8 flex gap-2">
            <input
              type="text"
              value={replyContent}
              onChange={(e) => setReplyContent(e.target.value)}
              placeholder="답글을 입력하세요"
              className="flex-1 px-3 py-2 border border-neutral-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-transparent"
            />
            <button
              onClick={() => handleReplySubmit(comment.id)}
              className="px-3 py-2 bg-primary-500 text-white rounded-lg hover:bg-primary-600 transition-colors"
            >
              답글
            </button>
          </div>
        )}

        {/* 대댓글 */}
        {comment.replies && comment.replies.length > 0 && (
          <div className="mt-2">
            {comment.replies.map((reply) => renderComment(reply, true))}
          </div>
        )}
      </div>
    </div>
  );

  if (isLoading) {
    return (
      <div className="max-w-3xl mx-auto text-center py-12">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-500 mx-auto" />
        <p className="mt-4 text-neutral-500">로딩 중...</p>
      </div>
    );
  }

  if (error || !post) {
    return (
      <div className="max-w-3xl mx-auto text-center py-12">
        <p className="text-red-500">{error || '게시글을 찾을 수 없습니다.'}</p>
        <Link to="/community" className="mt-4 text-primary-500 hover:underline">
          목록으로 돌아가기
        </Link>
      </div>
    );
  }

  const isAuthor = user?.id === post.author.id;
  const isAdmin = user?.role === 'ADMIN';

  return (
    <div className="max-w-3xl mx-auto">
      {/* 뒤로가기 */}
      <Link
        to="/community"
        className="inline-flex items-center gap-1 text-neutral-500 hover:text-neutral-700 mb-6"
      >
        <ChevronLeft className="w-4 h-4" />
        목록으로
      </Link>

      {/* 게시글 헤더 */}
      <article className="bg-white rounded-lg border border-neutral-200">
        <div className="p-6 border-b border-neutral-200">
          <div className="flex items-center gap-2 mb-3">
            {post.pinned && <Pin className="w-4 h-4 text-red-500" />}
            <span className={`px-2 py-0.5 text-xs font-medium rounded ${POST_TYPE_COLORS[post.postType]}`}>
              {POST_TYPE_LABELS[post.postType]}
            </span>
            {post.season && (
              <span className="text-sm text-neutral-500">{post.season.title}</span>
            )}
          </div>

          <h1 className="text-2xl font-bold text-neutral-900 mb-4">{post.title}</h1>

          <div className="flex items-center justify-between">
            <div className="flex items-center gap-4 text-sm text-neutral-500">
              <span className="font-medium text-neutral-700">{post.author.name}</span>
              <span>{formatDate(post.createdAt)}</span>
              <span className="flex items-center gap-1">
                <Eye className="w-4 h-4" />
                {post.viewCount}
              </span>
            </div>

            {(isAuthor || isAdmin) && (
              <div className="relative">
                <button
                  onClick={() => setShowMenu(!showMenu)}
                  className="p-2 rounded-lg hover:bg-neutral-100 transition-colors"
                >
                  <MoreVertical className="w-5 h-5 text-neutral-500" />
                </button>
                {showMenu && (
                  <div className="absolute right-0 mt-2 w-32 bg-white rounded-lg shadow-lg border border-neutral-200 py-1 z-10">
                    {isAuthor && (
                      <Link
                        to={`/community/write?edit=${post.id}`}
                        className="flex items-center gap-2 px-4 py-2 text-sm text-neutral-700 hover:bg-neutral-50"
                      >
                        <Edit2 className="w-4 h-4" />
                        수정
                      </Link>
                    )}
                    <button
                      onClick={handleDelete}
                      className="flex items-center gap-2 w-full px-4 py-2 text-sm text-red-600 hover:bg-red-50"
                    >
                      <Trash2 className="w-4 h-4" />
                      삭제
                    </button>
                  </div>
                )}
              </div>
            )}
          </div>
        </div>

        {/* 게시글 내용 */}
        <div className="p-6">
          <div className="prose max-w-none whitespace-pre-wrap">{post.content}</div>

          {/* 프로젝트 링크 (쇼케이스인 경우) */}
          {post.project && (
            <div className="mt-6 p-4 bg-neutral-50 rounded-lg">
              <p className="text-sm text-neutral-500 mb-1">연결된 프로젝트</p>
              <Link
                to={`/projects/${post.project.id}`}
                className="text-primary-600 hover:underline font-medium"
              >
                {post.project.name}
              </Link>
            </div>
          )}
        </div>

        {/* 좋아요/댓글 수 */}
        <div className="px-6 py-4 border-t border-neutral-200 flex items-center gap-4">
          <button
            onClick={handleLike}
            className={`flex items-center gap-2 px-4 py-2 rounded-lg transition-colors ${
              post.liked
                ? 'bg-red-50 text-red-600'
                : 'bg-neutral-100 text-neutral-600 hover:bg-neutral-200'
            }`}
          >
            <Heart className={`w-5 h-5 ${post.liked ? 'fill-current' : ''}`} />
            좋아요 {post.likeCount}
          </button>
          <div className="flex items-center gap-2 text-neutral-500">
            <MessageSquare className="w-5 h-5" />
            댓글 {post.commentCount}
          </div>
        </div>
      </article>

      {/* 댓글 섹션 */}
      <section className="mt-6 bg-white rounded-lg border border-neutral-200">
        <div className="p-6 border-b border-neutral-200">
          <h2 className="font-bold text-lg">댓글 {post.commentCount}개</h2>
        </div>

        {/* 댓글 입력 */}
        {isAuthenticated ? (
          <form onSubmit={handleCommentSubmit} className="p-6 border-b border-neutral-200">
            <div className="flex gap-2">
              <input
                type="text"
                value={commentContent}
                onChange={(e) => setCommentContent(e.target.value)}
                placeholder="댓글을 입력하세요"
                className="flex-1 px-4 py-2 border border-neutral-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-transparent"
              />
              <button
                type="submit"
                className="flex items-center gap-2 px-4 py-2 bg-primary-500 text-white rounded-lg hover:bg-primary-600 transition-colors"
              >
                <Send className="w-4 h-4" />
                작성
              </button>
            </div>
          </form>
        ) : (
          <div className="p-6 border-b border-neutral-200 text-center">
            <p className="text-neutral-500">
              댓글을 작성하려면{' '}
              <Link to="/login" className="text-primary-500 hover:underline">
                로그인
              </Link>
              하세요.
            </p>
          </div>
        )}

        {/* 댓글 목록 */}
        <div className="divide-y divide-neutral-200 px-6">
          {comments.length === 0 ? (
            <p className="py-8 text-center text-neutral-500">첫 댓글을 작성해보세요!</p>
          ) : (
            comments.map((comment) => renderComment(comment))
          )}
        </div>
      </section>
    </div>
  );
}
