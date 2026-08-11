package com.linqibin.blog.auth.domain;

// 用户角色：首期只有作者，后续扩展多用户时可拆分权限。
public enum UserRole {
    // 作者：可以创建、编辑、发布和管理文章。
    AUTHOR,
    // 超级管理员：首期与作者权限合并，后续可扩展更多管理能力。
    ADMIN
}
