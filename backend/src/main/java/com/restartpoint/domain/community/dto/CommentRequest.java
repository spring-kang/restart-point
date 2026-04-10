package com.restartpoint.domain.community.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class CommentRequest {

    @Getter
    @NoArgsConstructor
    public static class Create {
        @NotBlank(message = "댓글 내용은 필수입니다")
        private String content;

        private Long parentId;  // 대댓글인 경우

        @Builder
        public Create(String content, Long parentId) {
            this.content = content;
            this.parentId = parentId;
        }
    }

    @Getter
    @NoArgsConstructor
    public static class Update {
        @NotBlank(message = "댓글 내용은 필수입니다")
        private String content;

        @Builder
        public Update(String content) {
            this.content = content;
        }
    }
}
