package com.linqibin.blog.taxonomy.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

// 标签仓库抽象：领域层只定义需要什么持久化能力，不关心底层怎么存。
public interface TagRepository {

    // 保存标签的新状态，创建和更新都走这个入口。
    Tag save(Tag tag);

    // 按主键查询标签。
    Optional<Tag> findById(UUID id);

    // 按 slug 查询标签，主要给公开接口使用。
    Optional<Tag> findBySlug(String slug);

    // 判断 slug 是否已被占用。
    boolean existsBySlug(String slug);

    // 返回全部标签。
    List<Tag> findAll();

    // 按主键删除标签。
    void deleteById(UUID id);
}
