package com.linqibin.blog.auth.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.linqibin.blog.auth.application.AuthService;
import com.linqibin.blog.auth.domain.User;
import com.linqibin.blog.auth.exception.InvalidCredentialsException;

// 认证接口：提供登录、退出和当前用户查询。
// 使用 Spring Security 的 Session 机制，登录后 Cookie 中会写入 JSESSIONID。
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        // AuthService 校验邮箱和密码，校验失败时抛 InvalidCredentialsException。
        User user = authService.login(request.email(), request.password());

        // 登录成功后将认证信息写入 SecurityContext，Spring Security 会自动持久化到 Session。
        Authentication authentication = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                user, null, java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + user.role().name()))
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // 显式创建 Session，确保认证信息被持久化。
        HttpSession session = httpRequest.getSession(true);
        session.setAttribute("SPRING_SECURITY_CONTEXT", SecurityContextHolder.getContext());

        return AuthResponse.from(user);
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
