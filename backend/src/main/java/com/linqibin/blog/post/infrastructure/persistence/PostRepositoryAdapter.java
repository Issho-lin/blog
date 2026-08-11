package com.linqibin.blog.post.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import com.linqibin.blog.post.domain.Post;
import com.linqibin.blog.post.domain.PostRepository;
import com.linqibin.blog.post.domain.PostStatus;

// 数据库版文章仓库适配器：对外实现领域仓库接口，对内委托 Spring Data JPA。
// @Transactional 确保方法执行期间 JPA 会话存活，避免 @ElementCollection 的 LAZY 加载异常。
@Transactional
public class PostRepositoryAdapter implements PostRepository {

    private final SpringDataPostRepository springDataPostRepository;
    private final PostEntityMapper postEntityMapper;

    public PostRepositoryAdapter(
            SpringDataPostRepository springDataPostRepository,
            PostEntityMapper postEntityMapper
    ) {
        this.springDataPostRepository = springDataPostRepository;
        this.postEntityMapper = postEntityMapper;
    }

    @Override
    public Post save(Post post) {
        PostEntity savedEntity = springDataPostRepository.save(postEntityMapper.toEntity(post));
        return postEntityMapper.toDomain(savedEntity);
    }

    @Override
    public Optional<Post> findById(UUID id) {
        return springDataPostRepository.findById(id)
                .map(postEntityMapper::toDomain);
    }

    @Override
    public Optional<Post> findBySlug(String slug) {
        return springDataPostRepository.findBySlug(slug)
                .map(postEntityMapper::toDomain);
    }

    @Override
    public boolean existsBySlug(String slug) {
        return springDataPostRepository.existsBySlug(slug);
    }

    @Override
    public List<Post> findAll() {
        return springDataPostRepository.findAll().stream()
                .map(postEntityMapper::toDomain)
                .toList();
    }

    @Override
    public List<Post> findPublishedPosts(int page, int pageSize) {
        // Spring Data JPA 的 page 从 0 开始，外部传入的 page 从 1 开始，这里做转换。
        PageRequest pageable = PageRequest.of(page - 1, pageSize);
        return springDataPostRepository.findByStatus(PostStatus.PUBLISHED, pageable)
                .stream()
                .map(postEntityMapper::toDomain)
                .toList();
    }

    @Override
    public long countPublishedPosts() {
        return springDataPostRepository.countByStatus(PostStatus.PUBLISHED);
    }

    @Override
    public List<Post> findPublishedPostsByCategory(UUID categoryId, int page, int pageSize) {
        PageRequest pageable = PageRequest.of(page - 1, pageSize);
        return springDataPostRepository.findPublishedByCategory(PostStatus.PUBLISHED, categoryId, pageable)
                .stream()
                .map(postEntityMapper::toDomain)
                .toList();
    }

    @Override
    public long countPublishedPostsByCategory(UUID categoryId) {
        return springDataPostRepository.countPublishedByCategory(PostStatus.PUBLISHED, categoryId);
    }

    @Override
    public List<Post> findPublishedPostsByTag(UUID tagId, int page, int pageSize) {
        PageRequest pageable = PageRequest.of(page - 1, pageSize);
        return springDataPostRepository.findPublishedByTag(PostStatus.PUBLISHED, tagId, pageable)
                .stream()
                .map(postEntityMapper::toDomain)
                .toList();
    }

    @Override
    public long countPublishedPostsByTag(UUID tagId) {
        return springDataPostRepository.countPublishedByTag(PostStatus.PUBLISHED, tagId);
    }

    @Override
    public List<Post> searchPublishedPosts(String keyword, int page, int pageSize) {
        PageRequest pageable = PageRequest.of(page - 1, pageSize);
        return springDataPostRepository.searchPublished(PostStatus.PUBLISHED, keyword, pageable)
                .stream()
                .map(postEntityMapper::toDomain)
                .toList();
    }

    @Override
    public long countSearchPublishedPosts(String keyword) {
        return springDataPostRepository.countSearchPublished(PostStatus.PUBLISHED, keyword);
    }
}
