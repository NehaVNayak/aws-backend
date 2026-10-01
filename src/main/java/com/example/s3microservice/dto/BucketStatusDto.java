package com.example.s3microservice.dto;

public class BucketStatusDto {
    private boolean configured;
    private boolean connected;
    private String bucketName;
    private String region;
    private long objectCount;
    private long totalSizeBytes;
    private String formattedTotalSize;
    private String authType;
    private String message;

    public BucketStatusDto() {}

    public BucketStatusDto(boolean configured, boolean connected, String bucketName, String region,
                           long objectCount, long totalSizeBytes, String formattedTotalSize,
                           String authType, String message) {
        this.configured = configured;
        this.connected = connected;
        this.bucketName = bucketName;
        this.region = region;
        this.objectCount = objectCount;
        this.totalSizeBytes = totalSizeBytes;
        this.formattedTotalSize = formattedTotalSize;
        this.authType = authType;
        this.message = message;
    }

    public boolean isConfigured() {
        return configured;
    }

    public void setConfigured(boolean configured) {
        this.configured = configured;
    }

    public boolean isConnected() {
        return connected;
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
    }

    public String getBucketName() {
        return bucketName;
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public long getObjectCount() {
        return objectCount;
    }

    public void setObjectCount(long objectCount) {
        this.objectCount = objectCount;
    }

    public long getTotalSizeBytes() {
        return totalSizeBytes;
    }

    public void setTotalSizeBytes(long totalSizeBytes) {
        this.totalSizeBytes = totalSizeBytes;
    }

    public String getFormattedTotalSize() {
        return formattedTotalSize;
    }

    public void setFormattedTotalSize(String formattedTotalSize) {
        this.formattedTotalSize = formattedTotalSize;
    }

    public String getAuthType() {
        return authType;
    }

    public void setAuthType(String authType) {
        this.authType = authType;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
