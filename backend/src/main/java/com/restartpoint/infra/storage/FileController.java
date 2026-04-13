package com.restartpoint.infra.storage;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 파일 업로드 API
 */
@Tag(name = "File", description = "파일 업로드 API")
@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    @Operation(
            summary = "Presigned URL 생성",
            description = "S3 파일에 대한 임시 접근 URL을 생성합니다. (1시간 유효)"
    )
    @ApiResponse(
            responseCode = "200",
            description = "Presigned URL 생성 성공",
            content = @Content(schema = @Schema(implementation = PresignedUrlResponse.class))
    )
    @GetMapping("/presign")
    public ResponseEntity<Map<String, String>> getPresignedUrl(
            @Parameter(description = "S3 파일 URL", required = true)
            @RequestParam("url") String fileUrl
    ) {
        String presignedUrl = fileService.generatePresignedUrl(fileUrl);
        return ResponseEntity.ok(Map.of("presignedUrl", presignedUrl));
    }

    @Operation(
            summary = "파일 업로드",
            description = "파일을 S3에 업로드하고 URL을 반환합니다. (최대 10MB, 허용 형식: jpg, jpeg, png, gif, pdf, webp)"
    )
    @ApiResponse(
            responseCode = "200",
            description = "업로드 성공",
            content = @Content(schema = @Schema(implementation = FileUploadResponse.class))
    )
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> uploadFile(
            @Parameter(description = "업로드할 파일", required = true)
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "저장 디렉토리 (기본값: general)", example = "certificates")
            @RequestParam(value = "directory", defaultValue = "general") String directory
    ) {
        String url = fileService.uploadFile(file, directory);
        return ResponseEntity.ok(Map.of("url", url));
    }

    /**
     * 파일 업로드 응답 스키마 (Swagger 문서용)
     */
    @Schema(description = "파일 업로드 응답")
    private static class FileUploadResponse {
        @Schema(description = "업로드된 파일 URL", example = "https://bucket.s3.ap-northeast-2.amazonaws.com/certificates/uuid.jpg")
        public String url;
    }

    /**
     * Presigned URL 응답 스키마 (Swagger 문서용)
     */
    @Schema(description = "Presigned URL 응답")
    private static class PresignedUrlResponse {
        @Schema(description = "임시 접근 URL (1시간 유효)", example = "https://bucket.s3.ap-northeast-2.amazonaws.com/certificates/uuid.jpg?X-Amz-Algorithm=...")
        public String presignedUrl;
    }
}
