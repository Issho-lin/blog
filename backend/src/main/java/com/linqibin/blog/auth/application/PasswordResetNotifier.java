package com.linqibin.blog.auth.application;

public interface PasswordResetNotifier {

    void sendResetLink(String email, String resetUrl);
}
