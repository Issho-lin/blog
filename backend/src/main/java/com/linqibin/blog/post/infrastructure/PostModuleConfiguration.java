package com.linqibin.blog.post.infrastructure;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.linqibin.blog.post.application.PostService;
import com.linqibin.blog.post.domain.PostRepository;
import com.linqibin.blog.post.domain.SlugGenerator;

// 统一注册 post 模块需要的 Spring Bean，方便 web 层直接注入使用。
@Configuration
public class PostModuleConfiguration {

    @Bean
    public InMemoryPostRepository inMemoryPostRepository() {
        return new InMemoryPostRepository();
    }

    @Bean
    public PostRepository postRepository(InMemoryPostRepository inMemoryPostRepository) {
        return inMemoryPostRepository;
    }

    @Bean
    public SlugGenerator slugGenerator() {
        return new SlugGenerator();
    }

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    public PostService postService(PostRepository postRepository, SlugGenerator slugGenerator, Clock clock) {
        // PostService 依赖抽象仓库与 slug 规则，后续切数据库时这里只需要替换仓库实现。
        return new PostService(postRepository, slugGenerator, clock);
    }
}
