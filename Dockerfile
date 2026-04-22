# 多阶段Docker构建 - 校园二手交易平台

# ============================================
# 第一阶段：前端构建 (Node.js)
# ============================================
FROM node:18-alpine AS frontend-build

WORKDIR /app/frontend

# 先复制package文件，利用Docker层缓存
COPY frontend/package.json frontend/package-lock.json* ./

# 安装依赖（使用npm ci确保一致性）
RUN npm install --legacy-peer-deps || npm install

# 复制前端源码
COPY frontend/ .

# 构建前端（添加详细日志）
RUN npm run build -- --logLevel error 2>&1 || (echo "=== FRONTEND BUILD FAILED ===" && cat /app/frontend/vite.config.js && exit 1)

# ============================================
# 第二阶段：后端构建 (Maven + Java 21)
# ============================================
FROM maven:3.9-eclipse-temurin-21 AS backend-build

WORKDIR /app/backend

# 先复制pom.xml，利用Docker层缓存依赖下载
COPY backend/pom.xml .

# 下载依赖（利用Docker缓存）
RUN mvn dependency:go-offline -B --no-transfer-progress || true

# 复制后端源码
COPY backend/src ./src

# 构建JAR包
RUN mvn clean package -DskipTests -B --no-transfer-progress

# ============================================
# 第三阶段：运行镜像 (JRE 21 Alpine)
# ============================================
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# 设置时区为上海时间
ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# 从前端构建阶段复制产物
COPY --from=frontend-build /app/frontend/dist ./static

# 从后端构建阶段复制JAR包
COPY --from=backend-build /app/backend/target/backend-0.0.1-SNAPSHOT.jar ./backend.jar

# 复制启动脚本并设置权限
COPY start.sh ./start.sh
RUN chmod +x ./start.sh

# 创建非root用户运行应用
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# 暴露端口
EXPOSE 8080

# 启动应用
CMD ["./start.sh"]
