package com.linqibin.blog.taxonomy.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.linqibin.blog.taxonomy.domain.Tag;
import com.linqibin.blog.taxonomy.domain.TagRepository;

// 数据库版标签仓库适配器：对外实现领域仓库接口，对内委托 Spring Data JPA。
public class TagRepositoryAdapter implements TagRepository {

    private final SpringDataTagRepository springDataTagRepository;
    private final TagEntityMapper tagEntityMapper;

    public TagRepositoryAdapter(
            SpringDataTagRepository springDataTagRepository,
            TagEntityMapper tagEntityMapper
    ) {
        this.springDataTagRepository = springDataTagRepository;
        this.tagEntityMapper = tagEntityMapper;
    }

    @Override
    public Tag save(Tag tag) {
        TagEntity savedEntity = springDataTagRepository.save(tagEntityMapper.toEntity(tag));
        return tagEntityMapper.toDomain(savedEntity);
    }

    @Override
    public Optional<Tag> findById(UUID id) {
        return springDataTagRepository.findById(id)
                .map(tagEntityMapper::toDomain);
    }

    @Override
    public Optional<Tag> findBySlug(String slug) {
        return springDataTagRepository.findBySlug(slug)
                .map(tagEntityMapper::toDomain);
    }

    @Override
    public boolean existsBySlug(String slug) {
        return springDataTagRepository.existsBySlug(slug);
    }

    @Override
    public List<Tag> findAll() {
        return springDataTagRepository.findAll().stream()
                .map(tagEntityMapper::toDomain)
                .toList();
    }

    @Override
    public void deleteById(UUID id) {
        springDataTagRepository.deleteById(id);
    }
}
