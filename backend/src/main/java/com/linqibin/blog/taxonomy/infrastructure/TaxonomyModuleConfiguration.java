package com.linqibin.blog.taxonomy.infrastructure;

import java.time.Clock;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.linqibin.blog.post.domain.SlugGenerator;
import com.linqibin.blog.taxonomy.application.CategoryService;
import com.linqibin.blog.taxonomy.application.TagService;
import com.linqibin.blog.taxonomy.domain.CategoryRepository;
import com.linqibin.blog.taxonomy.domain.TagRepository;
import com.linqibin.blog.taxonomy.infrastructure.persistence.CategoryEntityMapper;
import com.linqibin.blog.taxonomy.infrastructure.persistence.CategoryRepositoryAdapter;
import com.linqibin.blog.taxonomy.infrastructure.persistence.SpringDataCategoryRepository;
import com.linqibin.blog.taxonomy.infrastructure.persistence.SpringDataTagRepository;
import com.linqibin.blog.taxonomy.infrastructure.persistence.TagEntityMapper;
import com.linqibin.blog.taxonomy.infrastructure.persistence.TagRepositoryAdapter;

// 统一注册 taxonomy 模块需要的 Spring Bean，方便 web 层直接注入使用。
@Configuration
public class TaxonomyModuleConfiguration {

    @Bean
    @ConditionalOnProperty(
            prefix = "blog.taxonomy",
            name = "repository-type",
            havingValue = "in-memory",
            matchIfMissing = true
    )
    public InMemoryCategoryRepository inMemoryCategoryRepository() {
        // 默认走内存仓库，保证测试和本地开发行为不变。
        return new InMemoryCategoryRepository();
    }

    @Bean
    @ConditionalOnProperty(
            prefix = "blog.taxonomy",
            name = "repository-type",
            havingValue = "in-memory",
            matchIfMissing = true
    )
    public InMemoryTagRepository inMemoryTagRepository() {
        return new InMemoryTagRepository();
    }

    @Bean
    @ConditionalOnProperty(prefix = "blog.taxonomy", name = "repository-type", havingValue = "jpa")
    public CategoryEntityMapper categoryEntityMapper() {
        return new CategoryEntityMapper();
    }

    @Bean
    @ConditionalOnProperty(prefix = "blog.taxonomy", name = "repository-type", havingValue = "jpa")
    public TagEntityMapper tagEntityMapper() {
        return new TagEntityMapper();
    }

    @Bean
    @ConditionalOnProperty(prefix = "blog.taxonomy", name = "repository-type", havingValue = "jpa")
    public CategoryRepository jpaCategoryRepositoryAdapter(
            SpringDataCategoryRepository springDataCategoryRepository,
            CategoryEntityMapper categoryEntityMapper
    ) {
        return new CategoryRepositoryAdapter(springDataCategoryRepository, categoryEntityMapper);
    }

    @Bean
    @ConditionalOnProperty(prefix = "blog.taxonomy", name = "repository-type", havingValue = "jpa")
    public TagRepository jpaTagRepositoryAdapter(
            SpringDataTagRepository springDataTagRepository,
            TagEntityMapper tagEntityMapper
    ) {
        return new TagRepositoryAdapter(springDataTagRepository, tagEntityMapper);
    }

    @Bean
    public CategoryService categoryService(
            CategoryRepository categoryRepository,
            SlugGenerator slugGenerator,
            Clock clock
    ) {
        // CategoryService 依赖抽象仓库和通用 slug 规则，切换仓库时上层无感知。
        return new CategoryService(categoryRepository, slugGenerator, clock);
    }

    @Bean
    public TagService tagService(
            TagRepository tagRepository,
            SlugGenerator slugGenerator,
            Clock clock
    ) {
        return new TagService(tagRepository, slugGenerator, clock);
    }
}
