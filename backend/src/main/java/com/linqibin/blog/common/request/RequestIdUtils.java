package com.linqibin.blog.common.request;

import jakarta.servlet.http.HttpServletRequest;

public final class RequestIdUtils {

    public static final String REQUEST_ID_ATTRIBUTE = "requestId";
    public static final String REQUEST_ID_HEADER = "X-Request-Id";

    private RequestIdUtils() {
    }

    public static String getRequestId(HttpServletRequest request) {
        Object requestId = request.getAttribute(REQUEST_ID_ATTRIBUTE);
        return requestId == null ? "" : requestId.toString();
    }
}
