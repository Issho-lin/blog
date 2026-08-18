package com.linqibin.blog.auth.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.linqibin.blog.auth.application.AuthService;
import com.linqibin.blog.auth.application.PasswordResetService;
import com.linqibin.blog.auth.domain.User;
import com.linqibin.blog.auth.exception.InvalidCredentialsException;

// 认证接口：提供登录、退出和当前用户查询。
// 使用 Spring Security 的 Session 机制，登录后 Cookie 中会写入 JSESSIONID。
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;
    private final SecurityContextRepository securityContextRepository =
            new HttpSessionSecurityContextRepository();

    public AuthController(AuthService authService, PasswordResetService passwordResetService) {
        this.authService = authService;
        this.passwordResetService = passwordResetService;
    }

    @PostMapping("/login")
    public AuthResponse login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        User user = authService.login(request.email(), request.password());

        Authentication authentication = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                user, null, java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + user.role().name()))
        );
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, httpRequest, httpResponse);

        return AuthResponse.from(user);
    }

    @PostMapping("/forgot-password")
    public MessageResponse forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        return new MessageResponse(passwordResetService.requestReset(request.email()));
    }

    @PostMapping("/reset-password")
    public MessageResponse resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.resetPassword(request.token(), request.newPassword());
        return new MessageResponse("密码已重置，请使用新密码登录");
    }

    @PutMapping("/password")
    public MessageResponse changePassword(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        if (user == null) {
            throw new InvalidCredentialsException("未登录");
        }
        authService.changePassword(user, request.currentPassword(), request.newPassword());
        return new MessageResponse("密码已更新");
    }

    @PostMapping("/logout")
    public void logout(HttpServletRequest httpRequest) {
        // 清除 SecurityContext 并使 Session 失效。
        SecurityContextHolder.clearContext();
        HttpSession session = httpRequest.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }

    @GetMapping("/me")
    public AuthResponse currentUser(@AuthenticationPrincipal User user) {
        // 从 SecurityContext 中取出当前登录用户，未登录时 Spring Security 会先返回 401。
        if (user == null) {
            throw new InvalidCredentialsException("未登录");
        }
        return AuthResponse.from(user);
    }
}
