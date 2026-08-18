package com.linqibin.blog.comment.web;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.linqibin.blog.comment.application.CommentService;
import com.linqibin.blog.comment.domain.Comment;
import com.linqibin.blog.post.application.PostService;
import com.linqibin.blog.post.domain.Post;
import com.linqibin.blog.post.exception.PostNotFoundException;

@RestController
@RequestMapping("/api/admin/comments")
public class AdminCommentController {

    private final CommentService commentService;
    private final PostService postService;

    public AdminCommentController(CommentService commentService, PostService postService) {
        this.commentService = commentService;
        this.postService = postService;
    }

    @GetMapping
    public List<AdminCommentResponse> list() {
        List<Comment> comments = commentService.listAllForAdmin();
        List<AdminCommentResponse> items = new ArrayList<>();
        for (Comment comment : comments) {
            try {
                Post post = postService.getPost(comment.postId());
                items.add(AdminCommentResponse.from(comment, post));
            } catch (PostNotFoundException ignored) {
                items.add(new AdminCommentResponse(
                        comment.id(),
                        comment.postId(),
                        "已删除的文章",
                        "",
                        comment.authorName(),
                        comment.content(),
                        comment.createdAt()
                ));
            }
        }
        return items;
    }

    @DeleteMapping("/{commentId}")
    public void delete(@PathVariable UUID commentId) {
        commentService.delete(commentId);
    }
}
