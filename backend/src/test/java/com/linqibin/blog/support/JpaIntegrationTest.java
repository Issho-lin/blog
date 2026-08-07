package com.linqibin.blog.support;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

// JPA 集成测试统一入口：固定 profile 和数据库连接参数，避免每个类重复声明。
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@SpringBootTest
@ActiveProfiles("jpa")
@TestPropertySource(properties = {
        "DB_HOST=localhost",
        "DB_PORT=5432",
        "DB_NAME=blog",
        "DB_USER=blog",
        "DB_PASSWORD=blog"
})
public @interface JpaIntegrationTest {
}
