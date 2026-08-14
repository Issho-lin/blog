-- 全站设置：固定只存一行。
CREATE TABLE site_settings (
    id                       SMALLINT PRIMARY KEY,
    site_name                VARCHAR(100) NOT NULL,
    site_subtitle            VARCHAR(200),
    site_description         VARCHAR(500),
    author_name              VARCHAR(100),
    author_avatar_url        VARCHAR(500),
    about_markdown           TEXT,
    posts_per_page           INTEGER      NOT NULL,
    timezone                 VARCHAR(64)  NOT NULL,
    default_language         VARCHAR(16)  NOT NULL,
    favicon_url              VARCHAR(500),
    default_share_image_url  VARCHAR(500),
    updated_at               TIMESTAMPTZ  NOT NULL,

    CONSTRAINT chk_site_settings_singleton CHECK (id = 1),
    CONSTRAINT chk_site_settings_posts_per_page CHECK (posts_per_page BETWEEN 1 AND 100)
);

INSERT INTO site_settings (
    id,
    site_name,
    site_subtitle,
    site_description,
    author_name,
    author_avatar_url,
    about_markdown,
    posts_per_page,
    timezone,
    default_language,
    favicon_url,
    default_share_image_url,
    updated_at
) VALUES (
    1,
    'Linqibin Blog',
    '書齋 · 技術手稿',
    '记录技术学习与工程实践。写给自己，也留给路过的人。',
    '',
    '',
    E'这里是个人技术博客。\n\n记录学习、工程实践与写作。',
    20,
    'Asia/Shanghai',
    'zh-CN',
    '',
    '',
    TIMESTAMPTZ '2026-01-01 00:00:00+00'
);
