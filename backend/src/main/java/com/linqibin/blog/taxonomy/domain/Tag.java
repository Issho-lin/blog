package com.linqibin.blog.taxonomy.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

// 标签领域实体：只关心标签自身的数据和校验规则。
public record Tag(
        UUID id,
        String name,
        String slug,
        Instant createdAt,
        Instant updatedAt
) {

    public Tag {
        Objects.requireNonNull(id, "id 不能为空");
        Objects.requireNonNull(name, "标签名称不能为空");
        Objects.requireNonNull(slug, "slug 不能为空");
        Objects.requireNonNull(createdAt, "创建时间不能为空");
        Objects.requireNonNull(updatedAt, "更新时间不能为空");

        name = name.trim();
        slug = slug.trim();

        if (name.isBlank()) {
            throw new IllegalArgumentException("标签名称不能为空");
        }
        if (slug.isBlank()) {
            throw new IllegalArgumentException("slug 不能为空");
        }
    }

    // 工厂方法：创建新标签，创建时间和更新时间相同。
    public static Tag create(UUID id, String name, String slug, Instant now) {
        return new Tag(id, name, slug, now, now);
    }

    // 更新标签信息：slug 不在此处变更，由应用层单独管理唯一性。
    public Tag update(String name, Instant now) {
        return new Tag(id, name, slug, createdAt, now);
    }
}
