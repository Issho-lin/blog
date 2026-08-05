package com.linqibin.blog.post.infrastructure;

import java.time.Clock;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.linqibin.blog.post.application.PostService;
import com.linqibin.blog.post.domain.PostRepository;
import com.linqibin.blog.post.domain.SlugGenerator;
import com.linqibin.blog.post.infrastructure.persistence.PostEntityMapper;
import com.linqibin.blog.post.infrastructure.persistence.PostRepositoryAdapter;
import com.linqibin.blog.post.infrastructure.persistence.SpringDataPostRepository;

// 统一注册 post 模块需要的 Spring Bean，方便 web 层直接注入使用。
@Configuration
public class PostModuleConfiguration {

    @Bean
    @ConditionalOnProperty(
            prefix = "blog.post",
            name = "repository-type",
            havingValue = "in-memory",
            matchIfMissing = true
    )
    public InMemoryPostRepository inMemoryPostRepository() {
        // 默认仍走内存仓库，保证当前测试和本地开发行为不变。
        return new InMemoryPostRepository();
    }

    @Bean
    @ConditionalOnProperty(prefix = "blog.post", name = "repository-type", havingValue = "jpa")
    public PostEntityMapper postEntityMapper() {
        // JPA 模式下显式注册翻译器，把领域对象和数据库实体隔离开。
        return new PostEntityMapper();
    }

    @Bean
    @ConditionalOnProperty(prefix = "blog.post", name = "repository-type", havingValue = "jpa")
    public PostRepository jpaPostRepositoryAdapter(
            SpringDataPostRepository springDataPostRepository,
            PostEntityMapper postEntityMapper
    ) {
        // JPA 模式下改由适配器承接领域仓库接口，上层仍只依赖 PostRepository。
        return new PostRepositoryAdapter(springDataPostRepository, postEntityMapper);
    }

    @Bean
    public SlugGenerator slugGenerator() {
        // slug 规则是模块级通用能力，集中注册成 Bean 复用。
        return new SlugGenerator();
    }

    @Bean
    public Clock clock() {
        // 用 Clock 注入时间，方便测试里固定当前时间。
        return Clock.systemUTC();
    }

    @Bean
    public PostService postService(PostRepository postRepository, SlugGenerator slugGenerator, Clock clock) {
        // PostService 依赖抽象仓库与 slug 规则，后续切数据库时这里只需要替换仓库实现。
        return new PostService(postRepository, slugGenerator, clock);
    }
}
