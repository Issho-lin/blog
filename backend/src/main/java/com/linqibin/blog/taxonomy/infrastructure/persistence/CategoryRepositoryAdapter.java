package com.linqibin.blog.taxonomy.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.linqibin.blog.taxonomy.domain.Category;
import com.linqibin.blog.taxonomy.domain.CategoryRepository;

// 数据库版分类仓库适配器：对外实现领域仓库接口，对内委托 Spring Data JPA。
public class CategoryRepositoryAdapter implements CategoryRepository {

    private final SpringDataCategoryRepository springDataCategoryRepository;
    private final CategoryEntityMapper categoryEntityMapper;

    public CategoryRepositoryAdapter(
            SpringDataCategoryRepository springDataCategoryRepository,
            CategoryEntityMapper categoryEntityMapper
    ) {
        this.springDataCategoryRepository = springDataCategoryRepository;
        this.categoryEntityMapper = categoryEntityMapper;
    }

    @Override
    public Category save(Category category) {
        CategoryEntity savedEntity = springDataCategoryRepository.save(categoryEntityMapper.toEntity(category));
        return categoryEntityMapper.toDomain(savedEntity);
    }

    @Override
    public Optional<Category> findById(UUID id) {
        return springDataCategoryRepository.findById(id)
                .map(categoryEntityMapper::toDomain);
    }

    @Override
    public Optional<Category> findBySlug(String slug) {
        return springDataCategoryRepository.findBySlug(slug)
                .map(categoryEntityMapper::toDomain);
    }

    @Override
    public boolean existsBySlug(String slug) {
        return springDataCategoryRepository.existsBySlug(slug);
    }

    @Override
    public List<Category> findAll() {
        return springDataCategoryRepository.findAll().stream()
                .map(categoryEntityMapper::toDomain)
                .toList();
    }

    @Override
    public void deleteById(UUID id) {
        springDataCategoryRepository.deleteById(id);
    }
}
