package com.linqibin.blog.common.request;

import jakarta.servlet.http.HttpServletRequest;

public final class RequestIdUtils {

    public static final String REQUEST_ID_ATTRIBUTE = "requestId";
    public static final String REQUEST_ID_HEADER = "X-Request-Id";

    private RequestIdUtils() {
    }

    public static String getRequestId(HttpServletRequest request) {
        // 优先从过滤器设置的属性中获取，回退到请求头，兼容 addFilters=false 的测试场景。
        Object requestId = request.getAttribute(REQUEST_ID_ATTRIBUTE);
        if (requestId == null) {
            requestId = request.getHeader(REQUEST_ID_HEADER);
        }
        return requestId == null ? "" : requestId.toString();
    }
}
