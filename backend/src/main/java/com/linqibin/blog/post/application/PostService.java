package com.linqibin.blog.post.application;

import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

import com.linqibin.blog.post.domain.Post;
import com.linqibin.blog.post.domain.PostRepository;
import com.linqibin.blog.post.domain.SlugGenerator;
import com.linqibin.blog.post.exception.DuplicateSlugException;
import com.linqibin.blog.post.exception.PostNotFoundException;

// 应用层负责把“查找/生成 slug/创建 Post/保存”这些步骤串成完整用例。
public class PostService {

    // 应用层依赖抽象仓库，具体落库方式由基础设施层决定。
    private final PostRepository postRepository;
    // slug 规则也通过领域服务集中管理，避免散落在各个接口里。
    private final SlugGenerator slugGenerator;
    private final Clock clock;
    private final Supplier<UUID> idSupplier;

    public PostService(PostRepository postRepository, SlugGenerator slugGenerator, Clock clock) {
        this(postRepository, slugGenerator, clock, UUID::randomUUID);
    }

    public PostService(
            PostRepository postRepository,
            SlugGenerator slugGenerator,
            Clock clock,
            Supplier<UUID> idSupplier
    ) {
        this.postRepository = Objects.requireNonNull(postRepository);
        this.slugGenerator = Objects.requireNonNull(slugGenerator);
        this.clock = Objects.requireNonNull(clock);
        this.idSupplier = Objects.requireNonNull(idSupplier);
    }

    public Post createDraft(String title, String markdownContent, String requestedSlug) {
        // 创建草稿的完整流程：定时间 -> 算 slug -> 生成实体 -> 持久化。
        Instant now = Instant.now(clock);
        String slug = resolveSlugForCreate(title, requestedSlug);
        Post post = Post.createDraft(idSupplier.get(), title, slug, defaultContent(markdownContent), now);
        return postRepository.save(post);
    }

    public Post updatePost(UUID postId, String title, String markdownContent, String requestedSlug) {
        // 编辑文章时先取到当前实体，再决定 slug 是否保留、重算或改成手动值。
        Post currentPost = getPost(postId);
        String resolvedSlug = resolveSlugForUpdate(currentPost, title, requestedSlug);
        Post updatedPost = currentPost.update(title, resolvedSlug, defaultContent(markdownContent), Instant.now(clock));
        return postRepository.save(updatedPost);
    }

    public Post getPost(UUID postId) {
        // 管理端以 id 为主读取文章，不存在时抛业务异常交给全局异常处理器转换。
        return postRepository.findById(postId).orElseThrow(() -> new PostNotFoundException(postId));
    }

    public Post getPostBySlug(String slug) {
        // 前台读取文章更适合走 slug，因为它比数据库 id 更稳定也更可读。
        return postRepository.findBySlug(slug).orElseThrow(() -> new PostNotFoundException(slug));
    }

    public List<Post> searchByTitleKeyword(String keyword) {
        // 先把搜索关键字规范化，空关键字时就等价于返回全部文章。
        String normalizedKeyword = keyword == null ? "" : keyword.trim().toLowerCase();
        return postRepository.findAll().stream()
                .filter(post -> normalizedKeyword.isBlank()
                        || post.title().toLowerCase().contains(normalizedKeyword))
                // 管理端列表通常优先看最近操作过的文章，所以按更新时间倒序。
                .sorted(Comparator.comparing(Post::updatedAt).reversed())
                .toList();
    }

    public Post publish(UUID postId) {
        // 先取出文章，再交给领域对象自己判断是否允许发布。
        Post publishedPost = getPost(postId).publish(Instant.now(clock));
        return postRepository.save(publishedPost);
    }

    public Post unpublish(UUID postId) {
        // 应用层只做流程串联，不在这里重复写状态判断。
        Post unpublishedPost = getPost(postId).unpublish(Instant.now(clock));
        return postRepository.save(unpublishedPost);
    }

    public Post moveToTrash(UUID postId) {
        // 回收站操作仍然复用领域对象的状态规则。
        Post trashedPost = getPost(postId).moveToTrash(Instant.now(clock));
        return postRepository.save(trashedPost);
    }

    public Post restoreFromTrash(UUID postId) {
        // 恢复逻辑由领域对象决定恢复到哪个状态，应用层只负责持久化结果。
        Post restoredPost = getPost(postId).restoreFromTrash(Instant.now(clock));
        return postRepository.save(restoredPost);
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

    private String defaultContent(String markdownContent) {
        // 草稿允许先没有正文，所以这里把 null 归一成空字符串。
        return markdownContent == null ? "" : markdownContent;
    }
}
