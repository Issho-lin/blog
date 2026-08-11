-- 给 posts 表增加阅读数字段，用于统计文章被访问的次数。
ALTER TABLE posts ADD COLUMN view_count BIGINT NOT NULL DEFAULT 0;
