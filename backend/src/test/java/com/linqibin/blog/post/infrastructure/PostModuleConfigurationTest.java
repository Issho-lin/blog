package com.linqibin.blog.post.infrastructure;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.linqibin.blog.markdown.exporter.FrontMatterExporter;
import com.linqibin.blog.markdown.parser.FrontMatterParser;
import com.linqibin.blog.post.domain.PostRepository;
import com.linqibin.blog.taxonomy.application.CategoryService;
import com.linqibin.blog.taxonomy.application.TagService;
import com.linqibin.blog.post.infrastructure.persistence.PostEntityMapper;
import com.linqibin.blog.post.infrastructure.persistence.PostRepositoryAdapter;
import com.linqibin.blog.post.infrastructure.persistence.SpringDataPostRepository;

import static org.assertj.core.api.Assertions.assertThat;

class PostModuleConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(PostModuleConfiguration.class, TestDependenciesConfiguration.class);

    @Test
    void defaultsToInMemoryRepository() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(InMemoryPostRepository.class);
            assertThat(context).hasSingleBean(PostRepository.class);
            assertThat(context.getBean(PostRepository.class)).isInstanceOf(InMemoryPostRepository.class);
            assertThat(context).doesNotHaveBean(PostEntityMapper.class);
            assertThat(context).doesNotHaveBean(PostRepositoryAdapter.class);
        });
    }

    @Test
    void switchesToJpaRepositoryWhenPropertyIsJpa() {
        contextRunner
                .withUserConfiguration(JpaRepositoryTestConfiguration.class)
                .withPropertyValues("blog.post.repository-type=jpa")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(InMemoryPostRepository.class);
                    assertThat(context).hasSingleBean(PostEntityMapper.class);
                    assertThat(context).hasSingleBean(SpringDataPostRepository.class);
                    assertThat(context).hasSingleBean(PostRepository.class);
                    assertThat(context.getBean(PostRepository.class)).isInstanceOf(PostRepositoryAdapter.class);
                });
    }

    @Configuration(proxyBeanMethods = false)
    static class TestDependenciesConfiguration {

        // PostImportExportService 依赖的分类和标签服务来自 TaxonomyModuleConfiguration，
        // 此处提供 Mock bean 保证配置测试可以独立加载。
        @Bean
        CategoryService categoryService() {
            return Mockito.mock(CategoryService.class);
        }

        @Bean
        TagService tagService() {
            return Mockito.mock(TagService.class);
        }

        @Bean
        FrontMatterParser frontMatterParser() {
            return new FrontMatterParser();
        }

        @Bean
        FrontMatterExporter frontMatterExporter() {
            return new FrontMatterExporter();
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class JpaRepositoryTestConfiguration {

        @Bean
        SpringDataPostRepository springDataPostRepository() {
            return Mockito.mock(SpringDataPostRepository.class);
        }
    }
}
