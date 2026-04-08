package com.restartpoint.infra.ai;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClaudeService {

    private final WebClient claudeWebClient;

    @Value("${claude.model}")
    private String model;

    @Value("${claude.max-tokens}")
    private int maxTokens;

    /**
     * Claude API를 호출하여 메시지에 대한 응답을 받습니다.
     */
    public String chat(String systemPrompt, String userMessage) {
        Map<String, Object> requestBody = Map.of(
                "model", model,
                "max_tokens", maxTokens,
                "system", systemPrompt,
                "messages", List.of(
                        Map.of("role", "user", "content", userMessage)
                )
        );

        try {
            Map<String, Object> response = claudeWebClient.post()
                    .uri("/messages")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response != null && response.containsKey("content")) {
                List<Map<String, Object>> contentList = (List<Map<String, Object>>) response.get("content");
                if (!contentList.isEmpty()) {
                    return (String) contentList.get(0).get("text");
                }
            }

            log.error("Claude API 응답 형식 오류: {}", response);
            return null;
        } catch (Exception e) {
            log.error("Claude API 호출 실패: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Claude API를 비동기로 호출합니다.
     */
    public Mono<String> chatAsync(String systemPrompt, String userMessage) {
        Map<String, Object> requestBody = Map.of(
                "model", model,
                "max_tokens", maxTokens,
                "system", systemPrompt,
                "messages", List.of(
                        Map.of("role", "user", "content", userMessage)
                )
        );

        return claudeWebClient.post()
                .uri("/messages")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    if (response.containsKey("content")) {
                        List<Map<String, Object>> contentList = (List<Map<String, Object>>) response.get("content");
                        if (!contentList.isEmpty()) {
                            return (String) contentList.get(0).get("text");
                        }
                    }
                    return null;
                })
                .doOnError(e -> log.error("Claude API 비동기 호출 실패: {}", e.getMessage()));
    }
}
