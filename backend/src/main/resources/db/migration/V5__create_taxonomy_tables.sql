-- 分类表：文章归属的一级分类，每篇文章最多关联一个分类。
CREATE TABLE categories (
    id           UUID PRIMARY KEY,
    name         VARCHAR(100) NOT NULL,
    slug         VARCHAR(120) NOT NULL,
    description  VARCHAR(500),
    created_at   TIMESTAMPTZ  NOT NULL,
    updated_at   TIMESTAMPTZ  NOT NULL,

    CONSTRAINT uk_categories_slug UNIQUE (slug)
);

-- 标签表：文章可关联多个标签，标签之间是多对多关系。
CREATE TABLE tags (
    id          UUID PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    slug        VARCHAR(120) NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL,
    updated_at  TIMESTAMPTZ  NOT NULL,

    CONSTRAINT uk_tags_slug UNIQUE (slug)
);

-- 文章-标签关联表：多对多关系，删除文章时级联删除关联。
CREATE TABLE post_tags (
    post_id  UUID NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
    tag_id   UUID NOT NULL REFERENCES tags(id)  ON DELETE CASCADE,

    CONSTRAINT pk_post_tags PRIMARY KEY (post_id, tag_id)
);

-- 文章表增加分类外键列，删除分类时置空。
ALTER TABLE posts ADD COLUMN category_id UUID REFERENCES categories(id) ON DELETE SET NULL;

-- 索引：按分类筛选文章。
CREATE INDEX idx_posts_category_id ON posts (category_id);
-- 索引：按标签反向查询文章列表。
CREATE INDEX idx_post_tags_tag_id ON post_tags (tag_id);
-- 索引：按标题搜索加速 ILIKE 查询。
CREATE INDEX idx_posts_title ON posts (title);
-- 索引：已发布文章按发布时间倒序的分页查询已在 V1 中覆盖。
