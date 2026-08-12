package com.linqibin.blog.common.api;

import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;
import tools.jackson.databind.json.JsonMapper;

import jakarta.servlet.http.HttpServletRequest;
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
        Class<?> parameterType = returnType.getParameterType();
        if (parameterType == byte[].class || parameterType == Byte[].class) {
            return false;
        }

        // ResponseEntity<byte[]> 这类下载接口也不进入统一 JSON 包装。
        if (ResponseEntity.class.isAssignableFrom(parameterType)) {
            Class<?> bodyType = resolveResponseEntityBodyType(returnType);
            return bodyType != byte[].class && bodyType != Byte[].class;
        }

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
        String requestId = "";
        if (request instanceof ServletServerHttpRequest servletServerHttpRequest) {
            HttpServletRequest servletRequest = servletServerHttpRequest.getServletRequest();
            requestId = RequestIdUtils.getRequestId(servletRequest);
            response.getHeaders().add(RequestIdUtils.REQUEST_ID_HEADER, requestId);
        }

        if (body instanceof ApiResponse<?>) {
            return body;
        }

        ApiResponse<Object> apiResponse = ApiResponse.success(body, requestId);
        if (body instanceof String) {
            return jsonMapper.writeValueAsString(apiResponse);
        }

        return apiResponse;
    }

    private Class<?> resolveResponseEntityBodyType(MethodParameter returnType) {
        return ResolvableType.forMethodParameter(returnType).as(ResponseEntity.class).getGeneric(0).resolve();
    }
}
