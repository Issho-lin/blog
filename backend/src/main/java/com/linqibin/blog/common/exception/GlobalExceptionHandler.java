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

import com.linqibin.blog.auth.exception.InvalidCredentialsException;
import com.linqibin.blog.common.api.ApiResponse;
import com.linqibin.blog.common.request.RequestIdUtils;
import com.linqibin.blog.post.domain.Post;
import com.linqibin.blog.post.exception.ConcurrentPostModificationException;
import com.linqibin.blog.post.exception.DuplicateSlugException;
import com.linqibin.blog.post.exception.InvalidPostStateTransitionException;
import com.linqibin.blog.post.exception.PostNotFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidCredentials(
            InvalidCredentialsException exception,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                ApiResponse.error(
                        "INVALID_CREDENTIALS",
                        exception.getMessage(),
                        null,
                        RequestIdUtils.getRequestId(request)
                )
        );
    }

    @ExceptionHandler(PostNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handlePostNotFound(
            PostNotFoundException exception,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                ApiResponse.error(
                        "POST_NOT_FOUND",
                        exception.getMessage(),
                        null,
                        RequestIdUtils.getRequestId(request)
                )
        );
    }

    @ExceptionHandler({
            DuplicateSlugException.class,
            InvalidPostStateTransitionException.class
    })
    public ResponseEntity<ApiResponse<Void>> handlePostConflict(
            RuntimeException exception,
            HttpServletRequest request
    ) {
        String errorCode;
        if (exception instanceof DuplicateSlugException) {
            errorCode = "DUPLICATE_SLUG";
        } else {
            errorCode = "INVALID_POST_STATE_TRANSITION";
        }

        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                ApiResponse.error(
                        errorCode,
                        exception.getMessage(),
                        null,
                        RequestIdUtils.getRequestId(request)
                )
        );
    }

    @ExceptionHandler(ConcurrentPostModificationException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleConcurrentModification(
            ConcurrentPostModificationException exception,
            HttpServletRequest request
    ) {
        // 返回服务端当前文章数据，让前端对比后决定覆盖、保留本地内容或放弃修改。
        Map<String, Object> errorData = new LinkedHashMap<>();
        errorData.put("expectedVersion", exception.getExpectedVersion());
        errorData.put("actualVersion", exception.getActualVersion());

        if (exception.getCurrentPost() != null) {
            Post currentPost = exception.getCurrentPost();
            Map<String, Object> currentPostData = new LinkedHashMap<>();
            currentPostData.put("id", currentPost.id());
            currentPostData.put("title", currentPost.title());
            currentPostData.put("slug", currentPost.slug());
            currentPostData.put("markdownContent", currentPost.markdownContent());
            currentPostData.put("status", currentPost.status());
            currentPostData.put("version", currentPost.version());
            currentPostData.put("updatedAt", currentPost.updatedAt());
            errorData.put("currentPost", currentPostData);
        }

        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                ApiResponse.error(
                        "CONCURRENT_MODIFICATION",
                        exception.getMessage(),
                        errorData,
                        RequestIdUtils.getRequestId(request)
                )
        );
    }

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
