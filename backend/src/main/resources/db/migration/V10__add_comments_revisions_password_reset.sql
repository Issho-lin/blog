-- 文章评论：访客留言，仅已发布文章可写；管理端可删除。
CREATE TABLE comments (
    id          UUID PRIMARY KEY,
    post_id     UUID NOT NULL,
    author_name VARCHAR(40) NOT NULL,
    content     VARCHAR(2000) NOT NULL,
    ip          VARCHAR(64),
    created_at  TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_comments_post_created_at ON comments (post_id, created_at DESC);
CREATE INDEX idx_comments_created_at ON comments (created_at DESC);

-- 文章版本快照：自动保存合并窗口内只保留一份 AUTO；发布单独留点。
CREATE TABLE post_revisions (
    id               UUID PRIMARY KEY,
    post_id          UUID NOT NULL,
    title            VARCHAR(200) NOT NULL,
    markdown_content TEXT NOT NULL,
    excerpt          VARCHAR(500),
    kind             VARCHAR(20) NOT NULL,
    created_at       TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_post_revisions_kind CHECK (kind IN ('AUTO', 'PUBLISH', 'RESTORE'))
);

CREATE INDEX idx_post_revisions_post_created_at ON post_revisions (post_id, created_at DESC);

-- 找回密码令牌：只存哈希，明文只出现在邮件或开发日志。
CREATE TABLE password_reset_tokens (
    id          UUID PRIMARY KEY,
    user_id     UUID NOT NULL,
    token_hash  VARCHAR(64) NOT NULL,
    expires_at  TIMESTAMPTZ NOT NULL,
    used_at     TIMESTAMPTZ,
    CONSTRAINT uk_password_reset_tokens_hash UNIQUE (token_hash)
);

CREATE INDEX idx_password_reset_tokens_user_id ON password_reset_tokens (user_id);
