# AI Mall 管理后台

基于 React、TypeScript、Vite 与 Ant Design 的轻量商城管理后台。

## 第一阶段范围

- 管理员登录、登录态持久化、退出登录
- 商品分页查询与名称/SKU 搜索
- 按上下架状态筛选
- 商品新增、查看、编辑、删除
- 401 自动尝试刷新令牌，刷新失败返回登录页

## 技术方案

- React 18 + TypeScript
- Vite 5
- Ant Design 5
- React Router 6
- Axios

## 页面结构

```text
/login             登录页（公开）
/                  重定向到 /products
/products          商品管理（需要登录）
```

商品新增和编辑使用抽屉表单，详情也在抽屉中展示，以减少页面层级。

## 接口契约

后台复用仓库中的 `api/openapi.yaml`：

- `POST /api/v1/auth/login`
- `POST /api/v1/auth/refresh`
- `POST /api/v1/auth/logout`
- `GET /api/v1/auth/me`
- `GET/POST /api/v1/products`
- `GET/PUT/DELETE /api/v1/products/{id}`

开发服务器把 `/api` 代理到 `http://localhost:8080`。

## 目录说明

```text
src/
  api/          接口请求
  components/   通用组件
  layouts/      后台布局
  pages/        登录与商品页面
  types/        TypeScript 类型
  utils/        登录态与 Axios 封装
```

## 本地启动

```bash
npm install
npm run dev
```

默认地址为 `http://localhost:5174`。启动前请先运行仓库中的 Spring Boot 后端。

## 后续扩展

可按实际业务逐步增加用户管理、订单管理、角色权限、图片上传、操作审计和仪表盘；本阶段不提前引入复杂状态管理与权限框架。
