# 登录功能并行开发契约

`api/openapi.yaml` 是字段名、类型、状态码和错误码的唯一事实来源；本文件只约定协作方式和实现行为。任何接口变更必须先修改 OpenAPI，再改前后端代码。

## 已冻结的产品边界

- 第一版支持用户名 + 密码注册和登录，不包含短信、第三方登录和找回密码；自助注册账号固定为 `OPERATOR`，不能由客户端指定角色。
- `accessToken` 使用 JWT，默认 15 分钟；`refreshToken` 使用服务端可撤销的不透明随机串，默认 7 天，并在每次刷新时轮换。
- 受保护请求使用 `Authorization: Bearer <accessToken>`。登录、刷新和退出不要求 access token。
- 密码只允许通过 HTTPS 传输，数据库仅保存 BCrypt/Argon2 哈希，日志不得记录密码或完整 token。
- 连续 5 次密码错误锁定账号 15 分钟；锁定返回 HTTP 423 和 `Retry-After`。

## 前后端共同约束

- JSON 字段统一 camelCase；时间统一 ISO 8601（带时区）；数据库 `BIGINT` ID 在 JSON 中必须是字符串。
- 成功响应直接返回业务对象，不额外包 `data`。错误统一为现有 `ErrorResponse`。
- 登录失败统一返回 `401 / INVALID_CREDENTIALS / 用户名或密码错误`，不能暴露账号是否存在。
- access token 缺失或无效返回 `401 / UNAUTHORIZED`；refresh token 失效返回 `401 / REFRESH_TOKEN_INVALID`；账号被禁用返回 `403 / ACCOUNT_DISABLED`。
- `logout` 必须幂等：refresh token 已失效时仍返回 204。
- 除登录失败输入框提示外，前端只依赖 `code` 做程序分支，`message` 仅用于展示。

## 前端实现边界

- 页面只调用 `authApi` 和 auth store，不直接调用 `uni.request`。
- access token 仅由请求层读取并添加；业务页面不得自行拼装 Authorization。
- 收到 401 时最多自动刷新一次；所有并发 401 共用同一个刷新 Promise。刷新成功后每个原请求最多重放一次，刷新失败则清空会话并跳转登录页，禁止循环重试。
- 登录按钮提交期间禁用；密码不得持久化。token 存储封装在独立模块，方便 H5 后续切换为 HttpOnly Cookie。
- `redirect` 只允许应用内以 `/` 开头的页面路径，避免开放重定向。

## 后端实现边界

- Spring Security 负责认证和路由保护；Controller 不手写解析 token。
- JWT 至少包含 `sub`（用户 ID）、`roles`、`iat`、`exp`、`jti`，不放密码、手机号等敏感信息。
- refresh token 入库前做不可逆哈希，记录用户、会话 ID、过期时间、撤销时间；轮换必须在同一事务内完成。
- 商品写接口要求 `ADMIN` 或 `OPERATOR`，商品读接口第一版保持公开；`/auth/me` 只要求已登录。
- 登录、刷新和退出写安全审计日志，但 token 只记录 `jti`/会话 ID。

## 联调验收

1. 正确密码登录得到契约规定的完整字段，随后 `/auth/me` 返回同一用户。
2. 错误密码返回统一错误，连续 5 次后返回 423。
3. access token 过期后，并发请求只触发一次 refresh，且原请求各重放一次。
4. 同一个旧 refresh token 第二次使用失败；新 refresh token 可用。
5. logout 调用两次均为 204，退出后 refresh 失败。
6. 未登录访问受保护写接口返回统一 401；权限不足返回 403。
