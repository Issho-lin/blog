package com.linqibin.blog.site.application;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.linqibin.blog.site.infrastructure.InMemorySiteSettingsRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SiteSettingsServiceTest {

    private InMemorySiteSettingsRepository repository;
    private SiteSettingsService service;

    @BeforeEach
    void setUp() {
        repository = new InMemorySiteSettingsRepository();
        service = new SiteSettingsService(
                repository,
                Clock.fixed(Instant.parse("2026-08-14T08:00:00Z"), ZoneOffset.UTC)
        );
    }

    @Test
    void getReturnsDefaultSettings() {
        assertEquals("Linqibin Blog", service.get().siteName());
        assertEquals(20, service.get().postsPerPage());
    }

    @Test
    void updatePersistsFields() {
        var updated = service.update(
                "新站名",
                "副标题",
                "简介",
                "作者",
                "/uploads/avatar.png",
                "# 关于我",
                12,
                "Asia/Shanghai",
                "zh-CN",
                "/favicon.ico",
                "/og.png"
        );

        assertEquals("新站名", updated.siteName());
        assertEquals(12, updated.postsPerPage());
        assertEquals("# 关于我", repository.get().aboutMarkdown());
        assertEquals(Instant.parse("2026-08-14T08:00:00Z"), updated.updatedAt());
    }

    @Test
    void updateRejectsBlankName() {
        assertThrows(IllegalArgumentException.class, () -> service.update(
                "  ",
                "",
                "",
                "",
                "",
                "",
                10,
                "Asia/Shanghai",
                "zh-CN",
                "",
                ""
        ));
    }
}
