package com.linqibin.blog.taxonomy.exception;

import java.util.UUID;

// 分类不存在异常：按 id 或 slug 查询不到分类时统一抛这个异常。
public class CategoryNotFoundException extends RuntimeException {

    public CategoryNotFoundException(UUID id) {
        super("分类不存在: " + id);
    }

    public CategoryNotFoundException(String slug) {
        super("分类不存在: " + slug);
    }
}
