package com.linqibin.blog.post.application;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.linqibin.blog.markdown.exporter.FrontMatterExporter;
import com.linqibin.blog.markdown.parser.FrontMatter;
import com.linqibin.blog.markdown.parser.FrontMatterParser;
import com.linqibin.blog.media.exception.InvalidFileException;
import com.linqibin.blog.post.domain.Post;
import com.linqibin.blog.post.domain.SlugGenerator;
import com.linqibin.blog.taxonomy.application.CategoryService;
import com.linqibin.blog.taxonomy.application.TagService;
import com.linqibin.blog.taxonomy.domain.Category;
import com.linqibin.blog.taxonomy.domain.Tag;

// 文章导入导出服务：串联 Front Matter 解析、分类标签回填和草稿创建。
// 导入永远创建草稿，不会因为 Front Matter 中声明了 status: published 而直接发布。
public class PostImportExportService {

    private static final long DEFAULT_MAX_IMPORT_SIZE = 2 * 1024 * 1024; // 2 MB
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".md", ".markdown");


    private final PostService postService;
    private final CategoryService categoryService;
    private final TagService tagService;
    private final FrontMatterParser frontMatterParser;
    private final FrontMatterExporter frontMatterExporter;
    private final SlugGenerator slugGenerator;
    private final long maxImportSize;

    public PostImportExportService(
            PostService postService,
            CategoryService categoryService,
            TagService tagService,
            FrontMatterParser frontMatterParser,
            FrontMatterExporter frontMatterExporter,
            SlugGenerator slugGenerator
    ) {
        this(postService, categoryService, tagService, frontMatterParser, frontMatterExporter,
                slugGenerator, DEFAULT_MAX_IMPORT_SIZE);
    }

    public PostImportExportService(
            PostService postService,
            CategoryService categoryService,
            TagService tagService,
            FrontMatterParser frontMatterParser,
            FrontMatterExporter frontMatterExporter,
            SlugGenerator slugGenerator,
            long maxImportSize
    ) {
        this.postService = Objects.requireNonNull(postService);
        this.categoryService = Objects.requireNonNull(categoryService);
        this.tagService = Objects.requireNonNull(tagService);
        this.frontMatterParser = Objects.requireNonNull(frontMatterParser);
        this.frontMatterExporter = Objects.requireNonNull(frontMatterExporter);
        this.slugGenerator = Objects.requireNonNull(slugGenerator);
        this.maxImportSize = maxImportSize;
    }

    // 导入 Markdown 文件内容，创建草稿文章。
    // filename 用于在缺少 title 时回退生成标题。
    public Post importMarkdown(String filename, byte[] content) {
        Objects.requireNonNull(filename, "filename 不能为空");
        Objects.requireNonNull(content, "content 不能为空");

        validateImportFile(filename, content.length);
        String markdown = new String(content, StandardCharsets.UTF_8);
        var parseResult = frontMatterParser.parse(markdown);
        FrontMatter fm = parseResult.frontMatter();

        // 标题优先从 Front Matter 获取，其次用文件名（不含扩展名）。
        String title = fm.title();
        if (title == null || title.isBlank()) {
            title = stripExtension(filename);
        }

        // 正文为空时使用空字符串。
        String body = parseResult.body() != null && !parseResult.body().isBlank()
                ? parseResult.body()
                : "";

        // slug 从 Front Matter 获取；为空时由 PostService 自动生成。
        // 已存在时自动追加数字后缀，保证导入不会因为 slug 冲突而失败。
        String slug = fm.slug();
        if (slug != null && !slug.isBlank()) {
            slug = slugGenerator.normalizeRequestedSlug(slug);
            slug = slugGenerator.ensureUnique(slug, postService::existsBySlug);
        }

        // 分类：先按 slug 查找，再按名称查找，找不到就留空。
        UUID categoryId = null;
        if (fm.category() != null && !fm.category().isBlank()) {
            categoryId = resolveCategory(fm.category());
        }

        // 标签：逐个按 slug 或名称查找，找不到就跳过。
        List<UUID> tagIds = new ArrayList<>();
        for (String tagIdentifier : fm.tags()) {
            UUID tagId = resolveTag(tagIdentifier);
            if (tagId != null) {
                tagIds.add(tagId);
            }
        }

        // 导入永远创建草稿，忽略 Front Matter 中的 status 字段。
        return postService.createDraft(title, body, slug, categoryId, tagIds.isEmpty() ? null : tagIds);
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
                null, // excerpt: 暂不存储摘要字段，导出时为空
                null, // cover: 暂不存储封面字段
                categoryName,
                tagNames,
                post.status().name(),
                post.publishedAt(),
                post.updatedAt(),
                post.markdownContent()
        );
        return new ExportResult(markdown, generateExportFilename(post.slug()));
    }

    // 导出结果：包含 Markdown 内容和建议的文件名。
    public record ExportResult(String markdown, String filename) {}

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
