package com.restartpoint.infra.storage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.UUID;

/**
 * 파일 업로드 서비스 (AWS S3)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileService {

    private final S3Client s3Client;

    @Value("${aws.s3.bucket:}")
    private String bucket;

    @Value("${aws.s3.region:ap-northeast-2}")
    private String region;

    // 허용되는 파일 확장자
    private static final String[] ALLOWED_EXTENSIONS = {"jpg", "jpeg", "png", "gif", "pdf", "webp"};

    // 최대 파일 크기 (10MB)
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;

    /**
     * 파일 업로드
     * @param file 업로드할 파일
     * @param directory 저장 디렉토리 (예: "certificates", "profiles")
     * @return 업로드된 파일의 URL
     */
    public String uploadFile(MultipartFile file, String directory) {
        // S3 클라이언트 확인
        if (s3Client == null) {
            throw new IllegalStateException("S3가 설정되지 않았습니다. AWS 자격증명을 확인해주세요.");
        }

        // 파일 유효성 검사
        validateFile(file);

        // 고유한 파일명 생성
        String originalFilename = file.getOriginalFilename();
        String extension = getFileExtension(originalFilename);
        String newFilename = UUID.randomUUID().toString() + "." + extension;
        String key = directory + "/" + newFilename;

        try {
            // S3에 업로드
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(file.getContentType())
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(file.getBytes()));

            // 업로드된 파일의 URL 반환
            String fileUrl = String.format("https://%s.s3.%s.amazonaws.com/%s", bucket, region, key);
            log.info("파일 업로드 완료: {}", fileUrl);

            return fileUrl;
        } catch (IOException e) {
            log.error("파일 업로드 실패", e);
            throw new RuntimeException("파일 업로드에 실패했습니다.", e);
        }
    }

    /**
     * 파일 삭제
     * @param fileUrl 삭제할 파일의 URL
     */
    public void deleteFile(String fileUrl) {
        if (s3Client == null || fileUrl == null || fileUrl.isEmpty()) {
            return;
        }

        try {
            // URL에서 key 추출
            String key = extractKeyFromUrl(fileUrl);
            if (key == null) {
                return;
            }

            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build();

            s3Client.deleteObject(deleteObjectRequest);
            log.info("파일 삭제 완료: {}", fileUrl);
        } catch (Exception e) {
            log.error("파일 삭제 실패: {}", fileUrl, e);
        }
    }

    /**
     * 파일 유효성 검사
     */
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("파일이 없습니다.");
        }

        // 파일 크기 검사
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("파일 크기는 10MB를 초과할 수 없습니다.");
        }

        // 확장자 검사
        String extension = getFileExtension(file.getOriginalFilename());
        boolean isAllowed = false;
        for (String allowedExt : ALLOWED_EXTENSIONS) {
            if (allowedExt.equalsIgnoreCase(extension)) {
                isAllowed = true;
                break;
            }
        }
        if (!isAllowed) {
            throw new IllegalArgumentException("허용되지 않는 파일 형식입니다. (허용: jpg, jpeg, png, gif, pdf, webp)");
        }
    }

    /**
     * 파일 확장자 추출
     */
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }

    /**
     * URL에서 S3 key 추출
     */
    private String extractKeyFromUrl(String fileUrl) {
        try {
            String prefix = String.format("https://%s.s3.%s.amazonaws.com/", bucket, region);
            if (fileUrl.startsWith(prefix)) {
                return fileUrl.substring(prefix.length());
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }
}
