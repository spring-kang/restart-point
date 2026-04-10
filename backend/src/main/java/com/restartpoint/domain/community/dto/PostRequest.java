package com.restartpoint.domain.community.dto;

import com.restartpoint.domain.community.entity.PostType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class PostRequest {

    @Getter
    @NoArgsConstructor
    public static class Create {
        @NotNull(message = "게시글 유형은 필수입니다")
        private PostType postType;

        @NotBlank(message = "제목은 필수입니다")
        @Size(max = 200, message = "제목은 200자 이내로 작성해주세요")
        private String title;

        @NotBlank(message = "내용은 필수입니다")
        private String content;

        private Long seasonId;
        private Long projectId;

        @Builder
        public Create(PostType postType, String title, String content,
                      Long seasonId, Long projectId) {
            this.postType = postType;
            this.title = title;
            this.content = content;
            this.seasonId = seasonId;
            this.projectId = projectId;
        }
    }

    @Getter
    @NoArgsConstructor
    public static class Update {
        @NotBlank(message = "제목은 필수입니다")
        @Size(max = 200, message = "제목은 200자 이내로 작성해주세요")
        private String title;

        @NotBlank(message = "내용은 필수입니다")
        private String content;

        @Builder
        public Update(String title, String content) {
            this.title = title;
            this.content = content;
        }
    }
}
