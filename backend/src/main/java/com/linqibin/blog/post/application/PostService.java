package com.linqibin.blog.post.application;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;

import com.linqibin.blog.ai.application.AiCorpusSync;
import com.linqibin.blog.comment.domain.CommentRepository;
import com.linqibin.blog.media.application.MediaService;
import com.linqibin.blog.post.domain.Post;
import com.linqibin.blog.post.domain.PostRepository;
import com.linqibin.blog.post.domain.PostRevision;
import com.linqibin.blog.post.domain.PostRevisionKind;
import com.linqibin.blog.post.domain.PostStatus;
import com.linqibin.blog.post.domain.SlugGenerator;
import com.linqibin.blog.post.exception.ConcurrentPostModificationException;
import com.linqibin.blog.post.exception.DuplicateSlugException;
import com.linqibin.blog.post.exception.PostNotFoundException;

// 应用层负责把“查找/生成 slug/创建 Post/保存”这些步骤串成完整用例。
public class PostService {

    private static final int DASHBOARD_RECENT_LIMIT = 5;

    // 应用层依赖抽象仓库，具体落库方式由基础设施层决定。
    private final PostRepository postRepository;
    // slug 规则也通过领域服务集中管理，避免散落在各个接口里。
    private final SlugGenerator slugGenerator;
    private final Clock clock;
    private final Supplier<UUID> idSupplier;
    private final MediaService mediaService;
    private final PostRevisionService revisionService;
    private final CommentRepository commentRepository;
    private final AiCorpusSync aiCorpusSync;

    public PostService(PostRepository postRepository, SlugGenerator slugGenerator, Clock clock) {
        this(postRepository, slugGenerator, clock, UUID::randomUUID, null, null, null, null);
    }

    public PostService(
            PostRepository postRepository,
            SlugGenerator slugGenerator,
            Clock clock,
            Supplier<UUID> idSupplier
    ) {
        this(postRepository, slugGenerator, clock, idSupplier, null, null, null, null);
    }

    public PostService(
            PostRepository postRepository,
            SlugGenerator slugGenerator,
            Clock clock,
            Supplier<UUID> idSupplier,
            MediaService mediaService
    ) {
        this(postRepository, slugGenerator, clock, idSupplier, mediaService, null, null, null);
    }

    public PostService(
            PostRepository postRepository,
            SlugGenerator slugGenerator,
            Clock clock,
            Supplier<UUID> idSupplier,
            MediaService mediaService,
            PostRevisionService revisionService,
            CommentRepository commentRepository
    ) {
        this(postRepository, slugGenerator, clock, idSupplier, mediaService, revisionService, commentRepository, null);
    }

    public PostService(
            PostRepository postRepository,
            SlugGenerator slugGenerator,
            Clock clock,
            Supplier<UUID> idSupplier,
            MediaService mediaService,
            PostRevisionService revisionService,
            CommentRepository commentRepository,
            AiCorpusSync aiCorpusSync
    ) {
        this.postRepository = Objects.requireNonNull(postRepository);
        this.slugGenerator = Objects.requireNonNull(slugGenerator);
        this.clock = Objects.requireNonNull(clock);
        this.idSupplier = Objects.requireNonNull(idSupplier);
        this.mediaService = mediaService;
        this.revisionService = revisionService;
        this.commentRepository = commentRepository;
        this.aiCorpusSync = aiCorpusSync;
    }

    public Post createDraft(String title, String markdownContent, String requestedSlug,
                            UUID categoryId, List<UUID> tagIds) {
        return createDraft(title, markdownContent, requestedSlug, categoryId, tagIds, null, null);
    }

    public Post createDraft(String title, String markdownContent, String requestedSlug,
                            UUID categoryId, List<UUID> tagIds, String excerpt, String coverUrl) {
        return createDraft(title, markdownContent, requestedSlug, categoryId, tagIds,
                excerpt, coverUrl, null, null);
    }

