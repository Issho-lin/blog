package com.linqibin.blog.taxonomy.application;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

import com.linqibin.blog.post.domain.SlugGenerator;
import com.linqibin.blog.taxonomy.domain.Tag;
import com.linqibin.blog.taxonomy.domain.TagRepository;
import com.linqibin.blog.taxonomy.exception.DuplicateTaxonomySlugException;
import com.linqibin.blog.taxonomy.exception.TagNotFoundException;

// 应用层负责把"查找/生成 slug/创建 Tag/保存"这些步骤串成完整用例。
public class TagService {

    private final TagRepository tagRepository;
    private final SlugGenerator slugGenerator;
    private final Clock clock;
    private final Supplier<UUID> idSupplier;

    public TagService(TagRepository tagRepository, SlugGenerator slugGenerator, Clock clock) {
        this(tagRepository, slugGenerator, clock, UUID::randomUUID);
    }

    public TagService(
            TagRepository tagRepository,
            SlugGenerator slugGenerator,
            Clock clock,
            Supplier<UUID> idSupplier
    ) {
        this.tagRepository = Objects.requireNonNull(tagRepository);
        this.slugGenerator = Objects.requireNonNull(slugGenerator);
        this.clock = Objects.requireNonNull(clock);
        this.idSupplier = Objects.requireNonNull(idSupplier);
    }

    // 创建标签：解析 slug -> 生成实体 -> 持久化。
    public Tag create(String name, String requestedSlug) {
        Instant now = Instant.now(clock);
        String slug = resolveSlugForCreate(name, requestedSlug);
        Tag tag = Tag.create(idSupplier.get(), name, slug, now);
        return tagRepository.save(tag);
    }

    // 更新标签名称：slug 不在此处变更。
    public Tag update(UUID id, String name) {
        Tag tag = getTag(id);
        Tag updated = tag.update(name, Instant.now(clock));
        return tagRepository.save(updated);
    }

    // 单独修改标签 slug：需要做唯一性校验。
    public Tag updateSlug(UUID id, String requestedSlug) {
        Tag tag = getTag(id);
        String normalizedSlug = slugGenerator.normalizeRequestedSlug(requestedSlug);
        if (!tag.slug().equals(normalizedSlug) && tagRepository.existsBySlug(normalizedSlug)) {
            throw new DuplicateTaxonomySlugException(normalizedSlug);
        }
        Tag updated = new Tag(tag.id(), tag.name(), normalizedSlug, tag.createdAt(), Instant.now(clock));
        return tagRepository.save(updated);
    }

    public Tag getTag(UUID id) {
        return tagRepository.findById(id).orElseThrow(() -> new TagNotFoundException(id));
    }

    public Tag getTagBySlug(String slug) {
        return tagRepository.findBySlug(slug).orElseThrow(() -> new TagNotFoundException(slug));
    }

    // 按 slug 查找标签，返回 Optional 而非抛异常，给导入功能使用。
    public java.util.Optional<Tag> findBySlug(String slug) {
        return tagRepository.findBySlug(slug);
    }

    // 按名称查找标签（用于导入时从 Front Matter 回填）。
    public java.util.Optional<Tag> findByName(String name) {
        return tagRepository.findAll().stream()
                .filter(t -> t.name().equalsIgnoreCase(name))
                .findFirst();
    }

    public List<Tag> findAll() {
        return tagRepository.findAll();
    }

    public void delete(UUID id) {
        tagRepository.deleteById(id);
    }

    private String resolveSlugForCreate(String name, String requestedSlug) {
        if (requestedSlug != null && !requestedSlug.isBlank()) {
            String normalized = slugGenerator.normalizeRequestedSlug(requestedSlug);
            if (tagRepository.existsBySlug(normalized)) {
                throw new DuplicateTaxonomySlugException(normalized);
            }
            return normalized;
        }
        String generated = slugGenerator.generateFromTitle(name);
        return slugGenerator.ensureUnique(generated, tagRepository::existsBySlug);
    }
}
