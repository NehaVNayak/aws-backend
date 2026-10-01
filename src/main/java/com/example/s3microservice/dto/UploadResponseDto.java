package com.example.s3microservice.dto;

public class UploadResponseDto {
    private String key;
    private String fileName;
    private long size;
    private String formattedSize;
    private String contentType;
    private String presignedUrl;
    private String message;

    public UploadResponseDto() {}

    public UploadResponseDto(String key, String fileName, long size, String formattedSize,
                             String contentType, String presignedUrl, String message) {
        this.key = key;
        this.fileName = fileName;
        this.size = size;
        this.formattedSize = formattedSize;
        this.contentType = contentType;
        this.presignedUrl = presignedUrl;
        this.message = message;
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

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getPresignedUrl() {
        return presignedUrl;
    }

    public void setPresignedUrl(String presignedUrl) {
        this.presignedUrl = presignedUrl;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
