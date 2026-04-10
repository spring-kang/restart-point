package com.restartpoint.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Swagger(OpenAPI) 설정
 */
@Configuration
public class SwaggerConfig {

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    @Bean
    public OpenAPI openAPI() {
        String securitySchemeName = "bearerAuth";

        return new OpenAPI()
                .info(apiInfo())
                .servers(List.of(
                        new Server().url(baseUrl).description("Current Server")
                ))
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .name(securitySchemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("JWT 토큰을 입력하세요. (Bearer 접두사 불필요)")
                        )
                );
    }

    private Info apiInfo() {
        return new Info()
                .title("Re:Start Point API")
                .description("""
                        ## Re:Start Point API 문서

                        부트캠프 수료 이후의 성장을 다시 시작하는 AI 프로젝트 러닝 플랫폼

                        ### 주요 기능
                        - **인증**: 회원가입, 로그인, 이메일 인증
                        - **사용자**: 프로필 관리, 수료 인증
                        - **시즌**: 공모전 시즌 관리
                        - **팀**: 팀 생성, 지원, 멤버 관리
                        - **프로젝트**: 워크스페이스, 체크포인트, AI 코칭
                        - **심사**: 루브릭 기반 심사, AI 분석
                        - **성장 리포트**: AI 기반 개인/팀 리포트
                        - **커뮤니티**: 게시판, 댓글, 좋아요
                        - **알림**: 실시간 알림

                        ### 인증 방식
                        JWT Bearer 토큰을 사용합니다. 로그인 후 발급받은 토큰을 `Authorization` 헤더에 포함하세요.

                        ```
                        Authorization: Bearer {token}
                        ```
                        """)
                .version("1.0.0")
                .contact(new Contact()
                        .name("Re:Start Point Team")
                        .email("support@restartpoint.com")
                )
                .license(new License()
                        .name("MIT License")
                        .url("https://opensource.org/licenses/MIT")
                );
    }
}
