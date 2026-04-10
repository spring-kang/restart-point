package com.restartpoint.domain.community.controller;

import com.restartpoint.domain.community.dto.*;
import com.restartpoint.domain.community.entity.PostType;
import com.restartpoint.domain.community.service.CommunityService;
import com.restartpoint.global.common.ApiResponse;
import com.restartpoint.global.security.CurrentUser;
import com.restartpoint.global.security.CustomUserPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/community")
@RequiredArgsConstructor
public class CommunityController {

    private final CommunityService communityService;

    // ========== 게시글 API ==========

    /**
     * 게시글 작성
     */
    @PostMapping("/posts")
    public ResponseEntity<ApiResponse<PostResponse>> createPost(
            @CurrentUser CustomUserPrincipal principal,
            @Valid @RequestBody PostRequest.Create request) {
        PostResponse post = communityService.createPost(principal.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.success(post, "게시글이 작성되었습니다."));
    }

    /**
     * 게시글 상세 조회 (비로그인 사용자도 접근 가능)
     */
    @GetMapping("/posts/{postId}")
    public ResponseEntity<ApiResponse<PostResponse>> getPost(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable Long postId) {
        Long userId = principal != null ? principal.getUserId() : null;
        PostResponse post = communityService.getPost(postId, userId);
        return ResponseEntity.ok(ApiResponse.success(post));
    }

    /**
     * 게시글 수정
     */
    @PutMapping("/posts/{postId}")
    public ResponseEntity<ApiResponse<PostResponse>> updatePost(
            @CurrentUser CustomUserPrincipal principal,
            @PathVariable Long postId,
            @Valid @RequestBody PostRequest.Update request) {
        PostResponse post = communityService.updatePost(postId, principal.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.success(post, "게시글이 수정되었습니다."));
    }

    /**
     * 게시글 삭제
     */
    @DeleteMapping("/posts/{postId}")
    public ResponseEntity<ApiResponse<Void>> deletePost(
            @CurrentUser CustomUserPrincipal principal,
            @PathVariable Long postId) {
        communityService.deletePost(postId, principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success(null, "게시글이 삭제되었습니다."));
    }

    /**
     * 게시판별 게시글 목록 조회
     */
    @GetMapping("/posts")
    public ResponseEntity<ApiResponse<Page<PostResponse.ListItem>>> getPosts(
            @RequestParam(required = false) PostType type,
            @RequestParam(required = false) Long seasonId,
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<PostResponse.ListItem> posts;

        if (keyword != null && !keyword.isBlank()) {
            // 검색
            posts = communityService.searchPosts(keyword, type, pageable);
        } else if (type != null && seasonId != null) {
            // 시즌 + 유형 필터
            posts = communityService.getPostsByTypeAndSeason(type, seasonId, pageable);
        } else if (seasonId != null) {
            // 시즌 필터만
            posts = communityService.getPostsBySeason(seasonId, pageable);
        } else if (type != null) {
            // 유형 필터만
            posts = communityService.getPostsByType(type, pageable);
        } else {
            // 인기 게시글
            posts = communityService.getPopularPosts(pageable);
        }

        return ResponseEntity.ok(ApiResponse.success(posts));
    }

    /**
     * 팀원 모집 게시판
     */
    @GetMapping("/recruitment")
    public ResponseEntity<ApiResponse<Page<PostResponse.ListItem>>> getRecruitmentPosts(
            @RequestParam(required = false) Long seasonId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<PostResponse.ListItem> posts;
        if (seasonId != null) {
            posts = communityService.getPostsByTypeAndSeason(PostType.RECRUITMENT, seasonId, pageable);
        } else {
            posts = communityService.getPostsByType(PostType.RECRUITMENT, pageable);
        }

        return ResponseEntity.ok(ApiResponse.success(posts));
    }

    /**
     * 공지사항 게시판
     */
    @GetMapping("/announcements")
    public ResponseEntity<ApiResponse<Page<PostResponse.ListItem>>> getAnnouncements(
            @RequestParam(required = false) Long seasonId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<PostResponse.ListItem> posts;
        if (seasonId != null) {
            posts = communityService.getPostsByTypeAndSeason(PostType.ANNOUNCEMENT, seasonId, pageable);
        } else {
            posts = communityService.getPostsByType(PostType.ANNOUNCEMENT, pageable);
        }

        return ResponseEntity.ok(ApiResponse.success(posts));
    }

