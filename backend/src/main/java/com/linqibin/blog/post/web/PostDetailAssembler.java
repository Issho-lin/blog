package com.linqibin.blog.post.web;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.linqibin.blog.markdown.MarkdownRenderResult;
import com.linqibin.blog.markdown.MarkdownService;
import com.linqibin.blog.post.application.AdjacentPublishedPosts;
import com.linqibin.blog.post.domain.Post;
import com.linqibin.blog.post.util.ReadingTimeEstimator;
import com.linqibin.blog.post.util.SummaryGenerator;
import com.linqibin.blog.taxonomy.application.CategoryService;
import com.linqibin.blog.taxonomy.application.TagService;
import com.linqibin.blog.taxonomy.domain.Category;
import com.linqibin.blog.taxonomy.domain.Tag;

@Component
public class PostDetailAssembler {

    private final MarkdownService markdownService;
    private final CategoryService categoryService;
    private final TagService tagService;

    public PostDetailAssembler(
            MarkdownService markdownService,
            CategoryService categoryService,
            TagService tagService
    ) {
        this.markdownService = markdownService;
        this.categoryService = categoryService;
        this.tagService = tagService;
    }

    public PublicPostDetailResponse toPublicDetail(Post post, long viewCount) {
        return toPublicDetail(post, viewCount, new AdjacentPublishedPosts(null, null));
    }

    public PublicPostDetailResponse toPublicDetail(Post post, long viewCount, AdjacentPublishedPosts adjacent) {
        MarkdownRenderResult renderResult = markdownService.renderWithTableOfContents(post.markdownContent());
        String summary = resolveSummary(post);
        String plainText = SummaryGenerator.stripMarkdown(post.markdownContent());
        int readingTime = ReadingTimeEstimator.estimate(plainText);
        Category category = resolveCategory(post.categoryId());
        List<Tag> tags = resolveTags(post.tagIds());
        AdjacentPublishedPosts neighbors = adjacent == null
                ? new AdjacentPublishedPosts(null, null)
                : adjacent;

        return new PublicPostDetailResponse(
                post.id(),
                post.title(),
                post.slug(),
                renderResult.html(),
                post.markdownContent(),
                summary,
                post.coverUrl(),
                renderResult.tableOfContents(),
                readingTime,
                viewCount,
                post.publishedAt(),
                post.updatedAt(),
                category == null ? null : category.name(),
                category == null ? null : category.slug(),
                tags.stream().map(Tag::name).toList(),
                tags.stream().map(Tag::slug).toList(),
                post.seoTitle() != null ? post.seoTitle() : post.title(),
                post.seoDescription() != null ? post.seoDescription() : summary,
                "/posts/" + post.slug(),
                toNeighbor(neighbors.previous()),
                toNeighbor(neighbors.next())
        );
    }

    public AdminPostPreviewResponse toAdminPreview(Post post) {
        return toAdminPreview(post, new AdjacentPublishedPosts(null, null));
    }

    public AdminPostPreviewResponse toAdminPreview(Post post, AdjacentPublishedPosts adjacent) {
        return AdminPostPreviewResponse.from(post.status(), toPublicDetail(post, post.viewCount(), adjacent));
    }

    private static PublicPostNeighbor toNeighbor(Post post) {
        if (post == null) {
            return null;
        }
        return new PublicPostNeighbor(post.title(), post.slug());
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

    private static String resolveSummary(Post post) {
        if (post.excerpt() != null && !post.excerpt().isBlank()) {
            return post.excerpt();
        }
        return SummaryGenerator.generate(post.markdownContent());
    }
}
