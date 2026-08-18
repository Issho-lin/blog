package com.linqibin.blog.comment.web;

import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.linqibin.blog.comment.application.CommentService;
import com.linqibin.blog.comment.domain.Comment;

@RestController
@RequestMapping("/api/public/posts/{slug}/comments")
public class PublicCommentController {

    private final CommentService commentService;

    public PublicCommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @GetMapping
    public List<CommentResponse> list(@PathVariable String slug) {
        return commentService.listPublishedPostComments(slug).stream()
                .map(CommentResponse::from)
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CommentResponse create(
            @PathVariable String slug,
            @Valid @RequestBody CreateCommentRequest request,
            HttpServletRequest httpRequest
    ) {
        Comment comment = commentService.addToPublishedPost(
                slug,
                request.authorName(),
                request.content(),
                clientIp(httpRequest)
        );
        return CommentResponse.from(comment);
    }

    private static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
