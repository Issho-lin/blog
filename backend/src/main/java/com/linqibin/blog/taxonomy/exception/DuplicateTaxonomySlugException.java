package com.linqibin.blog.taxonomy.exception;

// 分类或标签 slug 冲突异常：当新增或修改时的 slug 已被其他分类或标签占用时抛出。
public class DuplicateTaxonomySlugException extends RuntimeException {

    public DuplicateTaxonomySlugException(String slug) {
        super("slug 已存在: " + slug);
    }
}
