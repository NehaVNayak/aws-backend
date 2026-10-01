package com.example.s3microservice.service;

import com.example.s3microservice.config.AwsS3Config;
import com.example.s3microservice.dto.BucketStatusDto;
import com.example.s3microservice.dto.PresignedUrlResponseDto;
import com.example.s3microservice.dto.S3ObjectDto;
import com.example.s3microservice.dto.UploadResponseDto;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.IOException;
import java.net.URLConnection;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class S3Service {

    private static final Logger log = LoggerFactory.getLogger(S3Service.class);

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final AwsS3Config awsS3Config;

    public S3Service(
            S3Client s3Client,
            S3Presigner s3Presigner,
            AwsS3Config awsS3Config) {

        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
        this.awsS3Config = awsS3Config;
    }

    // ============================================================
    // BUCKET STATUS
    // ============================================================

    public BucketStatusDto getBucketStatus() {

        String bucketName = awsS3Config.getBucketName();
        String region = awsS3Config.getRegion();

        String authType = awsS3Config.hasExplicitCredentials()
                ? "IAM User Access Key & Secret"
                : "AWS Default Credential Chain (EC2 / IAM Role)";

        if (bucketName == null
                || bucketName.trim().isEmpty()
                || bucketName.contains("YOUR_")) {

            return new BucketStatusDto(
                    false,
                    false,
                    bucketName == null ? "" : bucketName,
                    region,
                    0,
                    0,
                    "0 B",
                    authType,
                    "Bucket name is not configured. Please open .env file and set AWS_S3_BUCKET_NAME."
            );
        }

        try {

            // Verify bucket accessibility
            HeadBucketRequest headBucketRequest =
                    HeadBucketRequest.builder()
                            .bucket(bucketName)
                            .build();

            s3Client.headBucket(headBucketRequest);

            // Fetch summary statistics
            ListObjectsV2Request listRequest =
                    ListObjectsV2Request.builder()
                            .bucket(bucketName)
                            .maxKeys(1000)
                            .build();

            ListObjectsV2Response listResponse =
                    s3Client.listObjectsV2(listRequest);

            long totalCount = listResponse.keyCount();

            long totalBytes = listResponse.contents()
                    .stream()
                    .mapToLong(S3Object::size)
                    .sum();

            return new BucketStatusDto(
                    true,
                    true,
                    bucketName,
                    region,
                    totalCount,
                    totalBytes,
                    S3ObjectDto.formatBytes(totalBytes),
                    authType,
                    "Connected successfully to S3 bucket '" + bucketName + "'"
            );

        } catch (NoSuchBucketException e) {

            return new BucketStatusDto(
                    true,
                    false,
                    bucketName,
                    region,
                    0,
                    0,
                    "0 B",
                    authType,
                    "Bucket '" + bucketName
                            + "' does not exist in region '"
                            + region
                            + "'. Please check bucket name or region in .env."
            );

        } catch (S3Exception e) {

            log.error(
                    "S3 Error checking status: {}",
                    e.awsErrorDetails().errorMessage()
            );

            return new BucketStatusDto(
                    true,
                    false,
                    bucketName,
                    region,
                    0,
                    0,
                    "0 B",
                    authType,
                    "AWS Error ("
                            + e.awsErrorDetails().errorCode()
                            + "): "
                            + e.awsErrorDetails().errorMessage()
            );

        } catch (Exception e) {

            log.error(
                    "General error checking bucket status",
                    e
            );

            return new BucketStatusDto(
                    true,
                    false,
                    bucketName,
                    region,
                    0,
                    0,
                    "0 B",
                    authType,
                    "Connection error: " + e.getMessage()
            );
        }
    }

    // ============================================================
    // LIST OBJECTS
    // ============================================================

    public List<S3ObjectDto> listObjects(String prefix) {

        String bucketName = awsS3Config.getBucketName();

        validateBucketConfigured();

        ListObjectsV2Request.Builder requestBuilder =
                ListObjectsV2Request.builder()
                        .bucket(bucketName);

        if (prefix != null && !prefix.trim().isEmpty()) {

            requestBuilder.prefix(prefix.trim());
        }

        ListObjectsV2Response response =
                s3Client.listObjectsV2(requestBuilder.build());

        List<S3ObjectDto> items = new ArrayList<>();

        for (S3Object s3Object : response.contents()) {

            String key = s3Object.key();

            // Don't show empty folder markers
            if (key.endsWith("/")) {
                continue;
            }

            String fileName =
                    key.contains("/")
                            ? key.substring(key.lastIndexOf('/') + 1)
                            : key;

            String presignedUrl = null;

            try {

                presignedUrl =
                        generatePresignedUrl(
                                key,
                                Duration.ofHours(2)
                        );

            } catch (Exception e) {

                log.warn(
                        "Could not generate presigned URL for key {}: {}",
                        key,
                        e.getMessage()
                );
            }

            String contentType =
                    detectContentType(fileName);

            items.add(
                    new S3ObjectDto(
                            key,
                            fileName,
                            s3Object.size(),
                            S3ObjectDto.formatBytes(s3Object.size()),
                            s3Object.lastModified(),
                            s3Object.eTag(),
                            s3Object.storageClassAsString(),
                            presignedUrl,
                            contentType
                    )
            );
        }

        // Sort latest modified first
        items.sort(
                Comparator.comparing(
                        S3ObjectDto::getLastModified,
                        Comparator.nullsLast(
                                Comparator.reverseOrder()
                        )
                )
        );

        return items;
    }

    // ============================================================
    // UPLOAD FILE
    // ============================================================

    public UploadResponseDto uploadFile(
            MultipartFile file,
            String prefix) throws IOException {

        String bucketName =
                awsS3Config.getBucketName();

        validateBucketConfigured();

        if (file.isEmpty()) {

            throw new IllegalArgumentException(
                    "Cannot upload empty file"
            );
        }

        String originalFilename =
                file.getOriginalFilename();

        if (originalFilename == null
                || originalFilename.trim().isEmpty()) {

            originalFilename =
                    "unnamed_file_"
                            + System.currentTimeMillis();
        }

        // --------------------------------------------------------
        // Clean file name
        // --------------------------------------------------------

        String cleanFileName =
                originalFilename.replaceAll(
                        "[^a-zA-Z0-9._-]",
                        "_"
                );

        // --------------------------------------------------------
        // ALWAYS upload into the fanverse folder
        // --------------------------------------------------------

        String key =
                "fanverse/" + cleanFileName;

        // --------------------------------------------------------
        // Detect content type
        // --------------------------------------------------------

        String contentType =
                file.getContentType();

        if (contentType == null
                || contentType.trim().isEmpty()
                || contentType.equals(
                        "application/octet-stream")) {

            contentType =
                    detectContentType(cleanFileName);
        }

        // --------------------------------------------------------
        // Create S3 PUT request
        // --------------------------------------------------------

        PutObjectRequest putObjectRequest =
                PutObjectRequest.builder()
                        .bucket(bucketName)
                        .key(key)
                        .contentType(contentType)
                        .build();

        // --------------------------------------------------------
        // Upload file to S3
        // --------------------------------------------------------

        s3Client.putObject(
                putObjectRequest,
                RequestBody.fromInputStream(
                        file.getInputStream(),
                        file.getSize()
                )
        );

        // --------------------------------------------------------
        // Generate presigned URL
        // --------------------------------------------------------

        String presignedUrl = null;

        try {

            presignedUrl =
                    generatePresignedUrl(
                            key,
                            Duration.ofHours(2)
                    );

        } catch (Exception e) {

            log.warn(
                    "Failed generating presigned URL for new upload {}: {}",
                    key,
                    e.getMessage()
            );
        }

        log.info(
                "File uploaded successfully to S3: {} (Size: {} bytes)",
                key,
                file.getSize()
        );

        // --------------------------------------------------------
        // Return upload response
        // --------------------------------------------------------

        return new UploadResponseDto(
                key,
                cleanFileName,
                file.getSize(),
                S3ObjectDto.formatBytes(
                        file.getSize()
                ),
                contentType,
                presignedUrl,
                "File uploaded successfully to S3 bucket '"
                        + bucketName
                        + "'"
        );
    }

    // ============================================================
    // DOWNLOAD FILE
    // ============================================================

    public ResponseEntity<Resource> downloadFile(
            String key,
            boolean asAttachment) {

        String bucketName =
                awsS3Config.getBucketName();

        validateBucketConfigured();

        GetObjectRequest getObjectRequest =
                GetObjectRequest.builder()
                        .bucket(bucketName)
                        .key(key)
                        .build();

        ResponseBytes<GetObjectResponse> objectBytes =
                s3Client.getObjectAsBytes(
                        getObjectRequest
                );

        ByteArrayResource resource =
                new ByteArrayResource(
                        objectBytes.asByteArray()
                );

        String fileName =
                key.contains("/")
                        ? key.substring(
                                key.lastIndexOf('/') + 1
                        )
                        : key;

        String contentType =
                objectBytes.response().contentType();

        if (contentType == null
                || contentType.isEmpty()) {

            contentType =
                    detectContentType(fileName);
        }

        String dispositionType =
                asAttachment
                        ? "attachment"
                        : "inline";

        return ResponseEntity.ok()
                .contentType(
                        MediaType.parseMediaType(
                                contentType
                        )
                )
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        dispositionType
                                + "; filename=\""
                                + fileName
                                + "\""
                )
                .contentLength(
                        objectBytes.asByteArray().length
                )
                .body(resource);
    }

    // ============================================================
    // PRESIGNED URL DTO
    // ============================================================

    public PresignedUrlResponseDto getPresignedUrlDto(
            String key,
            long durationMinutes) {

        if (durationMinutes <= 0) {

            durationMinutes = 60;
        }

        String url =
                generatePresignedUrl(
                        key,
                        Duration.ofMinutes(
                                durationMinutes
                        )
                );

        return new PresignedUrlResponseDto(
                key,
                url,
                durationMinutes * 60
        );
    }

    // ============================================================
    // GENERATE PRESIGNED URL
    // ============================================================

    public String generatePresignedUrl(
            String key,
            Duration duration) {

        String bucketName =
                awsS3Config.getBucketName();

        validateBucketConfigured();

        GetObjectRequest getObjectRequest =
                GetObjectRequest.builder()
                        .bucket(bucketName)
                        .key(key)
                        .build();

        GetObjectPresignRequest presignRequest =
                GetObjectPresignRequest.builder()
                        .signatureDuration(duration)
                        .getObjectRequest(
                                getObjectRequest
                        )
                        .build();

        PresignedGetObjectRequest
                presignedGetObjectRequest =
                s3Presigner.presignGetObject(
                        presignRequest
                );

        return presignedGetObjectRequest
                .url()
                .toString();
    }

    // ============================================================
    // DELETE OBJECT
    // ============================================================

    public void deleteObject(String key) {

        String bucketName =
                awsS3Config.getBucketName();

        validateBucketConfigured();

        DeleteObjectRequest deleteObjectRequest =
                DeleteObjectRequest.builder()
                        .bucket(bucketName)
                        .key(key)
                        .build();

        s3Client.deleteObject(
                deleteObjectRequest
        );

        log.info(
                "Deleted S3 object: {}",
                key
        );
    }

    // ============================================================
    // VALIDATE BUCKET
    // ============================================================

    private void validateBucketConfigured() {

        String bucketName =
                awsS3Config.getBucketName();

        if (bucketName == null
                || bucketName.trim().isEmpty()
                || bucketName.contains("YOUR_")) {

            throw new IllegalStateException(
                    "AWS S3 Bucket Name is not configured! "
                            + "Please configure AWS_S3_BUCKET_NAME "
                            + "in .env file."
            );
        }
    }

    // ============================================================
    // DETECT CONTENT TYPE
    // ============================================================

    private String detectContentType(
            String fileName) {

        String mimeType =
                URLConnection.guessContentTypeFromName(
                        fileName
                );

        if (mimeType != null) {

            return mimeType;
        }

        String lower =
                fileName.toLowerCase();

        if (lower.endsWith(".png")) {
            return "image/png";
        }

        if (lower.endsWith(".jpg")
                || lower.endsWith(".jpeg")) {

            return "image/jpeg";
        }

        if (lower.endsWith(".gif")) {
            return "image/gif";
        }

        if (lower.endsWith(".webp")) {
            return "image/webp";
        }

        if (lower.endsWith(".svg")) {
            return "image/svg+xml";
        }

        if (lower.endsWith(".pdf")) {
            return "application/pdf";
        }

        if (lower.endsWith(".json")) {
            return "application/json";
        }

        if (lower.endsWith(".txt")
                || lower.endsWith(".log")
                || lower.endsWith(".md")) {

            return "text/plain";
        }

        if (lower.endsWith(".csv")) {
            return "text/csv";
        }

        if (lower.endsWith(".zip")) {
            return "application/zip";
        }

        if (lower.endsWith(".mp4")) {
            return "video/mp4";
        }

        if (lower.endsWith(".mp3")) {
            return "audio/mpeg";
        }

        return "application/octet-stream";
    }
}