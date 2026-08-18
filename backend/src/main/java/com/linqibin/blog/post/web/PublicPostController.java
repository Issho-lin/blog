package com.linqibin.blog.post.web;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.linqibin.blog.common.api.PageResponse;
import com.linqibin.blog.post.application.PostService;
import com.linqibin.blog.post.domain.Post;
import com.linqibin.blog.post.util.ReadingTimeEstimator;
import com.linqibin.blog.post.util.SummaryGenerator;
import com.linqibin.blog.taxonomy.application.CategoryService;
import com.linqibin.blog.taxonomy.application.TagService;
import com.linqibin.blog.taxonomy.domain.Category;
import com.linqibin.blog.taxonomy.domain.Tag;

// 公开文章接口：负责首页列表、文章详情、搜索和归档所需的只读查询，供前台页面和 SSR 使用。
@RestController
@RequestMapping("/api/public/posts")
public class PublicPostController {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 100;

    private final PostService postService;
    private final CategoryService categoryService;
    private final TagService tagService;
    private final PostDetailAssembler postDetailAssembler;

    public PublicPostController(PostService postService,
                                CategoryService categoryService, TagService tagService,
                                PostDetailAssembler postDetailAssembler) {
        this.postService = postService;
        this.categoryService = categoryService;
        this.tagService = tagService;
        this.postDetailAssembler = postDetailAssembler;
    }

    // 公开文章列表：分页返回已发布文章，支持按分类和标签筛选，列表项不含正文。
    @GetMapping
    public PageResponse<PublicPostSummary> listPublishedPosts(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) UUID tagId
    ) {
        int normalizedPage = Math.max(page, DEFAULT_PAGE);
        int normalizedPageSize = (pageSize == null || pageSize < 1)
                ? DEFAULT_PAGE_SIZE
                : Math.min(pageSize, MAX_PAGE_SIZE);

        List<Post> posts;
        long total;

        if (categoryId != null) {
            posts = postService.listPublishedPostsByCategory(categoryId, normalizedPage, normalizedPageSize);
            total = postService.countPublishedPostsByCategory(categoryId);
        } else if (tagId != null) {
            posts = postService.listPublishedPostsByTag(tagId, normalizedPage, normalizedPageSize);
            total = postService.countPublishedPostsByTag(tagId);
        } else {
            posts = postService.listPublishedPosts(normalizedPage, normalizedPageSize);
            total = postService.countPublishedPosts();
        }

        List<PublicPostSummary> items = posts.stream()
                .map(this::toSummary)
                .toList();

        return PageResponse.of(items, normalizedPage, normalizedPageSize, total);
    }

    // 公开搜索接口：按关键词搜索已发布文章（匹配标题和正文），分页返回。
    // 精确路径 /search 优先于 /{slug} 匹配，不会把 "search" 当作 slug。
    @GetMapping("/search")
    public PageResponse<PublicPostSummary> searchPosts(
            @RequestParam(name = "q") String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(required = false) Integer pageSize
    ) {
        int normalizedPage = Math.max(page, DEFAULT_PAGE);
        int normalizedPageSize = (pageSize == null || pageSize < 1)
                ? DEFAULT_PAGE_SIZE
                : Math.min(pageSize, MAX_PAGE_SIZE);

        List<Post> posts = postService.searchPublishedPosts(keyword, normalizedPage, normalizedPageSize);
        long total = postService.countSearchPublishedPosts(keyword);

        List<PublicPostSummary> items = posts.stream()
                .map(this::toSummary)
                .toList();

        return PageResponse.of(items, normalizedPage, normalizedPageSize, total);
    }

    // 公开文章详情：返回渲染后的 HTML、目录、摘要、阅读时长和 SEO 元信息。
    // 同时递增阅读数。
    @GetMapping("/{slug}")
    public PublicPostDetailResponse getPostBySlug(@PathVariable String slug) {
        Post post = postService.getPublishedPostBySlug(slug);
        postService.incrementViewCount(post.id());
        return postDetailAssembler.toPublicDetail(
                post,
                post.viewCount() + 1,
                postService.findAdjacentPublished(post.id())
        );
    }

    // 归档列表：按年月分组返回已发布文章的标题和 slug，供归档页使用。
    // 精确路径 /archives 优先于 /{slug} 匹配，不会把 "archives" 当作 slug。
    @GetMapping("/archives")
    public List<ArchiveGroup> getArchives() {
        List<Post> publishedPosts = postService.findAllPublishedPosts();

        // 按年月分组，使用 LinkedHashMap 保持遍历顺序（已按发布时间倒序）。
        Map<String, List<ArchiveItem>> grouped = new LinkedHashMap<>();
        for (Post post : publishedPosts) {
            String key = buildArchiveKey(post.publishedAt());
            grouped.computeIfAbsent(key, k -> new ArrayList<>())
                    .add(new ArchiveItem(post.title(), post.slug(), post.publishedAt()));
        }

        // 将分组转换为 ArchiveGroup 列表。
        List<ArchiveGroup> groups = new ArrayList<>();
        for (Map.Entry<String, List<ArchiveItem>> entry : grouped.entrySet()) {
            String[] parts = entry.getKey().split("-");
            int year = Integer.parseInt(parts[0]);
            int month = Integer.parseInt(parts[1]);
            groups.add(new ArchiveGroup(year, month, entry.getValue()));
        }

        return groups;
    }

    // 将领域对象转换为公开列表项，生成摘要、估算阅读时长，并填充分类和标签名称。
    private PublicPostSummary toSummary(Post post) {
        String summary = resolveSummary(post);
        String plainText = SummaryGenerator.stripMarkdown(post.markdownContent());
        int readingTime = ReadingTimeEstimator.estimate(plainText);
        Category category = resolveCategory(post.categoryId());
        List<Tag> tags = resolveTags(post.tagIds());
        return new PublicPostSummary(
                post.id(),
                post.title(),
                post.slug(),
                summary,
                post.coverUrl(),
                post.publishedAt(),
                readingTime,
                post.viewCount(),
                category == null ? null : category.name(),
                category == null ? null : category.slug(),
                tags.stream().map(Tag::name).toList(),
                tags.stream().map(Tag::slug).toList()
        );
    }

    private Category resolveCategory(UUID categoryId) {
        if (categoryId == null) {
            return null;
        }
        return categoryService.getCategory(categoryId);
    }

    private List<Tag> resolveTags(List<UUID> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            return List.of();
        }
        return tagIds.stream().map(tagService::getTag).toList();
    }

    // 作者填写了摘要就用自定义摘要；未填写时从正文截取。
    private static String resolveSummary(Post post) {
        if (post.excerpt() != null && !post.excerpt().isBlank()) {
            return post.excerpt();
        }
        return SummaryGenerator.generate(post.markdownContent());
    }

    // 根据发布时间生成年月分组键，格式为 "yyyy-MM"。
    private static String buildArchiveKey(Instant publishedAt) {
        ZonedDateTime zdt = publishedAt.atZone(ZoneId.systemDefault());
        return zdt.getYear() + "-" + String.format("%02d", zdt.getMonthValue());
    }
}
