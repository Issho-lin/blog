# 个人博客项目

这是个人博客项目的开发仓库。后端已完成阶段 0-11 核心能力，前端已初始化并完成公开阅读与管理登录联调切片。

## 当前目录结构

```text
blog
├── backend                     Spring Boot 后端工程（含 Dockerfile）
├── frontend                    Next.js 公开站 + 管理端
├── docker-compose.yml          PostgreSQL / Redis / 后端应用
├── package.json                根目录编排：一键启动前后端
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
- Next.js 前端：公开首页、文章详情、管理登录与文章列表联调

## 一键启动（日常开发）

仓库是**混合技术栈的单仓**：前端（Next.js / pnpm）和后端（Spring Boot / Maven）在同一个 Git 仓库里，但不是纯前端那种 pnpm workspace。根目录 `package.json` 只负责把几件事串起来。

第一次：

```bash
cp .env.example .env
pnpm install
pnpm --dir frontend install
```

之后在仓库根目录：

```bash
pnpm dev
```

会依次：启动 PostgreSQL / Redis → 同时跑后端（8080）和前端（3000）。在该终端按 `Ctrl+C` 会停掉前后端进程。数据库容器仍会在后台运行，需要关掉时执行：

```bash
pnpm stop
```

只起其中一端：

```bash
pnpm dev:backend
pnpm dev:frontend
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

## 前端启动

```bash
cd frontend
pnpm install
pnpm dev
```

打开 `http://localhost:3000`。前端会把 `/api` 代理到 `http://localhost:8080`。

已实现：公开首页、文章详情、管理登录、文章列表（新建草稿 / 发布）。

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

待办清单见 [docs/待办功能.md](docs/待办功能.md)。当前优先：站点设置与关于页。
