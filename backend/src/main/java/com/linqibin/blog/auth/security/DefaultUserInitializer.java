package com.linqibin.blog.auth.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.linqibin.blog.auth.application.AuthService;
import com.linqibin.blog.auth.domain.User;

// 启动时自动初始化默认管理员账号，仅当仓库中还没有任何用户时执行。
@Component
public class DefaultUserInitializer {

    private static final Logger log = LoggerFactory.getLogger(DefaultUserInitializer.class);

    private final AuthService authService;

    @Value("${blog.auth.default-admin-email:admin@blog.com}")
    private String defaultEmail;

    @Value("${blog.auth.default-admin-password:admin123}")
    private String defaultPassword;

    @Value("${blog.auth.default-admin-name:Admin}")
    private String defaultName;

    public DefaultUserInitializer(AuthService authService) {
        this.authService = authService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void initializeDefaultAdmin() {
        User admin = authService.initializeDefaultAdmin(defaultEmail, defaultPassword, defaultName);
        if (admin != null) {
            log.info("默认管理员账号已初始化：{}", admin.email());
        }
    }
}
