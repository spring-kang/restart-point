package com.restartpoint.domain.community.repository;

import com.restartpoint.domain.community.entity.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    /**
     * 댓글 상세 조회 (작성자 포함)
     */
    @Query("SELECT c FROM Comment c " +
           "JOIN FETCH c.author " +
           "WHERE c.id = :commentId")
    Optional<Comment> findByIdWithAuthor(@Param("commentId") Long commentId);

    /**
     * 게시글의 최상위 댓글 목록 조회 (대댓글 제외)
     */
    @Query("SELECT c FROM Comment c " +
           "JOIN FETCH c.author " +
           "WHERE c.post.id = :postId AND c.parent IS NULL " +
           "ORDER BY c.createdAt ASC")
    List<Comment> findTopLevelCommentsByPostId(@Param("postId") Long postId);

    /**
     * 게시글의 모든 댓글 조회 (대댓글 포함)
     */
    @Query("SELECT c FROM Comment c " +
           "JOIN FETCH c.author " +
           "LEFT JOIN FETCH c.parent " +
           "WHERE c.post.id = :postId " +
           "ORDER BY c.createdAt ASC")
    List<Comment> findAllByPostId(@Param("postId") Long postId);

    /**
     * 특정 댓글의 대댓글 목록
     */
    @Query("SELECT c FROM Comment c " +
           "JOIN FETCH c.author " +
           "WHERE c.parent.id = :parentId " +
           "ORDER BY c.createdAt ASC")
    List<Comment> findRepliesByParentId(@Param("parentId") Long parentId);

    /**
     * 사용자가 작성한 댓글 목록
     */
    @Query("SELECT c FROM Comment c " +
           "JOIN FETCH c.post " +
           "WHERE c.author.id = :userId AND c.deleted = false " +
           "ORDER BY c.createdAt DESC")
    Page<Comment> findByAuthorId(@Param("userId") Long userId, Pageable pageable);

    /**
     * 게시글의 댓글 수 조회
     */
    @Query("SELECT COUNT(c) FROM Comment c " +
           "WHERE c.post.id = :postId AND c.deleted = false")
    long countByPostId(@Param("postId") Long postId);
}
