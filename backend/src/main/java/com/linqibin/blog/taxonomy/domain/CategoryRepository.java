package com.linqibin.blog.taxonomy.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

// 分类仓库抽象：领域层只定义需要什么持久化能力，不关心底层怎么存。
public interface CategoryRepository {

    // 保存分类的新状态，创建和更新都走这个入口。
    Category save(Category category);

    // 按主键查询分类。
    Optional<Category> findById(UUID id);

    // 按 slug 查询分类，主要给公开接口使用。
    Optional<Category> findBySlug(String slug);

    // 判断 slug 是否已被占用。
    boolean existsBySlug(String slug);

    // 返回全部分类。
    List<Category> findAll();

    // 按主键删除分类。
    void deleteById(UUID id);
}