    public Post createDraft(String title, String markdownContent, String requestedSlug,
                            UUID categoryId, List<UUID> tagIds, String excerpt, String coverUrl,
                            String seoTitle, String seoDescription) {
        Instant now = Instant.now(clock);
        String slug = resolveSlugForCreate(title, requestedSlug);
        Post post = Post.createDraft(idSupplier.get(), title, slug, defaultContent(markdownContent),
                categoryId, tagIds, now, excerpt, coverUrl, seoTitle, seoDescription);
        Post saved = postRepository.save(post);
        recordRevision(saved, PostRevisionKind.AUTO);
        return saved;
    }

    public Post updatePost(UUID postId, String title, String markdownContent, String requestedSlug,
                            UUID categoryId, List<UUID> tagIds, Long expectedVersion) {
        return updatePost(postId, title, markdownContent, requestedSlug, categoryId, tagIds,
                expectedVersion, null, null);
    }

    public Post updatePost(UUID postId, String title, String markdownContent, String requestedSlug,
                            UUID categoryId, List<UUID> tagIds, Long expectedVersion,
                            String excerpt, String coverUrl) {
        return updatePost(postId, title, markdownContent, requestedSlug, categoryId, tagIds,
                expectedVersion, excerpt, coverUrl, null, null);
    }

    public Post updatePost(UUID postId, String title, String markdownContent, String requestedSlug,
                            UUID categoryId, List<UUID> tagIds, Long expectedVersion,
                            String excerpt, String coverUrl,
                            String seoTitle, String seoDescription) {
        Post currentPost = getPost(postId);
        String resolvedSlug = resolveSlugForUpdate(currentPost, title, requestedSlug);
        String normalizedContent = defaultContent(markdownContent);
        UUID resolvedCategoryId = categoryId != null ? categoryId : currentPost.categoryId();
        List<UUID> resolvedTagIds = tagIds != null ? tagIds : currentPost.tagIds();
        String resolvedExcerpt = excerpt != null ? excerpt : currentPost.excerpt();
        String resolvedCoverUrl = coverUrl != null ? coverUrl : currentPost.coverUrl();
        String resolvedSeoTitle = seoTitle != null ? seoTitle : currentPost.seoTitle();
        String resolvedSeoDescription = seoDescription != null ? seoDescription : currentPost.seoDescription();

        if (contentUnchanged(currentPost, title, resolvedSlug, normalizedContent, resolvedCategoryId, resolvedTagIds,
                resolvedExcerpt, resolvedCoverUrl, resolvedSeoTitle, resolvedSeoDescription)) {
            return currentPost;
        }

        if (expectedVersion != null && expectedVersion != currentPost.version()) {
            throw new ConcurrentPostModificationException(postId, expectedVersion, currentPost.version(), currentPost);
        }

        Post updatedPost = currentPost.update(title, resolvedSlug, normalizedContent,
                resolvedCategoryId, resolvedTagIds, Instant.now(clock), resolvedExcerpt, resolvedCoverUrl,
                resolvedSeoTitle, resolvedSeoDescription);
        Post saved = postRepository.save(updatedPost);
        recordRevision(saved, PostRevisionKind.AUTO);
        syncCorpus(currentPost, saved);
        return saved;
    }

    // 轻量查询保存状态：只返回版本号、更新时间和状态，供前端确认服务端最新版本。
    public Post getSaveStatus(UUID postId) {
        return getPost(postId);
    }

    public Post getPost(UUID postId) {
        // 管理端以 id 为主读取文章，不存在时抛业务异常交给全局异常处理器转换。
        return postRepository.findById(postId).orElseThrow(() -> new PostNotFoundException(postId));
    }

    // 判断 slug 是否已被占用，供导入场景做唯一性检查。
    public boolean existsBySlug(String slug) {
        return postRepository.existsBySlug(slug);
    }

    public Post getPostBySlug(String slug) {
        // 前台读取文章更适合走 slug，因为它比数据库 id 更稳定也更可读。
        return postRepository.findBySlug(slug).orElseThrow(() -> new PostNotFoundException(slug));
    }

    // 公开接口只返回已发布的文章，草稿、下线、回收站文章对访客不可见。
    public Post getPublishedPostBySlug(String slug) {
        return postRepository.findBySlug(slug)
                .filter(Post::isPubliclyReadable)
                .orElseThrow(() -> new PostNotFoundException(slug));
    }

    // 分页返回已发布文章列表，供公开文章列表页使用。
    public List<Post> listPublishedPosts(int page, int pageSize) {
        return postRepository.findPublishedPosts(page, pageSize);
    }

