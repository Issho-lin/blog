package com.linqibin.blog.taxonomy.application;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

import com.linqibin.blog.post.domain.SlugGenerator;
import com.linqibin.blog.taxonomy.domain.Category;
import com.linqibin.blog.taxonomy.domain.CategoryRepository;
import com.linqibin.blog.taxonomy.exception.CategoryNotFoundException;
import com.linqibin.blog.taxonomy.exception.DuplicateTaxonomySlugException;

// 应用层负责把"查找/生成 slug/创建 Category/保存"这些步骤串成完整用例。
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final SlugGenerator slugGenerator;
    private final Clock clock;
    private final Supplier<UUID> idSupplier;

    public CategoryService(CategoryRepository categoryRepository, SlugGenerator slugGenerator, Clock clock) {
        this(categoryRepository, slugGenerator, clock, UUID::randomUUID);
    }

    public CategoryService(
            CategoryRepository categoryRepository,
            SlugGenerator slugGenerator,
            Clock clock,
            Supplier<UUID> idSupplier
    ) {
        this.categoryRepository = Objects.requireNonNull(categoryRepository);
        this.slugGenerator = Objects.requireNonNull(slugGenerator);
        this.clock = Objects.requireNonNull(clock);
        this.idSupplier = Objects.requireNonNull(idSupplier);
    }

    // 创建分类：解析 slug -> 生成实体 -> 持久化。
    public Category create(String name, String requestedSlug, String description) {
        Instant now = Instant.now(clock);
        String slug = resolveSlugForCreate(name, requestedSlug);
        Category category = Category.create(idSupplier.get(), name, slug, description, now);
        return categoryRepository.save(category);
    }

    // 更新分类名称和描述：slug 不在此处变更。
    public Category update(UUID id, String name, String description) {
        Category category = getCategory(id);
        Category updated = category.update(name, description, Instant.now(clock));
        return categoryRepository.save(updated);
    }

    // 单独修改分类 slug：需要做唯一性校验。
    public Category updateSlug(UUID id, String requestedSlug) {
        Category category = getCategory(id);
        String normalizedSlug = slugGenerator.normalizeRequestedSlug(requestedSlug);
        if (!category.slug().equals(normalizedSlug) && categoryRepository.existsBySlug(normalizedSlug)) {
            throw new DuplicateTaxonomySlugException(normalizedSlug);
        }
        Category updated = new Category(
                category.id(), category.name(), normalizedSlug,
                category.description(), category.createdAt(), Instant.now(clock)
        );
        return categoryRepository.save(updated);
    }

    public Category getCategory(UUID id) {
        return categoryRepository.findById(id).orElseThrow(() -> new CategoryNotFoundException(id));
    }

    public Category getCategoryBySlug(String slug) {
        return categoryRepository.findBySlug(slug).orElseThrow(() -> new CategoryNotFoundException(slug));
    }

    // 按 slug 查找分类，返回 Optional 而非抛异常，给导入功能使用。
    public java.util.Optional<Category> findBySlug(String slug) {
        return categoryRepository.findBySlug(slug);
    }

    // 按名称查找分类（用于导入时从 Front Matter 回填）。
    public java.util.Optional<Category> findByName(String name) {
        return categoryRepository.findAll().stream()
                .filter(c -> c.name().equalsIgnoreCase(name))
                .findFirst();
    }

    public List<Category> findAll() {
        return categoryRepository.findAll();
    }

    public void delete(UUID id) {
        categoryRepository.deleteById(id);
    }

    private String resolveSlugForCreate(String name, String requestedSlug) {
        // 手动传 slug 时直接校验并使用；未传时根据名称自动生成并补唯一后缀。
        if (requestedSlug != null && !requestedSlug.isBlank()) {
            String normalized = slugGenerator.normalizeRequestedSlug(requestedSlug);
            if (categoryRepository.existsBySlug(normalized)) {
                throw new DuplicateTaxonomySlugException(normalized);
            }
            return normalized;
        }
        String generated = slugGenerator.generateFromTitle(name);
        return slugGenerator.ensureUnique(generated, categoryRepository::existsBySlug);
    }
}
