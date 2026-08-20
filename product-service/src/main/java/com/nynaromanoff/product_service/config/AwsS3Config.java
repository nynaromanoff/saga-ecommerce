package com.nynaromanoff.product_service.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;

import java.net.URI;

@Configuration
public class AwsS3Config {
    @Value("${aws.access-key-id}")
    private String accessKey;

    @Value("${aws.secret-access-key}")
    private String secretKey;

    @Value("${aws.s3.region}")
    private String region;

    @Value("${aws.s3.endpoint:#{null}}")
    private String endpoint;

    @Value("${aws.s3.bucket-name}") // Pegamos o nome do bucket configurado
    private String bucketName;

    @Bean
    public S3Client s3Client() {
        AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);

        S3ClientBuilder builder = S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(credentials));

        if (endpoint != null && !endpoint.isBlank()) {
            builder.endpointOverride(URI.create(endpoint));
            builder.forcePathStyle(true);
        }

        S3Client s3Client = builder.build();

        // 🔥 PULO DO GATO: Se for ambiente local, cria o bucket automaticamente se ele não existir
        if (endpoint != null && !endpoint.isBlank()) {
            try {
                s3Client.headBucket(HeadBucketRequest.builder().bucket(bucketName).build());
            } catch (NoSuchBucketException e) {
                s3Client.createBucket(CreateBucketRequest.builder().bucket(bucketName).build());
                System.out.println("🚀 Bucket '" + bucketName + "' criado automaticamente no LocalStack!");
            }
        }

        return s3Client;
    }
}
