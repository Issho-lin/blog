package com.linqibin.blog.taxonomy.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.linqibin.blog.taxonomy.domain.Tag;
import com.linqibin.blog.taxonomy.domain.TagRepository;

// 内存版标签仓库：当前用于开发和测试阶段，后续可以替换成数据库实现。
public class InMemoryTagRepository implements TagRepository {

    private final ConcurrentHashMap<UUID, Tag> tags = new ConcurrentHashMap<>();

    public void clear() {
        tags.clear();
    }

    @Override
    public Tag save(Tag tag) {
        tags.put(tag.id(), tag);
        return tag;
    }

    @Override
    public Optional<Tag> findById(UUID id) {
        return Optional.ofNullable(tags.get(id));
    }

    @Override
    public Optional<Tag> findBySlug(String slug) {
        return tags.values().stream()
                .filter(tag -> tag.slug().equals(slug))
                .findFirst();
    }

    @Override
    public boolean existsBySlug(String slug) {
        return tags.values().stream().anyMatch(tag -> tag.slug().equals(slug));
    }

    @Override
    public List<Tag> findAll() {
        return List.copyOf(tags.values());
    }

    @Override
    public void deleteById(UUID id) {
        tags.remove(id);
    }
}
