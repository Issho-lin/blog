package com.linqibin.blog.comment.infrastructure;

import java.time.Clock;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.linqibin.blog.comment.application.CommentRateLimiter;
import com.linqibin.blog.comment.application.CommentService;
import com.linqibin.blog.comment.domain.CommentRepository;
import com.linqibin.blog.comment.infrastructure.persistence.CommentEntityMapper;
import com.linqibin.blog.comment.infrastructure.persistence.CommentRepositoryAdapter;
import com.linqibin.blog.comment.infrastructure.persistence.SpringDataCommentRepository;
import com.linqibin.blog.post.application.PostService;

@Configuration
public class CommentModuleConfiguration {

    @Bean
    @ConditionalOnProperty(
            prefix = "blog.comment",
            name = "repository-type",
            havingValue = "in-memory",
            matchIfMissing = true
    )
    public InMemoryCommentRepository inMemoryCommentRepository() {
        return new InMemoryCommentRepository();
    }

    @Bean
    @ConditionalOnProperty(prefix = "blog.comment", name = "repository-type", havingValue = "jpa")
    public CommentEntityMapper commentEntityMapper() {
        return new CommentEntityMapper();
    }

    @Bean
    @ConditionalOnProperty(prefix = "blog.comment", name = "repository-type", havingValue = "jpa")
    public CommentRepository jpaCommentRepositoryAdapter(
            SpringDataCommentRepository springDataCommentRepository,
            CommentEntityMapper commentEntityMapper
    ) {
        return new CommentRepositoryAdapter(springDataCommentRepository, commentEntityMapper);
    }

    @Bean
    public CommentService commentService(
            CommentRepository commentRepository,
            PostService postService,
            CommentRateLimiter rateLimiter,
            Clock clock
    ) {
        return new CommentService(commentRepository, postService, rateLimiter, clock);
    }
}
