package com.linqibin.blog.auth.infrastructure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.linqibin.blog.auth.application.PasswordResetNotifier;

public class LoggingPasswordResetNotifier implements PasswordResetNotifier {

    private static final Logger log = LoggerFactory.getLogger(LoggingPasswordResetNotifier.class);

    private volatile String lastResetUrl;

    @Override
    public void sendResetLink(String email, String resetUrl) {
        this.lastResetUrl = resetUrl;
        log.info("密码重置链接（未配置邮件发送）：email={} url={}", email, resetUrl);
    }

    public String lastResetUrl() {
        return lastResetUrl;
    }
}
