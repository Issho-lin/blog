package com.linqibin.blog.post.web;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.linqibin.blog.post.application.ImportedImage;
import com.linqibin.blog.post.application.PostImportExportService;

// 管理端文章导入导出接口。
@RestController
@RequestMapping("/api/admin")
public class AdminPostImportExportController {

    private final PostImportExportService importExportService;

    public AdminPostImportExportController(PostImportExportService importExportService) {
        this.importExportService = importExportService;
    }

    @PostMapping("/imports")
    public ResponseEntity<ImportPostResponse> importMarkdown(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "images", required = false) MultipartFile[] images,
            @RequestParam(value = "targetPostId", required = false) UUID targetPostId,
            @RequestParam(value = "confirmOverwrite", defaultValue = "false") boolean confirmOverwrite
    ) throws IOException {
        byte[] content = file.getBytes();
        List<ImportedImage> companionImages = new ArrayList<>();
        if (images != null) {
            for (MultipartFile image : images) {
                if (image == null || image.isEmpty()) {
                    continue;
                }
                companionImages.add(new ImportedImage(
                        image.getOriginalFilename(),
                        image.getContentType(),
                        image.getBytes()
                ));
            }
        }
        var outcome = importExportService.importMarkdown(
                file.getOriginalFilename(),
                content,
                companionImages,
                targetPostId,
                confirmOverwrite
        );
        return ResponseEntity.ok(ImportPostResponse.from(outcome));
    }

    @GetMapping("/posts/{id}/export")
    public ResponseEntity<byte[]> exportPost(@PathVariable UUID id) {
        var result = importExportService.exportPostWithFilename(id);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + result.filename() + "\"")
                .contentType(MediaType.parseMediaType("text/markdown; charset=UTF-8"))
                .body(result.markdown().getBytes(StandardCharsets.UTF_8));
    }
}
