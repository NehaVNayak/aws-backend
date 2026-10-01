package com.example.s3microservice.controller;

import com.example.s3microservice.dto.BucketStatusDto;
import com.example.s3microservice.dto.PresignedUrlResponseDto;
import com.example.s3microservice.dto.S3ObjectDto;
import com.example.s3microservice.dto.UploadResponseDto;
import com.example.s3microservice.service.S3Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/s3")
public class S3Controller {

    private static final Logger log = LoggerFactory.getLogger(S3Controller.class);
    private final S3Service s3Service;

    public S3Controller(S3Service s3Service) {
        this.s3Service = s3Service;
    }

    @GetMapping("/status")
    public ResponseEntity<BucketStatusDto> getStatus() {
        return ResponseEntity.ok(s3Service.getBucketStatus());
    }

    @GetMapping("/files")
    public ResponseEntity<?> listFiles(@RequestParam(value = "prefix", required = false) String prefix) {
        try {
            List<S3ObjectDto> objects = s3Service.listObjects(prefix);
            return ResponseEntity.ok(objects);
        } catch (Exception e) {
            log.error("Error listing S3 files: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage(), "items", Collections.emptyList()));
        }
    }

    @PostMapping(value = "/upload", consumes = {"multipart/form-data"})
    public ResponseEntity<?> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "prefix", required = false) String prefix) {
        try {
            UploadResponseDto response = s3Service.uploadFile(file, prefix);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error uploading file to S3: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Upload failed: " + e.getMessage()));
        }
    }

    @GetMapping("/download")
    public ResponseEntity<Resource> downloadFile(@RequestParam("key") String key) {
        return s3Service.downloadFile(key, true);
    }

    @GetMapping("/view")
    public ResponseEntity<Resource> viewFile(@RequestParam("key") String key) {
        return s3Service.downloadFile(key, false);
    }

    @GetMapping("/presigned-url")
    public ResponseEntity<PresignedUrlResponseDto> getPresignedUrl(
            @RequestParam("key") String key,
            @RequestParam(value = "durationMinutes", defaultValue = "60") long durationMinutes) {
        return ResponseEntity.ok(s3Service.getPresignedUrlDto(key, durationMinutes));
    }

    @DeleteMapping("/files")
    public ResponseEntity<?> deleteFile(@RequestParam("key") String key) {
        try {
            s3Service.deleteObject(key);
            return ResponseEntity.ok(Map.of("message", "File deleted successfully", "key", key));
        } catch (Exception e) {
            log.error("Error deleting file {}: {}", key, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Delete failed: " + e.getMessage()));
        }
    }
}
