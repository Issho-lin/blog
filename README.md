# 个人博客项目

后端已完成阶段 0-10 核心能力，并开始阶段 11：Docker 部署。

## 当前目录结构

```text
blog
├── backend                     Spring Boot 后端工程（含 Dockerfile）
├── frontend                    前端工程占位目录
├── docker-compose.yml          PostgreSQL / Redis / 后端应用
├── .env.example                环境变量示例
├── 产品需求文档-个人博客.md
├── 架构设计与技术选型-个人博客.md
└── Java学习开发计划-个人博客.md
```

## 技术栈

- 后端：Java 17、Spring Boot 4.1、Maven Wrapper
- 数据库：PostgreSQL + Flyway
- 缓存：Redis（Session / 限流预留）
- 安全：Spring Security + HttpOnly Cookie Session
- 部署：Docker Compose

## 已完成内容

- 文章 CRUD、发布/下线、公开阅读与归档 API
- Markdown 预览、HTML 清洗、Front Matter 导入导出
- 分类 / 标签、图片上传、作者登录与权限保护
- 统一错误响应、请求链路 ID、Bean Validation
- 单元测试与集成测试覆盖核心业务与安全链路
- 多阶段 Dockerfile，以及 Compose 一键启动后端 + 数据库

## 一键启动（推荐）

复制环境变量模板：

```bash
cp .env.example .env
```

**日常开发**（只起数据库和 Redis，后端用 IDEA 运行）：

```bash
docker compose up -d
```

**整套容器启动**（含后端镜像构建）：

```bash
docker compose --profile app up -d --build
```

若构建时报 `auth.docker.io` / `i/o timeout`，说明访问 Docker Hub 不稳定。在 `.env` 中启用镜像加速地址，例如：

```text
JDK_IMAGE=docker.m.daocloud.io/library/eclipse-temurin:17-jdk-alpine
JRE_IMAGE=docker.m.daocloud.io/library/eclipse-temurin:17-jre-alpine
POSTGRES_IMAGE=docker.m.daocloud.io/library/postgres:17-alpine
REDIS_IMAGE=docker.m.daocloud.io/library/redis:7-alpine
```

也可在 Docker Desktop → Settings → Docker Engine 增加 `registry-mirrors`。

访问：

- 健康检查：`http://localhost:8080/api/health`
- PostgreSQL：`5432`
- Redis：`6379`

默认管理员（可用 `.env` 覆盖）：

- 邮箱：`admin@blog.com`
- 密码：`admin123`

查看日志：

```bash
docker compose logs -f backend
```

停止：

```bash
docker compose down
```

上传图片保存在 Docker volume `backend_uploads` 中，容器重建后仍会保留。

## 仅启动依赖（本地 IDEA 开发）

只起数据库和 Redis：

```bash
docker compose up -d postgres redis
```

然后在 IDEA 运行 `BackendApplication`，或：

```bash
cd backend
SPRING_PROFILES_ACTIVE=jpa ./mvnw spring-boot:run
```

不设 `jpa` profile 时默认使用内存仓库，适合快速联调接口。

## 后端配置

主要环境变量：

```text
DB_HOST / DB_PORT / DB_NAME / DB_USER / DB_PASSWORD
REDIS_HOST / REDIS_PORT
ADMIN_EMAIL / ADMIN_PASSWORD / ADMIN_NAME
CORS_ALLOWED_ORIGINS
MEDIA_UPLOAD_DIR
SPRING_PROFILES_ACTIVE=jpa
```

对应文件：`backend/src/main/resources/application.yaml`  
JPA 模式覆盖：`backend/src/main/resources/application-jpa.yaml`

## Flyway 与仓库模式

默认：

```text
blog.*.repository-type=in-memory
spring.flyway.enabled=false
```

启用 `jpa` profile 后：

- `post` / `auth` / `taxonomy` 全部切到 JPA
- Flyway 自动执行 `backend/src/main/resources/db/migration`
- Hibernate `ddl-auto=validate`

## 测试与质量检查

```bash
cd backend
./mvnw test
./mvnw validate
```

- `./mvnw test`：单元测试 + 接口集成测试
- `./mvnw validate`：Enforcer 检查 JDK >= 17
- 仓库根目录有 `.editorconfig`，统一缩进与换行

JPA 相关测试需要本地 PostgreSQL 已启动。

## 数据库备份与恢复（简版）

备份：

```bash
docker compose exec postgres pg_dump -U blog blog > backup.sql
```

恢复：

```bash
cat backup.sql | docker compose exec -T postgres psql -U blog blog
```

## 下一步建议

1. 初始化 Next.js 前端，完成公开站与后台联调
2. 补齐生产 HTTPS、域名与更完整的备份演练
3. 按需继续重构重复业务代码
