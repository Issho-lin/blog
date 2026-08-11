package com.linqibin.blog.post.web;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
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

import com.linqibin.blog.post.application.PostImportExportService;

// 管理端文章导入导出接口。
@RestController
@RequestMapping("/api/admin")
public class AdminPostImportExportController {

    private final PostImportExportService importExportService;

    public AdminPostImportExportController(PostImportExportService importExportService) {
        this.importExportService = importExportService;
    }

    // 导入 Markdown 文件，创建草稿文章。
    @PostMapping("/imports")
    public ResponseEntity<ImportPostResponse> importMarkdown(@RequestParam("file") MultipartFile file) throws IOException {
        byte[] content = file.getBytes();
        var post = importExportService.importMarkdown(file.getOriginalFilename(), content);
        return ResponseEntity.ok(ImportPostResponse.from(post));
    }

    // 导出文章为 Markdown 文件下载。
    @GetMapping("/posts/{id}/export")
    public ResponseEntity<byte[]> exportPost(@PathVariable UUID id) {
        var result = importExportService.exportPostWithFilename(id);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + result.filename() + "\"")
                .contentType(MediaType.parseMediaType("text/markdown; charset=UTF-8"))
                .body(result.markdown().getBytes(StandardCharsets.UTF_8));
    }
}
