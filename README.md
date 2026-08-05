# 个人博客项目

这是个人博客项目的开发仓库。当前已完成第 1 周的后端初始化骨架，重点是把后端工程、基础接口和本地依赖服务先搭起来。

## 当前目录结构

```text
blog
├── backend                     Spring Boot 后端工程
├── frontend                    前端工程占位目录
├── docker-compose.yml          本地 PostgreSQL / Redis
├── 产品需求文档-个人博客.md
├── 架构设计与技术选型-个人博客.md
└── Java学习开发计划-个人博客.md
```

## 技术栈

- 后端：Java 17、Spring Boot 4.1、Maven Wrapper
- 数据库：PostgreSQL
- 缓存：Redis

## 已完成内容

- 使用 Spring Initializr 创建 `backend` 工程
- 增加 `GET /api/health` 健康检查接口
- 增加首个 Web 层 JUnit 测试
- 提供本地 `PostgreSQL` 和 `Redis` 的 `docker-compose.yml`
- 预留 `frontend` 目录，后续用于初始化前端项目

## 本地依赖启动

在仓库根目录执行：

```bash
docker compose up -d
```

默认端口：

- PostgreSQL：`5432`
- Redis：`6379`

默认账号：

- PostgreSQL database：`blog`
- PostgreSQL username：`blog`
- PostgreSQL password：`blog`

## 后端配置

后端默认读取以下环境变量；如果未设置，会使用本地开发默认值：

```text
DB_HOST=localhost
DB_PORT=5432
DB_NAME=blog
DB_USER=blog
DB_PASSWORD=blog
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=
REDIS_DATABASE=0
```

对应文件：`backend/src/main/resources/application.yaml`

## 数据库建表脚本

当前文章表的版本化 SQL 脚本位于：

```text
backend/src/main/resources/db/migration/V1__create_posts_table.sql
```

在本地 PostgreSQL 已启动后，可以手动执行：

```bash
psql "postgresql://blog:blog@localhost:5432/blog" -f backend/src/main/resources/db/migration/V1__create_posts_table.sql
```

如果使用了自定义环境变量，请将连接串替换成自己的数据库地址、用户名和密码。

## Flyway 与仓库模式

后端默认使用内存仓库：

```text
blog.post.repository-type=in-memory
spring.flyway.enabled=false
```

当需要切到 PostgreSQL + JPA 时，启用 `jpa` profile：

```bash
SPRING_PROFILES_ACTIVE=jpa
```

启用后会自动生效：

- `blog.post.repository-type=jpa`
- `spring.flyway.enabled=true`
- 启动时自动执行 `backend/src/main/resources/db/migration` 下尚未执行过的 migration
- JPA 使用 `ddl-auto=validate` 校验实体与表结构是否一致

## 后端启动方式

推荐直接通过 IDEA 打开 `backend` 工程后运行 `BackendApplication`。

如果使用命令行，可在 `backend` 目录下执行：

```bash
./mvnw spring-boot:run
```

## 测试

在 `backend` 目录下执行：

```bash
./mvnw test
```

当前已包含：

- `GET /api/health` 返回 `{"status":"UP"}` 的接口测试

## 下一步建议

1. 启动 PostgreSQL 和 Redis
2. 在 IDEA 中运行后端工程，确认 `/api/health`
3. 初始化前端工程
4. 开始补充统一响应结构、请求链路 ID 和文章领域模型
