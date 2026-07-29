package com.linqibin.blog.common.api;

import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;
import tools.jackson.databind.json.JsonMapper;

import com.linqibin.blog.common.request.RequestIdUtils;

@RestControllerAdvice
public class ApiResponseAdvice implements ResponseBodyAdvice<Object> {

    private final JsonMapper jsonMapper;

    public ApiResponseAdvice(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    @Override
    public boolean supports(
            MethodParameter returnType,
            Class<? extends HttpMessageConverter<?>> converterType
    ) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(
            Object body,
            MethodParameter returnType,
            MediaType selectedContentType,
            Class<? extends HttpMessageConverter<?>> selectedConverterType,
            ServerHttpRequest request,
            ServerHttpResponse response
    ) {
        if (body instanceof ApiResponse<?>) {
            return body;
        }

        String requestId = "";
        if (request instanceof ServletServerHttpRequest servletServerHttpRequest) {
            requestId = RequestIdUtils.getRequestId(servletServerHttpRequest.getServletRequest());
        }

        ApiResponse<Object> apiResponse = ApiResponse.success(body, requestId);
        if (body instanceof String) {
            return jsonMapper.writeValueAsString(apiResponse);
        }

        return apiResponse;
    }
}
