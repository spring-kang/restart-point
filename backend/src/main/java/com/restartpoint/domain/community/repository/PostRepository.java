package com.restartpoint.domain.community.repository;

import com.restartpoint.domain.community.entity.Post;
import com.restartpoint.domain.community.entity.PostType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

    /**
     * 게시글 상세 조회 (작성자 정보 포함)
     */
    @Query("SELECT p FROM Post p " +
           "JOIN FETCH p.author " +
           "LEFT JOIN FETCH p.season " +
           "LEFT JOIN FETCH p.project " +
           "WHERE p.id = :postId AND p.deleted = false")
    Optional<Post> findByIdWithAuthor(@Param("postId") Long postId);

    /**
     * 게시판별 게시글 목록 조회
     */
    @Query("SELECT p FROM Post p " +
           "JOIN FETCH p.author " +
           "WHERE p.postType = :postType AND p.deleted = false " +
           "ORDER BY p.pinned DESC, p.createdAt DESC")
    Page<Post> findByPostType(@Param("postType") PostType postType, Pageable pageable);

    /**
     * 시즌별 게시글 목록 조회
     */
    @Query("SELECT p FROM Post p " +
           "JOIN FETCH p.author " +
           "WHERE p.season.id = :seasonId AND p.deleted = false " +
           "ORDER BY p.pinned DESC, p.createdAt DESC")
    Page<Post> findBySeasonId(@Param("seasonId") Long seasonId, Pageable pageable);

    /**
     * 시즌별 특정 유형 게시글 조회
     */
    @Query("SELECT p FROM Post p " +
           "JOIN FETCH p.author " +
           "WHERE p.postType = :postType AND p.season.id = :seasonId AND p.deleted = false " +
           "ORDER BY p.pinned DESC, p.createdAt DESC")
    Page<Post> findByPostTypeAndSeasonId(
            @Param("postType") PostType postType,
            @Param("seasonId") Long seasonId,
            Pageable pageable);

    /**
     * 프로젝트 쇼케이스 목록 조회
     */
    @Query("SELECT p FROM Post p " +
           "JOIN FETCH p.author " +
           "LEFT JOIN FETCH p.project proj " +
           "WHERE p.postType = 'SHOWCASE' AND p.deleted = false " +
           "ORDER BY p.likeCount DESC, p.createdAt DESC")
    Page<Post> findShowcases(Pageable pageable);

    /**
     * 사용자가 작성한 게시글 목록
     */
    @Query("SELECT p FROM Post p " +
           "WHERE p.author.id = :userId AND p.deleted = false " +
           "ORDER BY p.createdAt DESC")
    Page<Post> findByAuthorId(@Param("userId") Long userId, Pageable pageable);

    /**
     * 검색 (제목, 내용)
     */
    @Query("SELECT p FROM Post p " +
           "JOIN FETCH p.author " +
           "WHERE p.deleted = false " +
           "AND (p.title LIKE %:keyword% OR p.content LIKE %:keyword%) " +
           "ORDER BY p.createdAt DESC")
    Page<Post> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    /**
     * 특정 유형 내 검색
     */
    @Query("SELECT p FROM Post p " +
           "JOIN FETCH p.author " +
           "WHERE p.postType = :postType AND p.deleted = false " +
           "AND (p.title LIKE %:keyword% OR p.content LIKE %:keyword%) " +
           "ORDER BY p.createdAt DESC")
    Page<Post> searchByKeywordAndPostType(
            @Param("keyword") String keyword,
            @Param("postType") PostType postType,
            Pageable pageable);

    /**
     * 고정 게시글 목록
     */
    @Query("SELECT p FROM Post p " +
           "JOIN FETCH p.author " +
           "WHERE p.pinned = true AND p.deleted = false " +
           "ORDER BY p.createdAt DESC")
    List<Post> findPinnedPosts();

    /**
     * 인기 게시글 (좋아요 순)
     */
    @Query("SELECT p FROM Post p " +
           "JOIN FETCH p.author " +
           "WHERE p.deleted = false " +
           "ORDER BY p.likeCount DESC, p.viewCount DESC")
    Page<Post> findPopularPosts(Pageable pageable);
}
