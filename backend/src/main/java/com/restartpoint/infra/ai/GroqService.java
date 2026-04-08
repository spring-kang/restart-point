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
public class GroqService {

    private final WebClient groqWebClient;

    @Value("${groq.model}")
    private String model;

    @Value("${groq.max-tokens}")
    private int maxTokens;

    /**
     * Groq API를 호출하여 메시지에 대한 응답을 받습니다.
     * OpenAI 호환 API 형식을 사용합니다.
     */
    public String chat(String systemPrompt, String userMessage) {
        Map<String, Object> requestBody = Map.of(
                "model", model,
                "max_tokens", maxTokens,
                "temperature", 0.7,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userMessage)
                )
        );

        try {
            Map<String, Object> response = groqWebClient.post()
                    .uri("/chat/completions")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response != null && response.containsKey("choices")) {
                List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
                if (!choices.isEmpty()) {
                    Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                    return (String) message.get("content");
                }
            }

            log.error("Groq API 응답 형식 오류: {}", response);
            return null;
        } catch (Exception e) {
            log.error("Groq API 호출 실패: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Groq API를 비동기로 호출합니다.
     */
    public Mono<String> chatAsync(String systemPrompt, String userMessage) {
        Map<String, Object> requestBody = Map.of(
                "model", model,
                "max_tokens", maxTokens,
                "temperature", 0.7,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userMessage)
                )
        );

        return groqWebClient.post()
                .uri("/chat/completions")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    if (response.containsKey("choices")) {
                        List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
                        if (!choices.isEmpty()) {
                            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                            return (String) message.get("content");
                        }
                    }
                    return null;
                })
                .doOnError(e -> log.error("Groq API 비동기 호출 실패: {}", e.getMessage()));
    }
}
