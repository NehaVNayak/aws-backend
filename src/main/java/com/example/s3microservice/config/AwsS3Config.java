package com.example.s3microservice.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
public class AwsS3Config {

    private static final Logger log = LoggerFactory.getLogger(AwsS3Config.class);

    @Value("${AWS_ACCESS_KEY_ID:${aws.accessKeyId:}}")
    private String accessKeyId;

    @Value("${AWS_SECRET_ACCESS_KEY:${aws.secretAccessKey:}}")
    private String secretAccessKey;

    @Value("${AWS_SESSION_TOKEN:${aws.sessionToken:}}")
    private String sessionToken;

    @Value("${AWS_REGION:${aws.region:us-east-1}}")
    private String region;

    @Value("${AWS_S3_BUCKET_NAME:${aws.s3.bucket:}}")
    private String bucketName;

    public String getBucketName() {
        return bucketName;
    }

    public String getRegion() {
        return region;
    }

    public boolean hasExplicitCredentials() {
        return isConfigured(accessKeyId) && isConfigured(secretAccessKey);
    }

    private boolean isConfigured(String val) {
        return val != null && !val.trim().isEmpty() && !val.contains("YOUR_") && !val.equals("default");
    }

    @Bean
    public AwsCredentialsProvider awsCredentialsProvider() {
        if (hasExplicitCredentials()) {
            log.info("Configuring AWS S3 client using explicit IAM credentials from environment/.env (Key: ...{})",
                    accessKeyId.length() > 4 ? accessKeyId.substring(accessKeyId.length() - 4) : "****");
            if (isConfigured(sessionToken)) {
                return StaticCredentialsProvider.create(
                        AwsSessionCredentials.create(accessKeyId.trim(), secretAccessKey.trim(), sessionToken.trim())
                );
            }
            return StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(accessKeyId.trim(), secretAccessKey.trim())
            );
        } else {
            log.info("No explicit IAM credentials provided in .env - falling back to AWS Default Credentials Provider chain (EC2 IAM Role, AWS CLI, etc.)");
            return DefaultCredentialsProvider.create();
        }
    }

    @Bean
    public S3Client s3Client(AwsCredentialsProvider credentialsProvider) {
        Region awsRegion = parseRegion(region);
        S3ClientBuilder builder = S3Client.builder()
                .region(awsRegion)
                .credentialsProvider(credentialsProvider);

        return builder.build();
    }

    @Bean
    public S3Presigner s3Presigner(AwsCredentialsProvider credentialsProvider) {
        Region awsRegion = parseRegion(region);
        return S3Presigner.builder()
                .region(awsRegion)
                .credentialsProvider(credentialsProvider)
                .build();
    }

    private Region parseRegion(String regionName) {
        if (regionName == null || regionName.trim().isEmpty()) {
            return Region.US_EAST_1;
        }
        try {
            return Region.of(regionName.trim().toLowerCase());
        } catch (Exception e) {
            log.warn("Invalid AWS Region '{}', defaulting to us-east-1", regionName);
            return Region.US_EAST_1;
        }
    }
}
