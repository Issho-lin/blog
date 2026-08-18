package com.linqibin.blog.post.infrastructure;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.linqibin.blog.post.domain.Post;
import com.linqibin.blog.post.domain.PostRepository;

// 内存版文章仓库：当前用于开发和测试阶段，后续可以替换成数据库实现。
public class InMemoryPostRepository implements PostRepository {

    private final ConcurrentHashMap<UUID, Post> posts = new ConcurrentHashMap<>();

    public void clear() {
        // 测试前清空内存数据，避免用例互相污染。
        posts.clear();
    }

    @Override
    public void deleteById(UUID id) {
        posts.remove(id);
    }

    @Override
    public Post save(Post post) {
        // 直接用文章 id 覆盖保存，既支持创建也支持更新。
        posts.put(post.id(), post);
        return post;
    }

    @Override
    public Optional<Post> findById(UUID id) {
        // 管理端按主键读取时走哈希表直查。
        return Optional.ofNullable(posts.get(id));
    }

    @Override
    public Optional<Post> findBySlug(String slug) {
        // 公开接口按 slug 查文章，这里先用遍历实现，后续切数据库再优化。
        return posts.values().stream()
                .filter(post -> post.slug().equals(slug))
                .findFirst();
    }

    @Override
    public boolean existsBySlug(String slug) {
        // 创建文章前用它判断 slug 是否已占用。
        return posts.values().stream().anyMatch(post -> post.slug().equals(slug));
    }

    @Override
    public List<Post> findAll() {
        // 返回不可变快照，避免调用方误改仓库内部状态。
        return List.copyOf(posts.values());
    }

    @Override
    public List<Post> findPublishedPosts(int page, int pageSize) {
        // 过滤已发布文章并按发布时间倒序排列，然后做分页截取。
        List<Post> published = posts.values().stream()
                .filter(Post::isPubliclyReadable)
                .sorted(Comparator.comparing(Post::publishedAt).reversed())
                .toList();
        int fromIndex = (page - 1) * pageSize;
        if (fromIndex >= published.size()) {
            return List.of();
        }
        int toIndex = Math.min(fromIndex + pageSize, published.size());
        return published.subList(fromIndex, toIndex);
    }

    @Override
    public long countPublishedPosts() {
        return posts.values().stream()
                .filter(Post::isPubliclyReadable)
                .count();
    }

    @Override
    public List<Post> findPublishedPostsByCategory(UUID categoryId, int page, int pageSize) {
        List<Post> filtered = posts.values().stream()
                .filter(Post::isPubliclyReadable)
                .filter(post -> categoryId.equals(post.categoryId()))
                .sorted(Comparator.comparing(Post::publishedAt).reversed())
                .toList();
        int fromIndex = (page - 1) * pageSize;
        if (fromIndex >= filtered.size()) {
            return List.of();
        }
        int toIndex = Math.min(fromIndex + pageSize, filtered.size());
        return filtered.subList(fromIndex, toIndex);
    }

    @Override
    public long countPublishedPostsByCategory(UUID categoryId) {
        return posts.values().stream()
                .filter(Post::isPubliclyReadable)
                .filter(post -> categoryId.equals(post.categoryId()))
                .count();
    }

    @Override
    public List<Post> findPublishedPostsByTag(UUID tagId, int page, int pageSize) {
        List<Post> filtered = posts.values().stream()
                .filter(Post::isPubliclyReadable)
                .filter(post -> post.tagIds().contains(tagId))
                .sorted(Comparator.comparing(Post::publishedAt).reversed())
                .toList();
        int fromIndex = (page - 1) * pageSize;
        if (fromIndex >= filtered.size()) {
            return List.of();
        }
        int toIndex = Math.min(fromIndex + pageSize, filtered.size());
        return filtered.subList(fromIndex, toIndex);
    }

    @Override
    public long countPublishedPostsByTag(UUID tagId) {
        return posts.values().stream()
                .filter(Post::isPubliclyReadable)
                .filter(post -> post.tagIds().contains(tagId))
                .count();
    }

    @Override
    public List<Post> searchPublishedPosts(String keyword, int page, int pageSize) {
        String normalized = keyword == null ? "" : keyword.trim().toLowerCase();
        List<Post> matched = posts.values().stream()
                .filter(Post::isPubliclyReadable)
                .filter(post -> post.matchesKeyword(normalized))
                .sorted(Comparator.comparing(Post::publishedAt).reversed())
                .toList();
        int fromIndex = (page - 1) * pageSize;
        if (fromIndex >= matched.size()) {
            return List.of();
        }
        int toIndex = Math.min(fromIndex + pageSize, matched.size());
        return matched.subList(fromIndex, toIndex);
    }

    @Override
    public long countSearchPublishedPosts(String keyword) {
        String normalized = keyword == null ? "" : keyword.trim().toLowerCase();
        return posts.values().stream()
                .filter(Post::isPubliclyReadable)
                .filter(post -> post.matchesKeyword(normalized))
                .count();
    }
}
