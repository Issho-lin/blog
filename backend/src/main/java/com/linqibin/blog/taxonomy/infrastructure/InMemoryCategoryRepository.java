package com.linqibin.blog.taxonomy.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.linqibin.blog.taxonomy.domain.Category;
import com.linqibin.blog.taxonomy.domain.CategoryRepository;

// 内存版分类仓库：当前用于开发和测试阶段，后续可以替换成数据库实现。
public class InMemoryCategoryRepository implements CategoryRepository {

    private final ConcurrentHashMap<UUID, Category> categories = new ConcurrentHashMap<>();

    public void clear() {
        categories.clear();
    }

    @Override
    public Category save(Category category) {
        categories.put(category.id(), category);
        return category;
    }

    @Override
    public Optional<Category> findById(UUID id) {
        return Optional.ofNullable(categories.get(id));
    }

    @Override
    public Optional<Category> findBySlug(String slug) {
        return categories.values().stream()
                .filter(category -> category.slug().equals(slug))
                .findFirst();
    }

    @Override
    public boolean existsBySlug(String slug) {
        return categories.values().stream().anyMatch(category -> category.slug().equals(slug));
    }

    @Override
    public List<Category> findAll() {
        return List.copyOf(categories.values());
    }

    @Override
    public void deleteById(UUID id) {
        categories.remove(id);
    }
}
