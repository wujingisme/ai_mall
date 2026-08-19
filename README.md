# AI Mall

第一版商城应用，只提供商品列表、详情、新增、编辑和删除。

## 目录

- `frontend`：UniApp + Vue 3 + TypeScript
- `backend`：Java 17 + Spring Boot 3 + MyBatis-Plus
- `api/openapi.yaml`：OpenAPI 3 接口契约
- `database/schema.sql`：MySQL 8 数据库结构

## 启动

1. 在 MySQL 8 执行 `database/schema.sql`。
2. 根据本机数据库修改 `backend/src/main/resources/application.yml`，运行 `mvn spring-boot:run`。
3. 进入 `frontend`，执行 `npm install`，然后执行 `npm run dev:h5`。

后端默认监听 `http://localhost:8080`，H5 开发服务器会将 `/api` 代理到后端。
