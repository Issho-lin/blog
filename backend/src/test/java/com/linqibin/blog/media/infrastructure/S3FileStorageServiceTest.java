package com.linqibin.blog.media.infrastructure;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketResponse;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import com.linqibin.blog.media.exception.FileStorageException;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class S3FileStorageServiceTest {

    @Mock
    private S3Client s3Client;

    @BeforeEach
    void bucketExists() {
        lenient().when(s3Client.headBucket(any(HeadBucketRequest.class)))
                .thenReturn(HeadBucketResponse.builder().build());
    }

    @Test
    void storePutsObjectAndReturnsPublicUrl() {
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());
        S3FileStorageService storage = new S3FileStorageService(s3Client, "blog-media", "/uploads/");

        String url = storage.store("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa.png", new ByteArrayInputStream("img".getBytes(StandardCharsets.UTF_8)));

        assertEquals("/uploads/aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa.png", url);
        ArgumentCaptor<PutObjectRequest> captor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(captor.capture(), any(RequestBody.class));
        assertEquals("blog-media", captor.getValue().bucket());
        assertEquals("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa.png", captor.getValue().key());
        assertEquals("image/png", captor.getValue().contentType());
        verify(s3Client, never()).createBucket(any(CreateBucketRequest.class));
    }

    @Test
    void deleteRemovesObject() {
        S3FileStorageService storage = new S3FileStorageService(s3Client, "blog-media", "/uploads/");
        storage.delete("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa.jpg");

        ArgumentCaptor<DeleteObjectRequest> captor = ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(s3Client).deleteObject(captor.capture());
        assertEquals("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa.jpg", captor.getValue().key());
    }

    @Test
    void readReturnsBytes() {
        byte[] payload = {1, 2, 3};
        when(s3Client.getObjectAsBytes(any(GetObjectRequest.class)))
                .thenReturn(ResponseBytes.fromByteArray(GetObjectResponse.builder().build(), payload));
        S3FileStorageService storage = new S3FileStorageService(s3Client, "blog-media", "/uploads/");

        assertArrayEquals(payload, storage.read("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa.webp"));
        assertEquals("image/webp", storage.contentTypeOf("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa.webp"));
    }

    @Test
    void readMissingKeyThrowsFileStorageException() {
        when(s3Client.getObjectAsBytes(any(GetObjectRequest.class)))
                .thenThrow(NoSuchKeyException.builder().message("missing").build());
        S3FileStorageService storage = new S3FileStorageService(s3Client, "blog-media", "/uploads/");

        assertThrows(FileStorageException.class, () -> storage.read("bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb.gif"));
    }

    @Test
    void createsBucketWhenMissing() {
        when(s3Client.headBucket(any(HeadBucketRequest.class)))
                .thenThrow(NoSuchBucketException.builder().message("gone").build());

        new S3FileStorageService(s3Client, "blog-media", "/uploads/");

        ArgumentCaptor<CreateBucketRequest> captor = ArgumentCaptor.forClass(CreateBucketRequest.class);
        verify(s3Client).createBucket(captor.capture());
        assertEquals("blog-media", captor.getValue().bucket());
    }
}
