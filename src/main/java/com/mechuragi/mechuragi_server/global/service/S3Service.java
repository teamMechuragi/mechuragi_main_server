package com.mechuragi.mechuragi_server.global.service;

import com.mechuragi.mechuragi_server.global.exception.BusinessException;
import com.mechuragi.mechuragi_server.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3Service {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    @Value("${cloud.aws.s3.bucket}")
    private String bucketName;

    @Value("${cloud.aws.region.static}")
    private String region;

    @Value("${cloud.aws.cloudfront.domain}")
    private String cloudfrontDomain;

    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList("jpg", "jpeg", "png", "gif", "webp");
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

    public String uploadImage(MultipartFile file, String folder) {
        validateFile(file);

        String fileName = generateFileName(file.getOriginalFilename());
        // CloudFront path pattern `/images/*`에 맞게 images/ prefix 추가
        String key = "images/" + folder + "/" + fileName;

        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType(file.getContentType())
                    .contentLength(file.getSize())
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

            // CloudFront URL 반환 (S3 직접 접근 대신 CDN 경유)
            String imageUrl = String.format("https://%s/%s", cloudfrontDomain, key);
            log.info("Image uploaded successfully: {}", imageUrl);
            return imageUrl;

        } catch (IOException e) {
            log.error("Failed to upload image to S3", e);
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED);
        } catch (Exception e) {
            log.error("Unexpected error during S3 upload", e);
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    public void deleteImage(String imageUrl) {
        try {
            String key = extractKeyFromUrl(imageUrl);

            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            s3Client.deleteObject(deleteObjectRequest);
            log.info("Image deleted successfully: {}", imageUrl);

        } catch (Exception e) {
            log.error("Failed to delete image from S3: {}", imageUrl, e);
            // 삭제 실패해도 예외를 던지지 않음 (서비스 연속성을 위해)
        }
    }

    /**
     * Pre-signed URL 발급 (클라이언트가 S3에 직접 업로드)
     * @param folder 저장할 폴더 (예: vote-images)
     * @param filename 원본 파일명
     * @param contentType 파일의 Content-Type (예: image/jpeg)
     * @return uploadUrl (업로드용 pre-signed URL), imageUrl (업로드 후 접근할 CDN URL)
     */
    public Map<String, String> generatePresignedUploadUrl(String folder, String filename, String contentType) {
        // 파일 확장자 검증
        String extension = getFileExtension(filename).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BusinessException(ErrorCode.INVALID_FILE_FORMAT);
        }

        // Content-Type 검증
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BusinessException(ErrorCode.INVALID_FILE_FORMAT);
        }

        // 고유 파일명 생성
        String uniqueFileName = UUID.randomUUID().toString() + "." + extension;
        String key = "images/" + folder + "/" + uniqueFileName;

        // Pre-signed URL 생성 (10분 유효)
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType(contentType)
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(10))
                .putObjectRequest(putObjectRequest)
                .build();

        PresignedPutObjectRequest presignedRequest = s3Presigner.presignPutObject(presignRequest);
        String uploadUrl = presignedRequest.url().toString();

        // 업로드 완료 후 접근할 CDN URL
        String imageUrl = String.format("https://%s/%s", cloudfrontDomain, key);

        log.info("Pre-signed URL generated for key: {}", key);

        return Map.of(
                "uploadUrl", uploadUrl,
                "imageUrl", imageUrl
        );
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.FILE_NOT_FOUND);
        }

        // 파일 크기 검증
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException(ErrorCode.FILE_SIZE_EXCEEDED);
        }

        // 파일 확장자 검증
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new BusinessException(ErrorCode.INVALID_FILE_FORMAT);
        }

        String extension = getFileExtension(originalFilename).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BusinessException(ErrorCode.INVALID_FILE_FORMAT);
        }

        // Content-Type 검증
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BusinessException(ErrorCode.INVALID_FILE_FORMAT);
        }
    }

    private String generateFileName(String originalFilename) {
        String extension = getFileExtension(originalFilename);
        return UUID.randomUUID().toString() + "." + extension;
    }

    private String getFileExtension(String filename) {
        int lastIndexOf = filename.lastIndexOf(".");
        if (lastIndexOf == -1) {
            return "";
        }
        return filename.substring(lastIndexOf + 1);
    }

    private String extractKeyFromUrl(String imageUrl) {
        // CloudFront URL에서 S3 키 추출
        // https://cloudfront-domain/images/folder/filename.jpg -> images/folder/filename.jpg
        String cloudfrontUrl = String.format("https://%s/", cloudfrontDomain);
        if (imageUrl.startsWith(cloudfrontUrl)) {
            return imageUrl.substring(cloudfrontUrl.length());
        }

        // 기존 S3 URL도 지원 (하위 호환성)
        // https://bucket-name.s3.region.amazonaws.com/folder/filename.jpg -> folder/filename.jpg
        String bucketUrl = String.format("https://%s.s3.%s.amazonaws.com/", bucketName, region);
        if (imageUrl.startsWith(bucketUrl)) {
            return imageUrl.substring(bucketUrl.length());
        }

        throw new BusinessException(ErrorCode.INVALID_FILE_URL);
    }
}