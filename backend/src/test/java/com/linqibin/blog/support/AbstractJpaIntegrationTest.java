package com.linqibin.blog.support;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;

import com.linqibin.blog.post.infrastructure.persistence.SpringDataPostRepository;

// 所有 JPA 集成测试共用的清理逻辑，确保每个测试方法都从空表开始。
public abstract class AbstractJpaIntegrationTest {

    @Autowired
    protected SpringDataPostRepository springDataPostRepository;

    @BeforeEach
    void cleanDatabase() {
        springDataPostRepository.deleteAll();
    }
}