    // 统计已发布文章总数，供分页响应返回 total 字段。
    public long countPublishedPosts() {
        return postRepository.countPublishedPosts();
    }

    // 返回所有已发布文章，按发布时间倒序排列，供归档页使用。
    public List<Post> findAllPublishedPosts() {
        return postRepository.findAll().stream()
                .filter(Post::isPubliclyReadable)
                .sorted(Comparator.comparing(
                                (Post post) -> post.publishedAt(),
                                Comparator.nullsFirst(Comparator.naturalOrder()))
                        .reversed()
                        .thenComparing(Post::id))
                .toList();
    }

    // 上一篇为更早发布，下一篇为更晚发布；当前文不在已发布列表时两边都为空。
    public AdjacentPublishedPosts findAdjacentPublished(UUID postId) {
        List<Post> published = findAllPublishedPosts();
        int index = -1;
        for (int i = 0; i < published.size(); i++) {
            if (published.get(i).id().equals(postId)) {
                index = i;
                break;
            }
        }
        if (index < 0) {
            return new AdjacentPublishedPosts(null, null);
        }
        Post newer = index > 0 ? published.get(index - 1) : null;
        Post older = index + 1 < published.size() ? published.get(index + 1) : null;
        return new AdjacentPublishedPosts(older, newer);
    }

    // 按分类分页查询已发布文章。
    public List<Post> listPublishedPostsByCategory(UUID categoryId, int page, int pageSize) {
        return postRepository.findPublishedPostsByCategory(categoryId, page, pageSize);
    }

    // 统计指定分类下已发布文章总数。
    public long countPublishedPostsByCategory(UUID categoryId) {
        return postRepository.countPublishedPostsByCategory(categoryId);
    }

    // 按标签分页查询已发布文章。
    public List<Post> listPublishedPostsByTag(UUID tagId, int page, int pageSize) {
        return postRepository.findPublishedPostsByTag(tagId, page, pageSize);
    }

    // 统计指定标签下已发布文章总数。
    public long countPublishedPostsByTag(UUID tagId) {
        return postRepository.countPublishedPostsByTag(tagId);
    }

    // 按关键词搜索已发布文章（匹配标题和正文），分页返回。
    public List<Post> searchPublishedPosts(String keyword, int page, int pageSize) {
        return postRepository.searchPublishedPosts(keyword, page, pageSize);
    }

    // 统计关键词搜索匹配的已发布文章总数。
    public long countSearchPublishedPosts(String keyword) {
        return postRepository.countSearchPublishedPosts(keyword);
    }

    // 递增文章阅读数并持久化。不递增 version，因为这不是作者编辑操作。
    public void incrementViewCount(UUID postId) {
        Post post = getPost(postId);
        postRepository.save(post.incrementViewCount());
    }

    public List<Post> searchByTitleKeyword(String keyword) {
        return searchAdminPosts(keyword, null, null);
    }

    public List<Post> searchAdminPosts(String keyword, UUID categoryId, UUID tagId) {
        String normalizedKeyword = keyword == null ? "" : keyword.trim().toLowerCase();
        return postRepository.findAll().stream()
                .filter(post -> normalizedKeyword.isBlank()
                        || post.title().toLowerCase().contains(normalizedKeyword))
                .filter(post -> categoryId == null || categoryId.equals(post.categoryId()))
                .filter(post -> tagId == null || post.tagIds().contains(tagId))
                .sorted(Comparator.comparing((Post post) -> post.updatedAt()).reversed())
                .toList();
    }

    public Post publish(UUID postId) {
        Post current = getPost(postId);
        Post saved = postRepository.save(current.publish(Instant.now(clock)));
        recordRevision(saved, PostRevisionKind.PUBLISH);
        syncCorpus(current, saved);
        return saved;
    }

    public Post unpublish(UUID postId) {
        Post current = getPost(postId);
        Post saved = postRepository.save(current.unpublish(Instant.now(clock)));
        syncCorpus(current, saved);
        return saved;
    }

    public Post moveToTrash(UUID postId) {
        Post current = getPost(postId);
        Post saved = postRepository.save(current.moveToTrash(Instant.now(clock)));
        syncCorpus(current, saved);
        return saved;
    }

