package com.restartpoint.domain.community.dto;

import com.restartpoint.domain.community.entity.Comment;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Builder
public class CommentResponse {

    private Long id;
    private String content;
    private AuthorInfo author;
    private Long parentId;
    private List<CommentResponse> replies;
    private boolean deleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Getter
    @Builder
    public static class AuthorInfo {
        private Long id;
        private String name;

        public static AuthorInfo from(com.restartpoint.domain.user.entity.User user) {
            return AuthorInfo.builder()
                    .id(user.getId())
                    .name(user.getName())
                    .build();
        }
    }

    public static CommentResponse from(Comment comment) {
        return CommentResponse.builder()
                .id(comment.getId())
                .content(comment.getContent())
                .author(AuthorInfo.from(comment.getAuthor()))
                .parentId(comment.getParent() != null ? comment.getParent().getId() : null)
                .replies(comment.getReplies().stream()
                        .map(CommentResponse::from)
                        .collect(Collectors.toList()))
                .deleted(comment.isDeleted())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .build();
    }

    /**
     * 대댓글 없이 단순 응답
     */
    public static CommentResponse simple(Comment comment) {
        return CommentResponse.builder()
                .id(comment.getId())
                .content(comment.getContent())
                .author(AuthorInfo.from(comment.getAuthor()))
                .parentId(comment.getParent() != null ? comment.getParent().getId() : null)
                .deleted(comment.isDeleted())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .build();
    }
}
