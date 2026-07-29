package com.linqibin.blog.common.request;

import java.io.IOException;
import java.util.UUID;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class RequestIdFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String requestId = request.getHeader(RequestIdUtils.REQUEST_ID_HEADER);
        if (!StringUtils.hasText(requestId)) {
            requestId = UUID.randomUUID().toString();
        }

        request.setAttribute(RequestIdUtils.REQUEST_ID_ATTRIBUTE, requestId);
        response.setHeader(RequestIdUtils.REQUEST_ID_HEADER, requestId);
        MDC.put(RequestIdUtils.REQUEST_ID_ATTRIBUTE, requestId);

        try {
            filterChain.doFilter(request, response);
        }
        finally {
            MDC.remove(RequestIdUtils.REQUEST_ID_ATTRIBUTE);
        }
    }
}
