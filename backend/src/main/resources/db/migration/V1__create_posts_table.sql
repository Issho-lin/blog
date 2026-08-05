CREATE TABLE posts (
    id UUID PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    slug VARCHAR(120) NOT NULL,
    markdown_content TEXT NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    published_at TIMESTAMPTZ,
    previous_status_before_trash VARCHAR(20),
    CONSTRAINT uk_posts_slug UNIQUE (slug),
    CONSTRAINT chk_posts_status CHECK (status IN ('DRAFT', 'PUBLISHED', 'UNPUBLISHED', 'TRASHED')),
    CONSTRAINT chk_posts_previous_status_before_trash CHECK (
        previous_status_before_trash IS NULL
        OR previous_status_before_trash IN ('DRAFT', 'UNPUBLISHED')
    )
);

CREATE INDEX idx_posts_updated_at ON posts (updated_at DESC);
