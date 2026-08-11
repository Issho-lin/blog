package com.linqibin.blog.common.api;

import java.util.List;

// 统一分页响应：前端通过 items 获取数据，通过 page/pageSize/total/totalPages 渲染分页器。
public record PageResponse<T>(
        List<T> items,
        int page,
        int pageSize,
        long total,
        int totalPages
) {
    public static <T> PageResponse<T> of(List<T> items, int page, int pageSize, long total) {
        int totalPages = pageSize > 0 ? (int) Math.ceil((double) total / pageSize) : 0;
        return new PageResponse<>(items, page, pageSize, total, totalPages);
    }
}
