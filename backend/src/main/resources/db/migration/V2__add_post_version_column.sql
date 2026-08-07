-- 阶段 5：增加 version 列用于乐观锁并发冲突检测。
-- 新建文章的默认版本号为 0，每次更新递增。
ALTER TABLE posts ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
