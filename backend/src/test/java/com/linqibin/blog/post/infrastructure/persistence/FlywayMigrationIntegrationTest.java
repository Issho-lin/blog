package com.linqibin.blog.post.infrastructure.persistence;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import com.linqibin.blog.support.AbstractJpaIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;

// 验证 jpa profile 启动时 Flyway 会把全部 migration 应用到 PostgreSQL。
class FlywayMigrationIntegrationTest extends AbstractJpaIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void flywayAppliesAllMigrationsOnStartup() {
        Integer migrationCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE success = true",
                Integer.class
        );

        assertThat(migrationCount).isGreaterThanOrEqualTo(5);

        assertThat(tableExists("posts")).isTrue();
        assertThat(tableExists("users")).isTrue();
        assertThat(tableExists("categories")).isTrue();
        assertThat(tableExists("tags")).isTrue();
        assertThat(columnExists("posts", "version")).isTrue();
        assertThat(columnExists("posts", "view_count")).isTrue();
        assertThat(columnExists("posts", "excerpt")).isTrue();
        assertThat(columnExists("posts", "cover_url")).isTrue();
    }

    private boolean tableExists(String tableName) {
        Integer count = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*)
                        FROM information_schema.tables
                        WHERE table_schema = 'public' AND table_name = ?
                        """,
                Integer.class,
                tableName
        );
        return count != null && count > 0;
    }

    private boolean columnExists(String tableName, String columnName) {
        Integer count = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*)
                        FROM information_schema.columns
                        WHERE table_schema = 'public' AND table_name = ? AND column_name = ?
                        """,
                Integer.class,
                tableName,
                columnName
        );
        return count != null && count > 0;
    }
}
