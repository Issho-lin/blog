package com.linqibin.blog.common.exception;

import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.MissingServletRequestParameterException;

import com.linqibin.blog.common.api.ApiResponse;
import com.linqibin.blog.common.request.RequestIdUtils;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError fieldError : exception.getBindingResult().getFieldErrors()) {
            fieldErrors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }

        Map<String, Object> errorData = new LinkedHashMap<>();
        errorData.put("fieldErrors", fieldErrors);

        return ResponseEntity.badRequest().body(
                ApiResponse.error(
                        "VALIDATION_ERROR",
                        "请求参数校验失败",
                        errorData,
                        RequestIdUtils.getRequestId(request)
                )
        );
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleConstraintViolation(
            ConstraintViolationException exception,
            HttpServletRequest request
    ) {
        Map<String, String> violations = new LinkedHashMap<>();
        exception.getConstraintViolations().forEach(violation ->
                violations.put(violation.getPropertyPath().toString(), violation.getMessage())
        );

        Map<String, Object> errorData = new LinkedHashMap<>();
        errorData.put("violations", violations);

        return ResponseEntity.badRequest().body(
                ApiResponse.error(
                        "VALIDATION_ERROR",
                        "请求参数校验失败",
                        errorData,
                        RequestIdUtils.getRequestId(request)
                )
        );
    }

    @ExceptionHandler({
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class,
            IllegalArgumentException.class
    })
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(
            Exception exception,
            HttpServletRequest request
    ) {
        return ResponseEntity.badRequest().body(
                ApiResponse.error(
                        "BAD_REQUEST",
                        exception.getMessage(),
                        null,
                        RequestIdUtils.getRequestId(request)
                )
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(
            Exception exception,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(
                        "INTERNAL_SERVER_ERROR",
                        "服务器内部错误",
                        null,
                        RequestIdUtils.getRequestId(request)
                )
        );
    }
}
