package com.linqibin.blog.post.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.linqibin.blog.post.domain.Post;
import com.linqibin.blog.post.domain.PostRepository;

// 数据库版文章仓库适配器：对外实现领域仓库接口，对内委托 Spring Data JPA。
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
}
