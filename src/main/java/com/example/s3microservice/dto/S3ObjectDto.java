package com.example.s3microservice.dto;

import java.time.Instant;

public class S3ObjectDto {
    private String key;
    private String fileName;
    private long size;
    private String formattedSize;
    private Instant lastModified;
    private String eTag;
    private String storageClass;
    private String presignedUrl;
    private String contentType;

    public S3ObjectDto() {}

    public S3ObjectDto(String key, String fileName, long size, String formattedSize,
                       Instant lastModified, String eTag, String storageClass,
                       String presignedUrl, String contentType) {
        this.key = key;
        this.fileName = fileName;
        this.size = size;
        this.formattedSize = formattedSize;
        this.lastModified = lastModified;
        this.eTag = eTag;
        this.storageClass = storageClass;
        this.presignedUrl = presignedUrl;
        this.contentType = contentType;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public String getFormattedSize() {
        return formattedSize;
    }

    public void setFormattedSize(String formattedSize) {
        this.formattedSize = formattedSize;
    }

    public Instant getLastModified() {
        return lastModified;
    }

    public void setLastModified(Instant lastModified) {
        this.lastModified = lastModified;
    }

    public String getETag() {
        return eTag;
    }

    public void setETag(String eTag) {
        this.eTag = eTag;
    }

    public String getStorageClass() {
        return storageClass;
    }

    public void setStorageClass(String storageClass) {
        this.storageClass = storageClass;
    }

    public String getPresignedUrl() {
        return presignedUrl;
    }

    public void setPresignedUrl(String presignedUrl) {
        this.presignedUrl = presignedUrl;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public static String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format("%.2f %sB", bytes / Math.pow(1024, exp), pre);
    }
}
