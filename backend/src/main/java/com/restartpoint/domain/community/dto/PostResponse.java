package com.restartpoint.domain.community.dto;

import com.restartpoint.domain.community.entity.Post;
import com.restartpoint.domain.community.entity.PostType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class PostResponse {

    private Long id;
    private PostType postType;
    private String title;
    private String content;
    private AuthorInfo author;
    private SeasonInfo season;
    private ProjectInfo project;
    private int viewCount;
    private int likeCount;
    private int commentCount;
    private boolean pinned;
    private boolean liked;  // 현재 사용자의 좋아요 여부
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Getter
    @Builder
    public static class AuthorInfo {
        private Long id;
        private String name;
        private String email;

        public static AuthorInfo from(com.restartpoint.domain.user.entity.User user) {
            return AuthorInfo.builder()
                    .id(user.getId())
                    .name(user.getName())
                    .email(user.getEmail())
                    .build();
        }
    }

    @Getter
    @Builder
    public static class SeasonInfo {
        private Long id;
        private String title;

        public static SeasonInfo from(com.restartpoint.domain.season.entity.Season season) {
            if (season == null) return null;
            return SeasonInfo.builder()
                    .id(season.getId())
                    .title(season.getTitle())
                    .build();
        }
    }

    @Getter
    @Builder
    public static class ProjectInfo {
        private Long id;
        private String name;

        public static ProjectInfo from(com.restartpoint.domain.project.entity.Project project) {
            if (project == null) return null;
            return ProjectInfo.builder()
                    .id(project.getId())
                    .name(project.getName())
                    .build();
        }
    }

    public static PostResponse from(Post post, boolean liked) {
        return PostResponse.builder()
                .id(post.getId())
                .postType(post.getPostType())
                .title(post.getTitle())
                .content(post.getContent())
                .author(AuthorInfo.from(post.getAuthor()))
                .season(SeasonInfo.from(post.getSeason()))
                .project(ProjectInfo.from(post.getProject()))
                .viewCount(post.getViewCount())
                .likeCount(post.getLikeCount())
                .commentCount(post.getCommentCount())
                .pinned(post.isPinned())
                .liked(liked)
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .build();
    }

    public static PostResponse from(Post post) {
        return from(post, false);
    }

    /**
     * 목록용 간략 응답
     */
    @Getter
    @Builder
    public static class ListItem {
        private Long id;
        private PostType postType;
        private String title;
        private AuthorInfo author;
        private int viewCount;
        private int likeCount;
        private int commentCount;
        private boolean pinned;
        private LocalDateTime createdAt;

        public static ListItem from(Post post) {
            return ListItem.builder()
                    .id(post.getId())
                    .postType(post.getPostType())
                    .title(post.getTitle())
                    .author(AuthorInfo.from(post.getAuthor()))
                    .viewCount(post.getViewCount())
                    .likeCount(post.getLikeCount())
                    .commentCount(post.getCommentCount())
                    .pinned(post.isPinned())
                    .createdAt(post.getCreatedAt())
                    .build();
        }
    }
}