    public Post restoreFromTrash(UUID postId) {
        // 恢复逻辑由领域对象决定恢复到哪个状态，应用层只负责持久化结果。
        Post restoredPost = getPost(postId).restoreFromTrash(Instant.now(clock));
        return postRepository.save(restoredPost);
    }

    public void permanentlyDelete(UUID postId) {
        Post post = getPost(postId);
        post.assertPermanentlyDeletable();
        postRepository.deleteById(postId);
        if (revisionService != null) {
            revisionService.deleteByPostId(postId);
        }
        if (commentRepository != null) {
            commentRepository.deleteByPostId(postId);
        }
        if (mediaService != null) {
            mediaService.deleteUnreferencedLocalFiles(post, postRepository.findAll());
        }
        if (aiCorpusSync != null) {
            aiCorpusSync.delete(postId);
        }
    }

    public Post restoreRevision(UUID postId, UUID revisionId) {
        if (revisionService == null) {
            throw new IllegalStateException("版本服务未启用");
        }
        Post current = getPost(postId);
        PostRevision revision = revisionService.get(postId, revisionId);
        Post restored = updatePost(
                postId,
                revision.title(),
                revision.markdownContent(),
                current.slug(),
                current.categoryId(),
                current.tagIds(),
                current.version(),
                revision.excerpt() == null ? "" : revision.excerpt(),
                current.coverUrl(),
                current.seoTitle(),
                current.seoDescription()
        );
        recordRevision(restored, PostRevisionKind.RESTORE);
        return restored;
    }

    private void recordRevision(Post post, PostRevisionKind kind) {
        if (revisionService != null) {
            revisionService.record(post, kind);
        }
    }

