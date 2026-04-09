package com.restartpoint.infra.ai;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;

class GroqServiceTest {

    private MockWebServer mockWebServer;
    private GroqService groqService;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        WebClient webClient = WebClient.builder()
                .baseUrl(mockWebServer.url("/").toString())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer test-api-key")
                .build();

        groqService = new GroqService(webClient);
        setField(groqService, "model", "llama-3.1-70b-versatile");
        setField(groqService, "maxTokens", 4096);
        setField(groqService, "timeoutSeconds", 30);
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    @DisplayName("Groq API 호출이 성공하면 응답 메시지를 반환한다")
    void chatReturnsResponseOnSuccess() throws InterruptedException {
        String mockResponse = """
                {
                    "id": "chatcmpl-123",
                    "object": "chat.completion",
                    "created": 1677652288,
                    "model": "llama-3.1-70b-versatile",
                    "choices": [{
                        "index": 0,
                        "message": {
                            "role": "assistant",
                            "content": "안녕하세요! 어떻게 도와드릴까요?"
                        },
                        "finish_reason": "stop"
                    }],
                    "usage": {
                        "prompt_tokens": 9,
                        "completion_tokens": 12,
                        "total_tokens": 21
                    }
                }
                """;

        mockWebServer.enqueue(new MockResponse()
                .setBody(mockResponse)
                .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE));

        String result = groqService.chat("You are a helpful assistant.", "안녕하세요");

        assertThat(result).isEqualTo("안녕하세요! 어떻게 도와드릴까요?");

        RecordedRequest request = mockWebServer.takeRequest();
        assertThat(request.getPath()).isEqualTo("/chat/completions");
        assertThat(request.getMethod()).isEqualTo("POST");
    }

    @Test
    @DisplayName("Groq API 응답에 choices가 없으면 null을 반환한다")
    void chatReturnsNullWhenNoChoices() {
        String mockResponse = """
                {
                    "id": "chatcmpl-123",
                    "object": "chat.completion",
                    "choices": []
                }
                """;

        mockWebServer.enqueue(new MockResponse()
                .setBody(mockResponse)
                .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE));

        String result = groqService.chat("You are a helpful assistant.", "안녕하세요");

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("Groq API 호출이 실패하면 null을 반환한다")
    void chatReturnsNullOnError() {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(500)
                .setBody("Internal Server Error"));

        String result = groqService.chat("You are a helpful assistant.", "안녕하세요");

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("Groq API 응답 형식이 잘못되면 null을 반환한다")
    void chatReturnsNullOnInvalidResponse() {
        mockWebServer.enqueue(new MockResponse()
                .setBody("invalid json")
                .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE));

        String result = groqService.chat("You are a helpful assistant.", "안녕하세요");

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("JSON 배열 응답을 올바르게 파싱한다")
    void chatParsesJsonArrayResponse() {
        String mockResponse = """
                {
                    "choices": [{
                        "message": {
                            "content": "[{\\"teamId\\": 1, \\"matchScore\\": 85}]"
                        }
                    }]
                }
                """;

        mockWebServer.enqueue(new MockResponse()
                .setBody(mockResponse)
                .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE));

        String result = groqService.chat("Return JSON", "test");

        assertThat(result).contains("teamId");
        assertThat(result).contains("matchScore");
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("테스트 필드 설정에 실패했습니다: " + fieldName, exception);
        }
    }
}
