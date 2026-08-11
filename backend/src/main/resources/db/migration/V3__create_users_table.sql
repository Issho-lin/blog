-- 用户表：存储作者账号信息，密码以 BCrypt 哈希存储，禁止明文。
CREATE TABLE users (
    id              UUID PRIMARY KEY,
    email           VARCHAR(200) NOT NULL,
    password_hash   VARCHAR(200) NOT NULL,
    display_name    VARCHAR(100) NOT NULL,
    role            VARCHAR(20)  NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL,
    updated_at      TIMESTAMPTZ  NOT NULL,
    last_login_at   TIMESTAMPTZ,

    CONSTRAINT uk_users_email UNIQUE (email)
);

-- 约束角色枚举值，与 UserRole 枚举保持一致。
ALTER TABLE users
    ADD CONSTRAINT chk_users_role CHECK (role IN ('AUTHOR', 'ADMIN'));
