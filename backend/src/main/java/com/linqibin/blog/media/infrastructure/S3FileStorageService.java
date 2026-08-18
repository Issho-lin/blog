package com.linqibin.blog.media.infrastructure;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import com.linqibin.blog.media.exception.FileStorageException;

// S3 兼容存储（MinIO / AWS S3）：对外仍返回 /uploads/{filename}，由后端代读。
public class S3FileStorageService implements FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(S3FileStorageService.class);

    private final S3Client s3Client;
    private final String bucket;
    private final String urlPrefix;

    public S3FileStorageService(S3Client s3Client, String bucket, String urlPrefix) {
        this.s3Client = Objects.requireNonNull(s3Client);
        this.bucket = Objects.requireNonNull(bucket);
        this.urlPrefix = normalizeUrlPrefix(urlPrefix);
        ensureBucket();
    }

    @Override
    public String store(String storedFilename, InputStream content) {
        try {
            byte[] bytes = content.readAllBytes();
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(storedFilename)
                            .contentType(contentTypeOf(storedFilename))
                            .contentLength((long) bytes.length)
                            .build(),
                    RequestBody.fromBytes(bytes)
            );
            return urlPrefix + storedFilename;
        } catch (IOException | SdkException exception) {
            throw new FileStorageException("文件存储失败: " + storedFilename, exception);
        }
    }

    @Override
    public void delete(String storedFilename) {
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(storedFilename)
                    .build());
        } catch (SdkException exception) {
            throw new FileStorageException("文件删除失败: " + storedFilename, exception);
        }
    }

    public byte[] read(String storedFilename) {
        try {
            ResponseBytes<GetObjectResponse> object = s3Client.getObjectAsBytes(
                    GetObjectRequest.builder().bucket(bucket).key(storedFilename).build()
            );
            return object.asByteArray();
        } catch (NoSuchKeyException exception) {
            throw new FileStorageException("文件不存在: " + storedFilename, exception);
        } catch (S3Exception exception) {
            if (exception.statusCode() == 404) {
                throw new FileStorageException("文件不存在: " + storedFilename, exception);
            }
            throw new FileStorageException("文件读取失败: " + storedFilename, exception);
        } catch (SdkException exception) {
            throw new FileStorageException("文件读取失败: " + storedFilename, exception);
        }
    }

    public String contentTypeOf(String storedFilename) {
        String name = storedFilename.toLowerCase(Locale.ROOT);
        if (name.endsWith(".png")) {
            return "image/png";
        }
        if (name.endsWith(".gif")) {
            return "image/gif";
        }
        if (name.endsWith(".webp")) {
            return "image/webp";
        }
        return "image/jpeg";
    }

    private void ensureBucket() {
        try {
            s3Client.headBucket(HeadBucketRequest.builder().bucket(bucket).build());
        } catch (NoSuchBucketException exception) {
            createBucket();
        } catch (S3Exception exception) {
            if (exception.statusCode() == 404) {
                createBucket();
                return;
            }
            throw new FileStorageException("检查存储桶失败: " + bucket, exception);
        }
    }

    private void createBucket() {
        s3Client.createBucket(CreateBucketRequest.builder().bucket(bucket).build());
        log.info("已创建对象存储桶：{}", bucket);
    }

    private static String normalizeUrlPrefix(String prefix) {
        if (prefix == null || prefix.isBlank()) {
            return "/uploads/";
        }
        String normalized = prefix.trim();
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }
        if (!normalized.endsWith("/")) {
            normalized = normalized + "/";
        }
        return normalized;
    }
}
