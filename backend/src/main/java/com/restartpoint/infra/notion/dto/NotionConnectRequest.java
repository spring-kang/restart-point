package com.restartpoint.infra.notion.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class NotionConnectRequest {

    @NotBlank(message = "인증 코드는 필수입니다")
    private String code;

    private String redirectUri;
}