    public AdminDashboard getDashboard() {
        List<Post> all = postRepository.findAll();
        long published = countByStatus(all, PostStatus.PUBLISHED);
        long draft = countByStatus(all, PostStatus.DRAFT);
        long unpublished = countByStatus(all, PostStatus.UNPUBLISHED);
        long trashed = countByStatus(all, PostStatus.TRASHED);
        long publishedViewCount = all.stream()
                .filter(post -> post.status() == PostStatus.PUBLISHED)
                .mapToLong(Post::viewCount)
                .sum();

        List<Post> recentlyEdited = all.stream()
                .filter(post -> post.status() != PostStatus.TRASHED)
                .sorted(Comparator.comparing(Post::updatedAt).reversed())
                .limit(DASHBOARD_RECENT_LIMIT)
                .toList();
        List<Post> recentlyPublished = all.stream()
                .filter(post -> post.status() == PostStatus.PUBLISHED)
                .sorted(Comparator.comparing(Post::publishedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(DASHBOARD_RECENT_LIMIT)
                .toList();

        return new AdminDashboard(
                published + draft + unpublished,
                published,
                draft,
                unpublished,
                trashed,
                publishedViewCount,
                recentlyEdited,
                recentlyPublished
        );
    }

    private static long countByStatus(List<Post> posts, PostStatus status) {
        return posts.stream().filter(post -> post.status() == status).count();
    }

    public BatchPostResult batchUnpublish(List<UUID> ids) {
        return runBatch(ids, this::unpublish);
    }

    public BatchPostResult batchMoveToTrash(List<UUID> ids) {
        return runBatch(ids, this::moveToTrashAllowingPublished);
    }

    // 批量移入回收站时，已发布文章先下线再进回收站，避免选中后无法操作。
    private Post moveToTrashAllowingPublished(UUID postId) {
        Post post = getPost(postId);
        Instant now = Instant.now(clock);
        Post previous = post;
        if (post.status() == PostStatus.PUBLISHED) {
            post = post.unpublish(now);
        }
        Post saved = postRepository.save(post.moveToTrash(now));
        syncCorpus(previous, saved);
        return saved;
    }

    private void syncCorpus(Post previous, Post current) {
        if (aiCorpusSync == null || current == null) {
            return;
        }
        boolean nowPublished = current.status() == PostStatus.PUBLISHED;
        boolean wasPublished = previous != null && previous.status() == PostStatus.PUBLISHED;
        if (nowPublished) {
            aiCorpusSync.upsert(current);
        } else if (wasPublished) {
            aiCorpusSync.delete(current.id());
        }
    }

    private BatchPostResult runBatch(List<UUID> ids, Function<UUID, Post> action) {
        List<UUID> uniqueIds = ids.stream().distinct().toList();
        List<Post> succeeded = new ArrayList<>();
        List<BatchPostResult.Failure> failed = new ArrayList<>();
        for (UUID id : uniqueIds) {
            try {
                succeeded.add(action.apply(id));
            } catch (RuntimeException exception) {
                failed.add(new BatchPostResult.Failure(id, exception.getMessage()));
            }
        }
        return new BatchPostResult(List.copyOf(succeeded), List.copyOf(failed));
    }

    private String resolveSlugForCreate(String title, String requestedSlug) {
        // 手动传 slug 时直接校验并使用；未传时根据标题自动生成并补唯一后缀。
        if (requestedSlug != null && !requestedSlug.isBlank()) {
            String normalizedRequestedSlug = slugGenerator.normalizeRequestedSlug(requestedSlug);
            if (postRepository.existsBySlug(normalizedRequestedSlug)) {
                throw new DuplicateSlugException(normalizedRequestedSlug);
            }
            return normalizedRequestedSlug;
        }

        String generatedSlug = slugGenerator.generateFromTitle(title);
        return slugGenerator.ensureUnique(generatedSlug, postRepository::existsBySlug);
    }

    private String resolveSlugForUpdate(Post currentPost, String title, String requestedSlug) {
        // 不传 slug 时沿用原值；这样管理端只改标题或正文时不会意外改 URL。
        if (requestedSlug == null) {
            return currentPost.slug();
        }
        // 显式传空字符串时，表示让系统按最新标题重新生成 slug。
        if (requestedSlug.isBlank()) {
            String generatedSlug = slugGenerator.generateFromTitle(title);
            return slugGenerator.ensureUnique(generatedSlug, slug -> slugExistsForOtherPost(currentPost, slug));
        }

        String normalizedRequestedSlug = slugGenerator.normalizeRequestedSlug(requestedSlug);
        if (slugExistsForOtherPost(currentPost, normalizedRequestedSlug)) {
            throw new DuplicateSlugException(normalizedRequestedSlug);
        }
        return normalizedRequestedSlug;
    }

    private boolean slugExistsForOtherPost(Post currentPost, String slug) {
        return postRepository.findBySlug(slug)
                .filter(post -> !post.id().equals(currentPost.id()))
                .isPresent();
    }

    // 判断标题、slug、正文、分类、标签、摘要和封面是否与当前文章完全一致，用于自动保存的幂等检测。
    private boolean contentUnchanged(Post currentPost, String title, String resolvedSlug,
                                     String normalizedContent, UUID categoryId, List<UUID> tagIds,
                                     String excerpt, String coverUrl,
                                     String seoTitle, String seoDescription) {
        String trimmedTitle = title != null ? title.trim() : "";
        String normalizedExcerpt = excerpt == null || excerpt.isBlank() ? null : excerpt.trim();
        String normalizedCoverUrl = coverUrl == null || coverUrl.isBlank() ? null : coverUrl.trim();
        String normalizedSeoTitle = seoTitle == null || seoTitle.isBlank() ? null : seoTitle.trim();
        String normalizedSeoDescription = seoDescription == null || seoDescription.isBlank() ? null : seoDescription.trim();
        return trimmedTitle.equals(currentPost.title())
                && resolvedSlug.equals(currentPost.slug())
                && normalizedContent.equals(currentPost.markdownContent())
                && Objects.equals(categoryId, currentPost.categoryId())
                && Objects.equals(tagIds, currentPost.tagIds())
                && Objects.equals(normalizedExcerpt, currentPost.excerpt())
                && Objects.equals(normalizedCoverUrl, currentPost.coverUrl())
                && Objects.equals(normalizedSeoTitle, currentPost.seoTitle())
                && Objects.equals(normalizedSeoDescription, currentPost.seoDescription());
    }

    private String defaultContent(String markdownContent) {
        // 草稿允许先没有正文，所以这里把 null 归一成空字符串。
        return markdownContent == null ? "" : markdownContent;
    }
}
