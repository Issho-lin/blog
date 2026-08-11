package com.linqibin.blog.taxonomy.exception;

import java.util.UUID;

// 标签不存在异常：按 id 或 slug 查询不到标签时统一抛这个异常。
public class TagNotFoundException extends RuntimeException {

    public TagNotFoundException(UUID id) {
        super("标签不存在: " + id);
    }

    public TagNotFoundException(String slug) {
        super("标签不存在: " + slug);
    }
}
