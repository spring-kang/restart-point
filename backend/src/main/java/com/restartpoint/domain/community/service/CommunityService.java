package com.restartpoint.domain.community.service;

import com.restartpoint.domain.community.dto.*;
import com.restartpoint.domain.community.entity.*;
import com.restartpoint.domain.community.repository.*;
import com.restartpoint.domain.project.entity.Project;
import com.restartpoint.domain.project.repository.ProjectRepository;
import com.restartpoint.domain.season.entity.Season;
import com.restartpoint.domain.season.repository.SeasonRepository;
import com.restartpoint.domain.user.entity.Role;
import com.restartpoint.domain.user.entity.User;
import com.restartpoint.domain.user.repository.UserRepository;
import com.restartpoint.global.exception.BusinessException;
import com.restartpoint.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 커뮤니티 서비스
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class CommunityService {

    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final PostLikeRepository postLikeRepository;
    private final UserRepository userRepository;
    private final SeasonRepository seasonRepository;
    private final ProjectRepository projectRepository;

    // ========== 게시글 관련 ==========

    /**
     * 게시글 작성
     */
    @Transactional
    public PostResponse createPost(Long userId, PostRequest.Create request) {
        User author = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 공지는 관리자만 작성 가능
        if (request.getPostType() == PostType.ANNOUNCEMENT && author.getRole() != Role.ADMIN) {
            throw new BusinessException(ErrorCode.ANNOUNCEMENT_ADMIN_ONLY);
        }

        // 쇼케이스는 프로젝트 연결 필수
        if (request.getPostType() == PostType.SHOWCASE && request.getProjectId() == null) {
            throw new BusinessException(ErrorCode.SHOWCASE_REQUIRES_PROJECT);
        }

        Season season = null;
        if (request.getSeasonId() != null) {
            season = seasonRepository.findById(request.getSeasonId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.SEASON_NOT_FOUND));
        }

        Project project = null;
        if (request.getProjectId() != null) {
            project = projectRepository.findById(request.getProjectId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.PROJECT_NOT_FOUND));
        }

        Post post = Post.builder()
                .postType(request.getPostType())
                .title(request.getTitle())
                .content(request.getContent())
                .author(author)
                .season(season)
                .project(project)
                .pinned(false)
                .build();

        Post saved = postRepository.save(post);
        log.info("게시글 생성: postId={}, type={}, author={}", saved.getId(), saved.getPostType(), author.getName());

        return PostResponse.from(saved);
    }

    /**
     * 게시글 상세 조회
     */
    @Transactional
    public PostResponse getPost(Long postId, Long userId) {
        Post post = postRepository.findByIdWithAuthor(postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));

        // 조회수 증가
        post.incrementViewCount();

        // 현재 사용자의 좋아요 여부 확인
        boolean liked = userId != null && postLikeRepository.existsByPostIdAndUserId(postId, userId);

        return PostResponse.from(post, liked);
    }

    /**
     * 게시글 수정
     */
    @Transactional
    public PostResponse updatePost(Long postId, Long userId, PostRequest.Update request) {
        Post post = postRepository.findByIdWithAuthor(postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));

        if (!post.isAuthor(userId)) {
            throw new BusinessException(ErrorCode.NOT_POST_AUTHOR);
        }

        post.update(request.getTitle(), request.getContent());
        log.info("게시글 수정: postId={}", postId);

        return PostResponse.from(post);
    }

    /**
     * 게시글 삭제
     */
    @Transactional
    public void deletePost(Long postId, Long userId) {
        Post post = postRepository.findByIdWithAuthor(postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 작성자 또는 관리자만 삭제 가능
        if (!post.isAuthor(userId) && user.getRole() != Role.ADMIN) {
            throw new BusinessException(ErrorCode.NOT_POST_AUTHOR);
        }

        post.delete();
        log.info("게시글 삭제: postId={}", postId);
    }

    /**
     * 게시판별 게시글 목록 조회
     */
    public Page<PostResponse.ListItem> getPostsByType(PostType postType, Pageable pageable) {
        return postRepository.findByPostType(postType, pageable)
                .map(PostResponse.ListItem::from);
    }

    /**
     * 시즌별 게시글 목록 조회
     */
    public Page<PostResponse.ListItem> getPostsBySeason(Long seasonId, Pageable pageable) {
        return postRepository.findBySeasonId(seasonId, pageable)
                .map(PostResponse.ListItem::from);
    }

    /**
     * 시즌별 특정 유형 게시글 조회
     */
    public Page<PostResponse.ListItem> getPostsByTypeAndSeason(
            PostType postType, Long seasonId, Pageable pageable) {
        return postRepository.findByPostTypeAndSeasonId(postType, seasonId, pageable)
                .map(PostResponse.ListItem::from);
    }

    /**
     * 프로젝트 쇼케이스 목록 조회
     */
    public Page<PostResponse.ListItem> getShowcases(Pageable pageable) {
        return postRepository.findShowcases(pageable)
                .map(PostResponse.ListItem::from);
    }

    /**
     * 게시글 검색
     */
    public Page<PostResponse.ListItem> searchPosts(String keyword, PostType postType, Pageable pageable) {
        if (postType != null) {
            return postRepository.searchByKeywordAndPostType(keyword, postType, pageable)
                    .map(PostResponse.ListItem::from);
        }
        return postRepository.searchByKeyword(keyword, pageable)
                .map(PostResponse.ListItem::from);
    }

    /**
     * 인기 게시글 조회
     */
    public Page<PostResponse.ListItem> getPopularPosts(Pageable pageable) {
        return postRepository.findPopularPosts(pageable)
                .map(PostResponse.ListItem::from);
    }

    /**
     * 내가 작성한 게시글 목록
     */
    public Page<PostResponse.ListItem> getMyPosts(Long userId, Pageable pageable) {
        return postRepository.findByAuthorId(userId, pageable)
                .map(PostResponse.ListItem::from);
    }

    // ========== 좋아요 관련 ==========

    /**
     * 좋아요 토글
     */
    @Transactional
    public boolean toggleLike(Long postId, Long userId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        return postLikeRepository.findByPostIdAndUserId(postId, userId)
                .map(like -> {
                    // 좋아요 취소
                    postLikeRepository.delete(like);
                    post.decrementLikeCount();
                    return false;
                })
                .orElseGet(() -> {
                    // 좋아요 추가
                    PostLike postLike = PostLike.builder()
                            .post(post)
                            .user(user)
                            .build();
                    postLikeRepository.save(postLike);
                    post.incrementLikeCount();
                    return true;
                });
    }

    // ========== 댓글 관련 ==========

    /**
     * 댓글 작성
     */
    @Transactional
    public CommentResponse createComment(Long postId, Long userId, CommentRequest.Create request) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));

        User author = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Comment parent = null;
        if (request.getParentId() != null) {
            parent = commentRepository.findById(request.getParentId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.COMMENT_NOT_FOUND));

            // 부모 댓글이 현재 게시글에 속하는지 검증
            if (!parent.getPost().getId().equals(postId)) {
                throw new BusinessException(ErrorCode.COMMENT_NOT_BELONG_TO_POST);
            }
        }

        Comment comment = Comment.builder()
                .post(post)
                .author(author)
                .content(request.getContent())
                .parent(parent)
                .build();

        Comment saved = commentRepository.save(comment);
        post.incrementCommentCount();

        log.info("댓글 생성: commentId={}, postId={}, author={}", saved.getId(), postId, author.getName());

        return CommentResponse.simple(saved);
    }

    /**
     * 댓글 수정
     */
    @Transactional
    public CommentResponse updateComment(Long commentId, Long userId, CommentRequest.Update request) {
        Comment comment = commentRepository.findByIdWithAuthor(commentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMENT_NOT_FOUND));

        if (!comment.isAuthor(userId)) {
            throw new BusinessException(ErrorCode.NOT_COMMENT_AUTHOR);
        }

        comment.update(request.getContent());
        log.info("댓글 수정: commentId={}", commentId);

        return CommentResponse.simple(comment);
    }

    /**
     * 댓글 삭제
     */
    @Transactional
    public void deleteComment(Long commentId, Long userId) {
        Comment comment = commentRepository.findByIdWithAuthor(commentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMENT_NOT_FOUND));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 작성자 또는 관리자만 삭제 가능
        if (!comment.isAuthor(userId) && user.getRole() != Role.ADMIN) {
            throw new BusinessException(ErrorCode.NOT_COMMENT_AUTHOR);
        }

        comment.delete();
        comment.getPost().decrementCommentCount();

        log.info("댓글 삭제: commentId={}", commentId);
    }

    /**
     * 게시글의 댓글 목록 조회
     */
    public List<CommentResponse> getCommentsByPost(Long postId) {
        List<Comment> comments = commentRepository.findTopLevelCommentsByPostId(postId);
        return comments.stream()
                .map(CommentResponse::from)
                .collect(Collectors.toList());
    }

    // ========== 관리자 기능 ==========

    /**
     * 게시글 고정 (관리자)
     */
    @Transactional
    public void pinPost(Long postId, Long adminId) {
        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (admin.getRole() != Role.ADMIN) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));

        post.pin();
        log.info("게시글 고정: postId={}", postId);
    }

    /**
     * 게시글 고정 해제 (관리자)
     */
    @Transactional
    public void unpinPost(Long postId, Long adminId) {
        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (admin.getRole() != Role.ADMIN) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));

        post.unpin();
        log.info("게시글 고정 해제: postId={}", postId);
    }
}