    /**
     * 프로젝트 쇼케이스
     */
    @GetMapping("/showcase")
    public ResponseEntity<ApiResponse<Page<PostResponse.ListItem>>> getShowcases(
            @PageableDefault(size = 20, sort = "likeCount", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<PostResponse.ListItem> posts = communityService.getShowcases(pageable);
        return ResponseEntity.ok(ApiResponse.success(posts));
    }

    /**
     * Q&A 게시판
     */
    @GetMapping("/qna")
    public ResponseEntity<ApiResponse<Page<PostResponse.ListItem>>> getQnaPosts(
            @RequestParam(required = false) Long seasonId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<PostResponse.ListItem> posts;
        if (seasonId != null) {
            posts = communityService.getPostsByTypeAndSeason(PostType.QNA, seasonId, pageable);
        } else {
            posts = communityService.getPostsByType(PostType.QNA, pageable);
        }

        return ResponseEntity.ok(ApiResponse.success(posts));
    }

    /**
     * 내가 작성한 게시글 목록
     */
    @GetMapping("/posts/my")
    public ResponseEntity<ApiResponse<Page<PostResponse.ListItem>>> getMyPosts(
            @CurrentUser CustomUserPrincipal principal,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<PostResponse.ListItem> posts = communityService.getMyPosts(principal.getUserId(), pageable);
        return ResponseEntity.ok(ApiResponse.success(posts));
    }

    // ========== 좋아요 API ==========

    /**
     * 좋아요 토글
     */
    @PostMapping("/posts/{postId}/like")
    public ResponseEntity<ApiResponse<Boolean>> toggleLike(
            @CurrentUser CustomUserPrincipal principal,
            @PathVariable Long postId) {
        boolean liked = communityService.toggleLike(postId, principal.getUserId());
        String message = liked ? "좋아요를 추가했습니다." : "좋아요를 취소했습니다.";
        return ResponseEntity.ok(ApiResponse.success(liked, message));
    }

    // ========== 댓글 API ==========

    /**
     * 댓글 작성
     */
    @PostMapping("/posts/{postId}/comments")
    public ResponseEntity<ApiResponse<CommentResponse>> createComment(
            @CurrentUser CustomUserPrincipal principal,
            @PathVariable Long postId,
            @Valid @RequestBody CommentRequest.Create request) {
        CommentResponse comment = communityService.createComment(postId, principal.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.success(comment, "댓글이 작성되었습니다."));
    }

    /**
     * 댓글 목록 조회
     */
    @GetMapping("/posts/{postId}/comments")
    public ResponseEntity<ApiResponse<List<CommentResponse>>> getComments(
            @PathVariable Long postId) {
        List<CommentResponse> comments = communityService.getCommentsByPost(postId);
        return ResponseEntity.ok(ApiResponse.success(comments));
    }

    /**
     * 댓글 수정
     */
    @PutMapping("/comments/{commentId}")
    public ResponseEntity<ApiResponse<CommentResponse>> updateComment(
            @CurrentUser CustomUserPrincipal principal,
            @PathVariable Long commentId,
            @Valid @RequestBody CommentRequest.Update request) {
        CommentResponse comment = communityService.updateComment(commentId, principal.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.success(comment, "댓글이 수정되었습니다."));
    }

    /**
     * 댓글 삭제
     */
    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<ApiResponse<Void>> deleteComment(
            @CurrentUser CustomUserPrincipal principal,
            @PathVariable Long commentId) {
        communityService.deleteComment(commentId, principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success(null, "댓글이 삭제되었습니다."));
    }

    // ========== 관리자 API ==========

    /**
     * 게시글 고정 (관리자)
     */
    @PostMapping("/posts/{postId}/pin")
    public ResponseEntity<ApiResponse<Void>> pinPost(
            @CurrentUser CustomUserPrincipal principal,
            @PathVariable Long postId) {
        communityService.pinPost(postId, principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success(null, "게시글이 고정되었습니다."));
    }

    /**
     * 게시글 고정 해제 (관리자)
     */
    @DeleteMapping("/posts/{postId}/pin")
    public ResponseEntity<ApiResponse<Void>> unpinPost(
            @CurrentUser CustomUserPrincipal principal,
            @PathVariable Long postId) {
        communityService.unpinPost(postId, principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success(null, "게시글 고정이 해제되었습니다."));
    }
}
