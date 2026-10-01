package com.example.s3microservice.dto;

public class PresignedUrlResponseDto {
    private String key;
    private String url;
    private long expiresInSeconds;

    public PresignedUrlResponseDto() {}

    public PresignedUrlResponseDto(String key, String url, long expiresInSeconds) {
        this.key = key;
        this.url = url;
        this.expiresInSeconds = expiresInSeconds;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public long getExpiresInSeconds() {
        return expiresInSeconds;
    }

    public void setExpiresInSeconds(long expiresInSeconds) {
        this.expiresInSeconds = expiresInSeconds;
    }
}
