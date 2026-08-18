package com.linqibin.blog.post.application;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.linqibin.blog.markdown.exporter.FrontMatterExporter;
import com.linqibin.blog.markdown.parser.FrontMatter;
import com.linqibin.blog.markdown.parser.FrontMatterParser;
import com.linqibin.blog.media.application.MediaService;
import com.linqibin.blog.media.domain.MediaFile;
import com.linqibin.blog.media.exception.InvalidFileException;
import com.linqibin.blog.post.domain.Post;
import com.linqibin.blog.post.domain.PostStatus;
import com.linqibin.blog.post.domain.SlugGenerator;
import com.linqibin.blog.taxonomy.application.CategoryService;
import com.linqibin.blog.taxonomy.application.TagService;
import com.linqibin.blog.taxonomy.domain.Category;
import com.linqibin.blog.taxonomy.domain.Tag;

// 文章导入导出服务：串联 Front Matter 解析、分类标签回填和草稿创建。
// 导入永远不会因为 Front Matter 中声明了 status: published 而直接发布。
public class PostImportExportService {

    private static final long DEFAULT_MAX_IMPORT_SIZE = 2 * 1024 * 1024; // 2 MB
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".md", ".markdown");

    private final PostService postService;
    private final CategoryService categoryService;
    private final TagService tagService;
    private final FrontMatterParser frontMatterParser;
    private final FrontMatterExporter frontMatterExporter;
    private final SlugGenerator slugGenerator;
    private final MediaService mediaService;
    private final long maxImportSize;

    public PostImportExportService(
            PostService postService,
            CategoryService categoryService,
            TagService tagService,
            FrontMatterParser frontMatterParser,
            FrontMatterExporter frontMatterExporter,
            SlugGenerator slugGenerator,
            MediaService mediaService
    ) {
        this(postService, categoryService, tagService, frontMatterParser, frontMatterExporter,
                slugGenerator, mediaService, DEFAULT_MAX_IMPORT_SIZE);
    }

    public PostImportExportService(
            PostService postService,
            CategoryService categoryService,
            TagService tagService,
            FrontMatterParser frontMatterParser,
            FrontMatterExporter frontMatterExporter,
            SlugGenerator slugGenerator,
            MediaService mediaService,
            long maxImportSize
    ) {
        this.postService = Objects.requireNonNull(postService);
        this.categoryService = Objects.requireNonNull(categoryService);
        this.tagService = Objects.requireNonNull(tagService);
        this.frontMatterParser = Objects.requireNonNull(frontMatterParser);
        this.frontMatterExporter = Objects.requireNonNull(frontMatterExporter);
        this.slugGenerator = Objects.requireNonNull(slugGenerator);
        this.mediaService = Objects.requireNonNull(mediaService);
        this.maxImportSize = maxImportSize;
    }

    public Post importMarkdown(String filename, byte[] content) {
        return importMarkdown(filename, content, List.of(), null, false).post();
    }

    public ImportOutcome importMarkdown(
            String filename,
            byte[] content,
            List<ImportedImage> images,
            UUID targetPostId,
            boolean confirmOverwrite
    ) {
        if (filename == null || filename.isBlank()) {
            throw new InvalidFileException("文件名不能为空");
        }
        if (content == null || content.length == 0) {
            throw new InvalidFileException("文件内容不能为空");
        }
        if (targetPostId != null && !confirmOverwrite) {
            throw new InvalidFileException("导入到已有文章必须二次确认");
        }

        validateImportFile(filename, content.length);
        String markdown = new String(content, StandardCharsets.UTF_8);
        var parseResult = frontMatterParser.parse(markdown);
        FrontMatter fm = parseResult.frontMatter();

        String title = fm.title();
        if (title == null || title.isBlank()) {
            title = stripExtension(filename);
        }

        String body = parseResult.body() != null ? parseResult.body() : "";
        List<String> warnings = new ArrayList<>();
        Map<String, String> uploaded = uploadCompanionImages(images, warnings);
        List<String> missingImages = new ArrayList<>();
        body = MarkdownRelativeImageRewriter.rewrite(body, uploaded, missingImages);
        for (String ref : missingImages) {
            warnings.add("相对路径图片未随文件上传，预览中将不可用: " + ref);
        }

        String cover = fm.cover();
        if (cover != null && !cover.isBlank() && !MarkdownRelativeImageRewriter.isRemoteOrAbsolute(cover)) {
            String mappedCover = MarkdownRelativeImageRewriter.resolveUploaded(cover, uploaded);
            if (mappedCover != null) {
                cover = mappedCover;
            } else {
                warnings.add("封面图片未随文件上传: " + cover);
            }
        }

        String slug = fm.slug();
        if (slug != null && !slug.isBlank()) {
            slug = slugGenerator.normalizeRequestedSlug(slug);
            if (targetPostId == null) {
                slug = slugGenerator.ensureUnique(slug, postService::existsBySlug);
            }
        }

        UUID categoryId = null;
        if (fm.category() != null && !fm.category().isBlank()) {
            categoryId = resolveCategory(fm.category());
        }

        List<UUID> tagIds = new ArrayList<>();
        for (String tagIdentifier : fm.tags()) {
            UUID tagId = resolveTag(tagIdentifier);
            if (tagId != null) {
                tagIds.add(tagId);
            }
        }

        Post post;
        if (targetPostId == null) {
            post = postService.createDraft(title, body, slug, categoryId, tagIds.isEmpty() ? null : tagIds,
                    fm.description(), cover, fm.seoTitle(), fm.seoDescription());
        } else {
            post = overwriteExisting(targetPostId, title, body, categoryId, tagIds, fm.description(), cover,
                    fm.seoTitle(), fm.seoDescription());
        }
        return new ImportOutcome(post, List.copyOf(warnings));
    }

    // 导出文章为带 Front Matter 的 Markdown 字符串。
    public String exportPost(UUID postId) {
        return exportPostWithFilename(postId).markdown();
    }

    // 导出文章并返回文件名和内容，避免 Controller 解析 Markdown 提取 slug。
    public ExportResult exportPostWithFilename(UUID postId) {
        Post post = postService.getPost(postId);

        String categoryName = null;
        if (post.categoryId() != null) {
            Optional<Category> category = categoryService.findBySlug(post.categoryId().toString());
            if (category.isEmpty()) {
                // categoryId 可能是 UUID，需要遍历查找
                category = categoryService.findAll().stream()
                        .filter(c -> c.id().equals(post.categoryId()))
                        .findFirst();
            }
            categoryName = category.map(Category::name).orElse(null);
        }

        List<String> tagNames = new ArrayList<>();
        if (post.tagIds() != null) {
            for (UUID tagId : post.tagIds()) {
                Optional<Tag> tag = tagService.findAll().stream()
                        .filter(t -> t.id().equals(tagId))
                        .findFirst();
                tag.map(Tag::name).ifPresent(tagNames::add);
            }
        }

        String markdown = frontMatterExporter.export(
                post.title(),
                post.slug(),
                post.excerpt(),
                post.coverUrl(),
                categoryName,
                tagNames,
                post.status().name(),
                post.publishedAt(),
                post.updatedAt(),
                post.seoTitle(),
                post.seoDescription(),
                post.markdownContent()
        );
        return new ExportResult(markdown, generateExportFilename(post.slug()));
    }

    // 导出结果：包含 Markdown 内容和建议的文件名。
    public record ExportResult(String markdown, String filename) {}

    private Post overwriteExisting(
            UUID targetPostId,
            String title,
            String body,
            UUID categoryId,
            List<UUID> tagIds,
            String excerpt,
            String cover,
            String seoTitle,
            String seoDescription
    ) {
        Post target = postService.getPost(targetPostId);
        if (target.status() == PostStatus.TRASHED) {
            throw new InvalidFileException("不能导入到回收站中的文章，请先恢复");
        }
        if (target.status() == PostStatus.PUBLISHED) {
            target = postService.unpublish(targetPostId);
        }
        return postService.updatePost(
                target.id(),
                title,
                body,
                target.slug(),
                categoryId,
                tagIds,
                target.version(),
                excerpt,
                cover,
                seoTitle,
                seoDescription
        );
    }

    private Map<String, String> uploadCompanionImages(List<ImportedImage> images, List<String> warnings) {
        Map<String, String> uploaded = new LinkedHashMap<>();
        if (images == null || images.isEmpty()) {
            return uploaded;
        }
        for (ImportedImage image : images) {
            try {
                String contentType = inferImageContentType(image.originalFilename(), image.contentType());
                MediaFile stored = mediaService.uploadImage(
                        image.originalFilename(),
                        contentType,
                        image.content() == null ? 0 : image.content().length,
                        new ByteArrayInputStream(image.content() == null ? new byte[0] : image.content())
                );
                String filename = image.originalFilename() == null ? stored.originalFilename() : image.originalFilename();
                uploaded.put(MarkdownRelativeImageRewriter.normalizePath(filename), stored.url());
                uploaded.put(MarkdownRelativeImageRewriter.basename(filename), stored.url());
            } catch (RuntimeException ex) {
                warnings.add("配图上传失败「" + image.originalFilename() + "」: " + ex.getMessage());
            }
        }
        return uploaded;
    }

    private static String inferImageContentType(String filename, String provided) {
        if (provided != null && provided.toLowerCase(Locale.ROOT).startsWith("image/")) {
            return provided;
        }
        String lower = filename == null ? "" : filename.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".png")) {
            return "image/png";
        }
        if (lower.endsWith(".gif")) {
            return "image/gif";
        }
        if (lower.endsWith(".webp")) {
            return "image/webp";
        }
        return "image/jpeg";
    }

    // 导出时生成文件名：slug.md，非法字符替换为短横线。
    public String generateExportFilename(String slug) {
        String safeName = slug.replaceAll("[^a-zA-Z0-9._-]", "-");
        return safeName + ".md";
    }

    private void validateImportFile(String filename, long size) {
        if (size > maxImportSize) {
            throw new InvalidFileException("文件大小超过限制: " + size + " bytes, 上限 " + maxImportSize + " bytes");
        }
        String lowerName = filename.toLowerCase();
        boolean validExtension = false;
        for (String ext : ALLOWED_EXTENSIONS) {
            if (lowerName.endsWith(ext)) {
                validExtension = true;
                break;
            }
        }
        if (!validExtension) {
            throw new InvalidFileException("不支持的文件扩展名，只支持 .md 和 .markdown");
        }
    }

    private UUID resolveCategory(String identifier) {
        // 先按 slug 查找，再按名称查找。
        Optional<Category> category = categoryService.findBySlug(identifier);
        if (category.isEmpty()) {
            category = categoryService.findByName(identifier);
        }
        return category.map(Category::id).orElse(null);
    }

    private UUID resolveTag(String identifier) {
        // 先按 slug 查找，再按名称查找。
        Optional<Tag> tag = tagService.findBySlug(identifier);
        if (tag.isEmpty()) {
            tag = tagService.findByName(identifier);
        }
        return tag.map(Tag::id).orElse(null);
    }

    private String stripExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        return dotIndex > 0 ? filename.substring(0, dotIndex) : filename;
    }
}
