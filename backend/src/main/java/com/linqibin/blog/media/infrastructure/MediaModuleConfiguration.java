package com.linqibin.blog.media.infrastructure;

import java.net.URI;
import java.time.Clock;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import com.linqibin.blog.media.application.MediaService;

@Configuration
public class MediaModuleConfiguration {

    @Bean
    @ConditionalOnProperty(
            prefix = "blog.media",
            name = "storage-type",
            havingValue = "local",
            matchIfMissing = true
    )
    public FileStorageService localFileStorageService(
            @Value("${blog.media.upload-dir:./uploads}") String uploadDir,
            @Value("${blog.media.url-prefix:/uploads/}") String urlPrefix
    ) {
        return new LocalFileStorageService(uploadDir, urlPrefix);
    }

    @Bean(destroyMethod = "close")
    @ConditionalOnProperty(prefix = "blog.media", name = "storage-type", havingValue = "s3")
    public S3Client s3Client(
            @Value("${blog.media.s3.endpoint:http://localhost:9000}") String endpoint,
            @Value("${blog.media.s3.region:us-east-1}") String region,
            @Value("${blog.media.s3.access-key:minio}") String accessKey,
            @Value("${blog.media.s3.secret-key:minioadmin}") String secretKey
    ) {
        return S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)
                ))
                .forcePathStyle(true)
                .build();
    }

    @Bean
    @ConditionalOnProperty(prefix = "blog.media", name = "storage-type", havingValue = "s3")
    public S3FileStorageService s3FileStorageService(
            S3Client s3Client,
            @Value("${blog.media.s3.bucket:blog-media}") String bucket,
            @Value("${blog.media.url-prefix:/uploads/}") String urlPrefix
    ) {
        return new S3FileStorageService(s3Client, bucket, urlPrefix);
    }

    @Bean
    public MediaService mediaService(
            FileStorageService fileStorageService,
            Clock clock,
            @Value("${blog.media.max-image-size:10485760}") long maxImageSize
    ) {
        return new MediaService(fileStorageService, clock, maxImageSize);
    }
}
