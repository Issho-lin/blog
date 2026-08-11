package com.linqibin.blog.taxonomy.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

// 分类领域实体：只关心分类自身的数据和校验规则。
public record Category(
        UUID id,
        String name,
        String slug,
        String description,
        Instant createdAt,
        Instant updatedAt
) {

    public Category {
        Objects.requireNonNull(id, "id 不能为空");
        Objects.requireNonNull(name, "分类名称不能为空");
        Objects.requireNonNull(slug, "slug 不能为空");
        Objects.requireNonNull(createdAt, "创建时间不能为空");
        Objects.requireNonNull(updatedAt, "更新时间不能为空");

        name = name.trim();
        slug = slug.trim();

        if (name.isBlank()) {
            throw new IllegalArgumentException("分类名称不能为空");
        }
        if (slug.isBlank()) {
            throw new IllegalArgumentException("slug 不能为空");
        }
    }

    // 工厂方法：创建新分类，创建时间和更新时间相同。
    public static Category create(UUID id, String name, String slug, String description, Instant now) {
        return new Category(id, name, slug, description, now, now);
    }

    // 更新分类信息：slug 不在此处变更，由应用层单独管理唯一性。
    public Category update(String name, String description, Instant now) {
        return new Category(id, name, slug, description, createdAt, now);
    }
}
