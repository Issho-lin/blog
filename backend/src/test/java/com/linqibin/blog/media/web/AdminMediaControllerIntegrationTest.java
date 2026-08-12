package com.linqibin.blog.media.web;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.linqibin.blog.common.request.RequestIdUtils;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class AdminMediaControllerIntegrationTest {

    private static final Path UPLOAD_DIR = createUploadDir();

    @DynamicPropertySource
    static void mediaProperties(DynamicPropertyRegistry registry) {
        registry.add("blog.media.upload-dir", () -> UPLOAD_DIR.toString());
        registry.add("blog.media.max-image-size", () -> "1024");
    }

    @Autowired
    private MockMvc mockMvc;

    @Test
    void uploadValidPngReturnsCreatedWithUrl() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "photo.png",
                "image/png",
                new byte[100]
        );

        mockMvc.perform(multipart("/api/admin/media/images").file(file)
                        .header(RequestIdUtils.REQUEST_ID_HEADER, "media-upload-request-id"))
                .andExpect(status().isCreated())
                .andExpect(header().string(RequestIdUtils.REQUEST_ID_HEADER, "media-upload-request-id"))
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.url").value(startsWith("/uploads/")))
                .andExpect(jsonPath("$.data.originalFilename").value("photo.png"))
                .andExpect(jsonPath("$.data.contentType").value("image/png"))
                .andExpect(jsonPath("$.data.size").value(100));
    }

    @Test
    void uploadRejectsUnsupportedContentType() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "notes.txt",
                "text/plain",
                "hello".getBytes()
        );

        mockMvc.perform(multipart("/api/admin/media/images").file(file)
                        .header(RequestIdUtils.REQUEST_ID_HEADER, "media-invalid-type-request-id"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_FILE"))
                .andExpect(jsonPath("$.requestId").value("media-invalid-type-request-id"));
    }

    @Test
    void uploadRejectsOversizedFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "big.png",
                "image/png",
                new byte[2048]
        );

        mockMvc.perform(multipart("/api/admin/media/images").file(file)
                        .header(RequestIdUtils.REQUEST_ID_HEADER, "media-oversized-request-id"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_FILE"))
                .andExpect(jsonPath("$.requestId").value("media-oversized-request-id"));
    }

    private static Path createUploadDir() {
        try {
            return Files.createTempDirectory("blog-media-test-");
        } catch (IOException exception) {
            throw new IllegalStateException("无法创建媒体上传测试目录", exception);
        }
    }
}
