# 多阶段构建：先用 Maven 打包，再用精简 JRE 镜像运行。
# 若 Docker Hub 超时，构建时传入镜像加速地址，例如：
#   --build-arg JDK_IMAGE=docker.m.daocloud.io/library/eclipse-temurin:17-jdk-alpine
#   --build-arg JRE_IMAGE=docker.m.daocloud.io/library/eclipse-temurin:17-jre-alpine
ARG JDK_IMAGE=eclipse-temurin:17-jdk-alpine
ARG JRE_IMAGE=eclipse-temurin:17-jre-alpine

FROM ${JDK_IMAGE} AS build

WORKDIR /workspace

COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
RUN chmod +x mvnw \
    && ./mvnw -q -DskipTests dependency:go-offline

COPY src src
RUN ./mvnw -q -DskipTests package \
    && cp target/*.jar /workspace/app.jar

FROM ${JRE_IMAGE} AS runtime

RUN apk add --no-cache wget \
    && addgroup -S blog \
    && adduser -S blog -G blog

WORKDIR /app

COPY --from=build /workspace/app.jar /app/app.jar

RUN mkdir -p /app/uploads \
    && chown -R blog:blog /app

USER blog

ENV JAVA_OPTS="" \
    SPRING_PROFILES_ACTIVE=jpa \
    MEDIA_UPLOAD_DIR=/app/uploads

EXPOSE 8080

HEALTHCHECK --interval=15s --timeout=5s --start-period=40s --retries=5 \
  CMD wget -qO- http://127.0.0.1:8080/api/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
