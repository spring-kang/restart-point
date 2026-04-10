package com.restartpoint.domain.community.repository;

import com.restartpoint.domain.community.entity.PostLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PostLikeRepository extends JpaRepository<PostLike, Long> {

    /**
     * 특정 사용자의 게시글 좋아요 조회
     */
    @Query("SELECT pl FROM PostLike pl " +
           "WHERE pl.post.id = :postId AND pl.user.id = :userId")
    Optional<PostLike> findByPostIdAndUserId(
            @Param("postId") Long postId,
            @Param("userId") Long userId);

    /**
     * 좋아요 여부 확인
     */
    boolean existsByPostIdAndUserId(Long postId, Long userId);

    /**
     * 게시글의 좋아요 수
     */
    long countByPostId(Long postId);
}
