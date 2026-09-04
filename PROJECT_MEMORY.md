# AI Mall shared project memory

Last updated: 2026-09-04

This file is the shared, repository-local memory for all AI Mall tasks. Read it first when starting work in this repository and update it after material changes. Source code and `api/openapi.yaml` remain the final authority if anything here is stale.

## Project map

| Path | Purpose | Stack |
| --- | --- | --- |
| `frontend/` | Consumer application, including H5/app/mini-program targets | UniApp, Vue 3, TypeScript |
| `admin/` | Merchant/admin web console | React 18, TypeScript, Vite, Ant Design |
| `backend/` | REST API | Java 17, Spring Boot 3, Spring Security, MyBatis-Plus |
| `api/openapi.yaml` | Shared API contract | OpenAPI 3 |
| `database/schema.sql` | Database schema | MySQL 8 |

## Runtime and ports

- Backend: `http://localhost:8080`
- Admin dev server: `http://localhost:5174`; Vite proxies `/api` to `http://localhost:8080`.
- UniApp API destination is controlled by `frontend/.env.*`. As of 2026-08-31, `.env.development` has one active `VITE_API_BASE_URL=http://127.0.0.1:8080` and several commented examples, while `.env.production` points to `http://124.221.241.24:8081`. Confirm the active build mode before deciding which database received a write.
- A frontend message such as “无法连接服务器” normally means the backend is not listening on port 8080, backend startup failed (often database configuration), or the frontend was opened without its development proxy.

### Common commands

```powershell
# Backend
cd E:\ai_mall\backend
mvn spring-boot:run

# Backend tests (the repository-local Maven cache avoids restricted C:\.m2 access)
mvn "-Dmaven.repo.local=E:\ai_mall\.m2\repository" test

# React admin
cd E:\ai_mall\admin
npm install
npm run dev
npm run build

# UniApp H5
cd E:\ai_mall\frontend
npm install
npm run dev:h5
```

Backend startup requires a reachable MySQL database and the required environment/config values. Never record real database passwords or JWT secrets here.

## Deployment automation

- `.github/workflows/deploy-backend.yml` triggers on pushes to `main` that change `backend/`, builds/tests the Spring Boot JAR with Java 17, uploads it through SSH, and invokes the deployment script. It also supports `workflow_dispatch` for a manual run and serializes production deployments.
- `deploy/mall-backend-deploy.sh` is uploaded to `/www/wwwroot/mymall/backend/deploy/`; it keeps `backend.env` on the server, runs the JAR as `www`, keeps timestamped JAR backups, performs a product-list health check, and rolls back the previous JAR if startup fails. It assumes the current OpenCloudOS JDK path `/www/server/java/jdk-17.0.8/bin/java` unless `JAVA_BIN` is changed.
- The workflow requires GitHub Actions secrets `DEPLOY_HOST`, `DEPLOY_PORT`, `DEPLOY_USER`, `DEPLOY_PATH`, `DEPLOY_SSH_KEY`, and `DEPLOY_KNOWN_HOSTS`. No database password, JWT secret, WeChat AppSecret, or server environment file belongs in GitHub.
- One-time server preparation and the exact GitHub setup are documented in `公共知识/GitHub Actions自动部署.md`. The automation is repository-ready but is not active until the server preparation and GitHub secrets are completed; frontend/admin and WeChat publishing are outside this workflow.

## API and authentication

- Base API prefix: `/api/v1`.
- Public consumer product endpoints: `/api/v1/shop/products/**`.
- Admin product endpoints: `/api/v1/products/**`; require `ADMIN` or `OPERATOR` authority.
- Public auth endpoints include register, login, refresh, and logout.
- 微信小程序登录使用 `POST /api/v1/auth/wechat/login`：前端只提交 `uni.login` 的一次性 code，后端使用环境变量中的 AppID/AppSecret 调用微信 code2Session，以 OpenID 创建或复用 CUSTOMER 用户，再签发商城自己的 JWT/refresh token。AppID 可保存在 `frontend/src/manifest.json`；AppSecret 严禁进入前端或 Git 仓库。
- 微信 code2Session 适配层限长读取原始响应并用 Jackson 独立解析，不依赖外部 `Content-Type`；无效 code 映射为 401，上游不可用、未配置或非法响应映射为 503。日志只能记录脱敏的状态、异常类型或微信 errcode，不得记录 AppSecret、code、完整 URL、原始正文或外部 errmsg。
- `/api/v1/auth/me` requires a valid Bearer access token.
- Access tokens are signed JWTs and include a `roles` claim.
- Refresh tokens are opaque, stored as hashes, rotated on refresh, and revoked on logout.
- Admin Axios logic lives in `admin/src/utils/request.ts`; session storage lives in `admin/src/utils/auth.ts`.
- Admin API origin is configured by `admin/.env.development` and `admin/.env.production`; Vite development proxies `/api` to the configured development origin, while production uses the configured server origin and its `/api` reverse proxy.
- Learning and deployment reference documents are stored under `公共知识/` by default.
- UniApp request/session logic lives under `frontend/src/utils/`.
- Consumer startup now opens the public product home directly. Successful login defaults to home unless a safe internal redirect was supplied; public home/detail/profile routes remain browsable without login, while cart navigation requires a session and backend authorization remains authoritative.
- Frontend navigation policy is centralized in `frontend/src/utils/navigation.ts`. Session freshness and concurrent `/auth/me` deduplication are centralized in `frontend/src/utils/session-validation.ts`; the default runtime validation window is 60 seconds. Background session validation can clear stale credentials without forcing a public page to jump to login.

## Current UI direction

Both clients should share the same visual language:

- Deep forest green: approximately `#173f34`, `#245d4d`, `#28604f`.
- Warm gold accent: approximately `#e4bd6d` / `#e4c57d`.
- Soft gray-green page background: `#f3f6f4` / `#f6f7f4`.
- Primary text: `#17362d`; secondary text uses muted gray-green.
- Rounded cards, restrained green shadows, and clean product-focused layouts.

The React admin theme is configured in `admin/src/main.tsx`, with application CSS in `admin/src/styles.css`. Its login page intentionally mirrors the UniApp login page's two-panel brand treatment.

## Important implementation notes

- **代码注释约定**：以后新增或修改前后端代码都必须补充关键中文注释。前端至少说明组件/函数职责、数据流、状态变化、接口参数和非直观的边界处理；后端必须更详细地说明分层职责、请求链路、权限与数据归属、异常路径、事务/并发/幂等、数据库影响，以及关键实现为什么这样选择。注释应解释业务原因，避免只重复代码字面含义。

- **增量提交约定**：用户希望以后代码尽量拆成多次、小范围、可独立验证的本地 Git 提交。推荐按数据库/契约、后端业务、测试、前端页面或文档等边界拆分；每次提交前后说明变更范围和验证结果，默认不推送远程。提交应保持可理解、可回退，避免把多个无关功能塞进一个大提交。

- **文件路径约束**：后续任何新文件、缓存、构建产物、可视化文件和临时文件均不得写入 C 盘；默认使用 `E:\ai_mall` 或用户明确指定的其他非 C 盘路径。已存在的 C 盘文件不得未经确认删除。

- User background and explanation style: the user is primarily a frontend developer and is not yet familiar with databases, backend development, or operations. When discussing MySQL, Java/Spring, Linux, servers, deployment, Nginx, BaoTa, permissions, processes, ports, or similar topics, explain each new term in plain frontend-oriented language; state what the step is for, exactly where to perform it, what successful output looks like, and whether it can affect local or production services. Do not provide unexplained operations commands or assume backend/operations knowledge.
- Before making any code, configuration, API, database, build, or workflow change, present the proposed design to the user first and wait for explicit approval. The proposal should identify the intended behavior, affected modules/files, API and data semantics, compatibility or migration impact, verification plan, and the viable options with each option's advantages, disadvantages, risks, and suitable use cases. State a recommendation and its rationale, then implement only after the user confirms. Read-only diagnosis and explanation may proceed without approval.
- User confirmation workflow: after every new request that would create, modify, delete, move, install, configure, or execute something, first briefly restate the understood goal and intended action, then stop and wait for explicit confirmation such as “是的”, “确认”, or “可以”. Do not start the action merely because the request appears clear. Read-only explanations that require no action may be answered directly.
- Choose implementations from explicit API and data semantics, not merely the shortest framework helper. Before coding, consider correctness, null/empty/absent states, failure paths, security, maintainability, and likely extension points.
- Engineering priority is correctness and long-term design, not implementation convenience or line count. Before selecting a framework shortcut, explicitly evaluate maintainability, extensibility, observability, testability, protocol deviations, compatibility, and operational failure modes. For external services, treat status, headers, body, timeouts, malformed/non-standard responses, proxy behavior, sensitive logging, and future provider changes as first-class design concerns; isolate provider-specific adaptation instead of weakening application-wide behavior.
- A full-form product edit is a full replacement of editable fields: optional fields submitted empty must be persisted as SQL `NULL`. Do not use an ORM helper that silently ignores `null` unless that selective-update behavior is intentionally part of the endpoint contract.
- When a MyBatis-Plus lambda update wrapper is the appropriate design, construct it with `Wrappers.lambdaUpdate(Entity.class)` so the entity type is explicit and the style is consistent. This convention does not override choosing custom SQL or another persistence mechanism when that better matches the operation.
- For material behavior changes, add regression coverage for normal updates, clearing an existing optional value, validation/conflicts, and relevant authorization paths.
- User verification preference: after ordinary source-only edits, a quick compile/type check is allowed to catch syntax and type errors, but do not automatically perform lengthy test suites, full builds, dependency installation, or broad verification. Report exactly what was and was not checked. For authentication, authorization, payments, money calculations, database migrations, concurrency/data-integrity behavior, or other high-risk changes, explain any additional recommended verification first and let the user decide unless it was explicitly requested.

- React admin routes are defined in `admin/src/App.tsx`.
- Admin product CRUD UI is in `admin/src/pages/ProductsPage.tsx`.
- Product filtering uses explicit query overrides when search/reset changes page or filter state, avoiding stale React state in immediate requests.
- Product `PUT /api/v1/products/{id}` is a full replacement of all admin-editable fields. `ProductService` uses `Wrappers.lambdaUpdate(Product.class)` with an explicit `.set(...)` list so optional `imageUrl` and `description` values normalized to `null` are written as SQL `NULL`; do not replace this with default `updateById()` selective-update behavior.
- JWT parsing is implemented in `backend/src/main/java/com/aimall/auth/service/JwtService.java`.
- JWT request authentication is implemented under `backend/src/main/java/com/aimall/auth/security/` and registered by `SecurityConfig`.
- Keep consumer product reads on `/shop/products`; do not expose admin CRUD endpoints for consumer use.
- API shape changes must also update `api/openapi.yaml` and affected TypeScript types/clients.
- Coupon-template administration is under `/api/v1/admin/coupon-templates` and requires `SUPER_ADMIN`, `ADMIN`, or `OPERATOR`. Templates start as `DRAFT`; only drafts can be fully replaced, activation locks core rules, and deactivation is idempotent and does not define revocation of already-issued user coupons. The admin UI is at `/coupon-templates` and supports list filtering, draft create/edit, detail, activation, and deactivation. Phase 2 does not include user coupon, grant, share, claim, or order-redemption tables.
- Manual coupon issuance uses `POST /api/v1/admin/coupon-grants` with a globally unique idempotency key, a persisted `coupon_grant` audit row, and immutable rule snapshots in `user_coupon`. The same transaction conditionally reserves template inventory and checks per-user limits after acquiring the template-row update lock. Admin user selection uses the non-sensitive summary endpoint `GET /api/v1/admin/customers`; consumer ownership comes only from the JWT on `GET /api/v1/me/coupons[/{id}]`. Expiration is derived from `valid_until` rather than maintained as a scheduled database status.
- Admin user management is available at `/users`, backed by `/api/v1/admin/customers/manage`; it lists/searches CUSTOMER users, shows only a `wechatBound` boolean instead of OpenID, exposes coupon summaries, and supports activation/deactivation. All operations require an admin role and the backend verifies the target is a CUSTOMER.
- Coupon sharing/claiming is implemented as a separate extension: an available share-enabled user coupon creates a random token whose SHA-256 hash is stored in `coupon_share`; public resolve only shows a preview, while authenticated claim checks creator identity, active template, expiry, inventory, per-user limit, and a unique `(share_id, claimant_user_id)` claim record. Sharing does not reward the creator yet. Existing Batch 3 databases need the one-time `database/migrations/20260831_coupon_share.sql` migration before runtime use.
- Consumer product-list tab data is retained for 30 seconds during tab switching; pull-to-refresh, search, and “换一批” force a refresh. Product card images use mini-program lazy loading.
- `FRONTEND_OPTIMIZATION_NOTES.md` is a short Chinese review/interview note covering only the reasons, implementation, effects, boundaries, and main code locations of the frontend optimizations. Keep it concise and aligned when these behaviors change.
- `功能设计方案.md` is the single consolidated record for planned features. Each feature is a numbered top-level section in the same file; the first section covers coupon templates, manual issuance, sharing/claiming, new-user grants, and later order redemption. Update its internal directory and relevant section when new designs or status changes are added.
- `后端知识点.md` is the user's concise project-based learning note. After each completed feature, add a short section covering request flow, relevant layers, permission behavior, transaction purpose when applicable, and one interview answer. Do not repeat access/refresh-token basics unless their behavior changes.

## Incremental delivery roadmap

- Batch 1 — Real WeChat mini-program login: complete on 2026-08-31, including successful manual acceptance in Weixin DevTools with a real one-time code. The full `uni.login → code2Session → OpenID user create/reuse → mall JWT/refresh token` path works; secrets remain outside the repository.
- Batch 2 — Coupon template management: repository implementation complete on 2026-08-31 across database schema, backend, OpenAPI, and admin frontend; local database DDL execution and runtime acceptance remain manual environment steps.
- Batch 3 — Manual issuance and consumer “My Coupons”: repository implementation complete on 2026-08-31 across database/backend/OpenAPI/admin frontend/consumer frontend; local runtime acceptance remains manual.
- Batch 3 extension — Coupon sharing and claiming: repository code complete on 2026-08-31; run the one-time migration and complete two-account WeChat acceptance before marking runtime complete. Creator rewards remain deferred.
- Batch 4 — Automatic new-WeChat-user coupon: not started.
- Batch 5 — Order coupon locking/redemption/refund behavior: not started and should wait for the order module.
- Order pickup design — 2026-09-04: added `公共知识/订单功能设计方案.md` for online ordering with offline pickup; excludes logistics and real payment, assumes one pickup point, defines order/item snapshots, reserved inventory, one-time pickup codes, state transitions, APIs, phased implementation, and local acceptance cases. Phase 1 repository implementation is now complete: `mall_order`/`order_item` DDL and migration, preview API, and read-only “my orders” APIs are present; real local acceptance still requires executing the migration against MySQL. Order creation, inventory reservation, pickup-code verification, coupons, and frontend/admin pages remain future phases.
- Optional Batch 1.1 — dev-profile-only simulated WeChat login: not started; only needed if real WeChat credentials are unavailable during local development.

## Verification baseline

As of 2026-08-31:

- `admin`: `npm run build` succeeds. Vite reports a non-blocking large-chunk warning (the main JS bundle is over 500 kB).
- `frontend`: `npm run type-check` and `npm run build:mp-weixin` succeed.
- `backend`: Maven test suite succeeds with 21 tests when using the repository-local Maven cache command above.

## Known follow-ups

- Admin bundle can be reduced later with route-level dynamic imports/manual chunks.
- When diagnosing login connectivity, first check whether port 8080 is listening and inspect backend startup logs before changing frontend code.
- Define an explicit UniApp per-target API strategy: H5 development can use the Vite `/api` proxy, Weixin DevTools can reach `127.0.0.1`, while a real device must use an HTTPS request domain configured in the WeChat platform; `localhost` on a device is not the development PC.
- Do not assume a successful UI route guard secures an API; authorization must remain enforced by Spring Security.

## Change log

- 2026-09-04 — `order/design`: 新增 `公共知识/订单功能设计方案.md`，确定线上下单、线下取货、不接真实支付和不做物流的范围；设计订单/明细快照、预留库存、取货码、状态流转、接口、事务并发规则、分阶段实施和本地测试场景。仅文档变更，未修改业务代码或数据库。

- 2026-09-04 — `order/phase1`: 新增订单主表 `mall_order`、明细表 `order_item`、迁移 SQL、详细注释的 DTO/Entity/Mapper/Service/Controller/VO 和订单异常；实现 `POST /api/v1/orders/preview`、`GET /api/v1/me/orders`、`GET /api/v1/me/orders/{id}`，后端从 JWT 做用户归属校验，预览按数据库最新价格/状态/库存计算且不写库、不锁库存；同步 OpenAPI、取货点配置、订单设计状态和后端学习笔记。执行仓库本地 Maven 缓存命令，34 个测试全部通过（包含匿名订单接口 401 安全边界测试）；未执行数据库迁移、真实运行时接口验收或前端页面接入。

- 2026-09-04 — `workspace/incremental-commits`: 记录用户确认的长期协作偏好：后续代码尽量按数据库/契约、后端、测试、前端和文档等小范围拆成多次本地提交；每次提交说明范围与验证结果，默认不推送远程，便于定位和安全回退。

- 2026-09-04 — `workspace/commenting convention`: 记录用户确认的长期代码注释要求：前后端代码都补充关键中文注释，后端额外详细说明分层、请求链路、安全边界、异常、事务、并发、幂等和数据库影响；仅更新项目记忆，未修改业务代码。

- 2026-09-04 — `backend/documentation`: 为后端 Java 主代码和测试代码补充中文类、方法、字段和关键流程注释；新增 `公共知识/后端代码阅读手册.md`，说明前端开发者的阅读路径、请求链路、分层职责、常见故障定位和后端开发顺序；同步扩展 `公共知识/后端知识点.md`。注释和文档不改变运行逻辑；使用仓库本地 Maven 缓存执行 `mvn "-Dmaven.repo.local=E:\\ai_mall\\.m2\\repository" test`，22 个测试全部通过。

- 2026-09-02 — `公共知识可视化`: 重做 `公共知识/优惠券功能设计详细交互版.html`，参考主流电商生命周期补充可用、锁券、支付核销、取消释放、退款回补、订单快照和对账视角；文件仍只写入 E 盘。

- 2026-09-02 — `公共知识可视化`: 新增 `公共知识/优惠券功能设计详细交互版.html`，按数据模型、人工发券、分享领取、订单核销和面试回答五个专题展示优惠券设计；文件写入 E 盘，未修改业务代码。

- 2026-09-02 — `公共知识文档`: 新增 `公共知识/优惠券功能设计详细版.md`，整理优惠券领域模型、接口链路、事务幂等并发、安全边界、测试验收和面试问答；文档依据现有设计与实现编写，未执行代码验证。

- 2026-09-02 — `workspace/file-path-policy`: recorded the user's requirement that future files, caches, build outputs, visualizations, and temporary files must not be written to C:; existing C: files require confirmation before deletion. Documentation-only change; verified by reviewing this memory entry.

- 2026-09-02 — `workspace/communication preference`: recorded that database, backend, and operations topics must be explained clearly from a frontend developer's perspective, including terminology, purpose, execution location, expected results, and operational risk. Documentation-only change; no runtime verification was needed.

- 2026-09-01 — `deployment documentation`: expanded `公共知识/GitHub Actions自动部署.md` into a click-by-click, command-by-command guide with expected outputs, success/failure checks, rollback, non-uploaded content, and the standard format for future `公共知识` documents. Documentation-only change; no deployment or server operation was run.

- 2026-09-01 — `deployment automation`: added a GitHub Actions backend deployment workflow and a server-side JAR deployment script with SSH upload, backup, health check, and rollback; added `公共知识/GitHub Actions自动部署.md` with one-time server preparation, SSH secrets, trigger, verification, and rollback steps. Static repository checks only; no server restart or GitHub Actions run was performed.

- 2026-09-01 — `公共知识文档`: 删除 `公共知识/本地与线上部署排错.md` 中多余空行，保留命令、分类和安全说明；未做额外验证。

- 2026-09-01 — `公共知识文档`: 新增 `公共知识/本地与线上部署排错.md`，按本地 PowerShell、服务器 Linux、配置、打包、日志和端口分类整理常用命令，并补充换行符、jar 打包和危险命令说明；未记录真实凭据，未做额外验证。

- 2026-09-01 — `公共知识文档`: 新增简洁的 `公共知识/Navicat连接线上数据库.md`，记录通过 SSH 隧道连接线上 MySQL 的命令、Navicat 参数和常见错误；未记录任何真实凭据，文档未做额外验证。

- 2026-08-31 — `admin/environment-config`: separated Admin development and production API origins into `.env.development` and `.env.production`; centralized Axios base URL construction and made the Vite development proxy read the same configuration. Quick TypeScript check passed; no dependencies installed.
- 2026-08-31 — `backend/error-observability`: added server-side stack-trace logging for previously hidden unexpected exceptions while keeping the client response generic and unchanged; logs record exception types and the stack trace only on the server. Quick Maven compile passed; no full test suite or runtime verification was run.

- 2026-08-31 — `coupon/share-claim`: added secure random share tokens (hash-only persistence), public preview, authenticated claim, creator/self-claim protection, per-share uniqueness, template inventory/limit checks, migration SQL, and mini-program share/claim pages; updated OpenAPI, design status, and learning notes. Quick backend compile plus frontend/admin type-check passed; no database migration, runtime API, or two-account WeChat acceptance was run.
- 2026-08-31 — `admin/coupon-grants/usability`: changed the per-user-limit conflict message to plain Chinese and defaulted the manual-grant reason to “活动发放” while keeping it editable. Quick Maven compile and admin type-check passed.
- 2026-08-31 — `admin/backend/user-management`: added admin user list/search/detail, coupon summary, activation/deactivation endpoints and `/users` page; protected with admin roles, restricted targets to CUSTOMER users, and kept WeChat identity fields out of responses. Quick Maven compile and admin type-check passed; no runtime API test or full build was run.
- 2026-08-31 — `batch 3/frontend/rollback-share-scope`: completed the admin manual-issuance page and consumer “My Coupons” page/detail with typed clients and protected routes; removed the uncompiled sharing/claiming backend, schema, and migration draft after scope review. Quick Maven compile, admin type-check, and frontend type-check passed; no runtime API test or production build was run.
- 2026-08-31 — `backend learning documentation`: added the concise `后端知识点.md` with project-specific explanations of authentication versus authorization, backend security boundaries, Controller/Service/Mapper/Entity layering, coupon-grant transactions, concurrency, idempotency, and short interview answers; recorded the rule to extend it after each completed feature. Documentation-only change; no verification run.
- 2026-08-31 — `batch 3/backend/database/api/coupon-grant`: added `coupon_grant` audit and immutable `user_coupon` snapshot tables; implemented idempotent admin manual issuance, concurrency-safe template inventory reservation, per-user limit checks, non-sensitive admin customer search, and JWT-owned paginated user coupon list/detail APIs with real-time expiration derivation; updated OpenAPI and feature status. Quick Maven compile passed with tests skipped; no DDL execution, runtime API test, or concurrency test was performed.
- 2026-08-31 — `batch 2/admin/coupon-template`: added typed admin coupon-template API clients and a dedicated `/coupon-templates` page with name/status filtering, detail display, draft creation/editing, fixed-range or post-receipt validity, string money submission, share flag display, and guarded activate/deactivate actions; added the navigation entry and marked the repository-side batch complete. `npm run type-check` passed in `admin`; no production build, runtime API test, or database DDL execution was performed.
- 2026-08-31 — `batch 2/backend/database/api/coupon-template`: added the `coupon_template` schema with database constraints; implemented paginated admin template create/get/list/full-draft-update/activate/deactivate APIs; used string money inputs converted to `BigDecimal`, explicit mutually exclusive validity modes, conditional state transitions, stable 400/404/409 errors, and admin-role security; updated OpenAPI and feature status. Quick backend compile passed with tests skipped; no database migration execution or runtime API test was performed.
- 2026-08-31 — `coupon/design documentation`: updated feature one in `功能设计方案.md` with a concise explanation of coupon-template purpose and immutability, the reviewed admin/current-user/share/claim endpoint structure, monetary representation, grant auditing, opaque share-token rationale, automatic new-user grant boundary, and design advantages/disadvantages. Documentation-only change; no verification run.
- 2026-08-31 — `workspace/confirmation workflow`: recorded the user's requirement that every actionable request must first be restated for understanding and explicitly confirmed before any modification or command is executed. Documentation-only change; no verification run.
- 2026-08-31 — `feature-design documentation structure`: restored a single root `功能设计方案.md` as requested; it now has an internal feature directory and uses headings such as `功能一：优惠券方案设计`, followed by `功能二：XXX方案设计` and `功能三：XXX方案设计`. Documentation-only change; no verification run.
- 2026-08-31 — `feature-design documentation structure`: replaced the single root feature-design document with `功能设计方案/README.md` plus numbered per-feature files, and moved the existing coupon design to `功能设计方案/01-优惠券.md`. Documentation-only change; no verification run.
- 2026-08-31 — `coupon/design documentation`: added `功能设计方案.md` with a concise coupon roadmap, core data, opaque share-token flow, security/concurrency rules, planned APIs, local/real-WeChat testing strategy, and interview summary. Documentation-only change; no verification run.
- 2026-08-31 — `frontend/documentation`: condensed `FRONTEND_OPTIMIZATION_NOTES.md` to a one-page key-points format covering startup, navigation, request deduplication, progressive profile refresh, product caching, lazy loading, security boundaries, and a short interview summary. Documentation-only change; no verification run.
- 2026-08-31 — `frontend/documentation`: added `FRONTEND_OPTIMIZATION_NOTES.md`, a Chinese study and interview guide explaining the implemented startup, navigation, session validation, caching, lazy loading, security-boundary, tradeoff, measurement, and future-extension decisions, including a consolidated cause/implementation/effect/cost matrix. Documentation-only change; no verification run.
- 2026-08-31 — `frontend/startup/navigation/session/performance`: changed the mini-program launch page and default post-login destination to the product home; centralized route constants, safe redirects, and tab navigation; limited navigation-level auth protection to private cart data; added single-flight 60-second session validation with non-disruptive background failure handling; made profile render cached user data immediately; cached product-list tab data for 30 seconds while preserving forced refresh/search behavior; and enabled lazy product images. Existing stored sessions and backend APIs remain compatible. Verified only with `npm run type-check`; no full mini-program build or automated runtime test was run.
- 2026-08-31 — `batch 1/manual acceptance`: successfully completed real WeChat mini-program login in Weixin DevTools after environment inheritance and non-standard Content-Type handling were corrected. Batch 1 is complete; Batch 2 coupon template management is next. Manual runtime acceptance only; no additional automated verification was run.
- 2026-08-31 — `workspace/verification workflow`: refined the user's preference to allow quick compilation/type checks while avoiding time-consuming test suites, full builds, dependency installation, or broad verification unless requested. Documentation-only workflow update.
- 2026-08-31 — `backend/auth/wechat compile fix`: corrected checked-`IOException` handling across HTTP status/body reads and Jackson byte-array parsing, keeping these failures inside the sanitized provider boundary without changing API behavior. Verified with a quick `mvn -DskipTests compile`; compilation succeeds. Tests were not run.
- 2026-08-31 — `backend/api/auth/wechat`: reviewed and hardened the WeChat integration after a real `UnknownContentTypeException`: replaced Content-Type-dependent automatic deserialization with a provider-local adapter that reads at most 16 KiB plus an overflow byte, parses raw bytes with Jackson, validates HTTP status/Wechat errcode/OpenID, classifies invalid credentials separately from configuration/upstream failures, and prevents external messages or sensitive request data from entering logs or client responses. Updated exception mapping, OpenAPI, auth contract, and controller regression coverage. The final implementation compiles; tests were not run per user verification preference.
- 2026-08-31 — `workspace/engineering principles`: strengthened the project rule that convenience and shorter code must not drive implementation choices; future designs must prioritize correctness, maintainability, extensibility, observability, testability, compatibility, and explicit external-service failure handling. Documentation-only change; intentionally not verified per user preference.
- 2026-08-31 — `backend/auth diagnostics`: added sanitized WeChat code2Session failure logging that distinguishes HTTP status, network/root-cause type, and response-processing type without logging AppSecret, one-time code, exception messages, or full request URLs. Behavior and client-facing error remain unchanged. Intentionally not tested or built per user verification preference.
- 2026-08-31 — `workspace/verification workflow`: recorded the user's preference to skip automatic tests/builds for ordinary source-only edits, while requiring an explicit verification recommendation for security-, money-, database-, and data-integrity-sensitive changes. Documentation-only change; intentionally not verified per user preference.
- 2026-08-31 — `batch 1/backend/frontend/api/auth`: completed the repository-side real WeChat mini-program login batch by documenting `/api/v1/auth/wechat/login` and `WechatLoginRequest` in OpenAPI, correcting the auth contract, preserving safe post-login redirects for WeChat login, and adding controller/service coverage for success, blank/invalid code handling, first-user creation, returning-user reuse, and disabled users. No AppSecret was added to the repository. Verified with 21 backend tests, frontend type-check, and the WeChat mini-program production build; real code2Session acceptance remains a local Weixin DevTools step with developer-owned credentials.
- 2026-08-31 — `backend/MyBatis-Plus convention`: standardized appropriate lambda update wrapper construction on `Wrappers.lambdaUpdate(Entity.class)` with explicit entity typing; updated product replacement accordingly. Verified with all 15 Maven tests passing.
- 2026-08-31 — `backend/product`: changed full-form product updates from `updateById()` to an explicit `LambdaUpdateWrapper` covering all seven editable fields, allowing empty image URLs and descriptions to clear existing database values as SQL `NULL`; added a service regression test that asserts the complete update set and both null parameters. Verified with all 15 Maven tests passing.
- 2026-08-31 — `workspace/change workflow`: recorded the user requirement that implementation proposals, including alternatives, advantages, disadvantages, risks, recommendation rationale, and verification, must be reviewed and explicitly approved before any code, configuration, API, database, build, or workflow modification. Documentation-only change; reviewed in `PROJECT_MEMORY.md`.
- 2026-08-31 — `workspace/engineering conventions`: recorded the requirement to evaluate correctness and extensibility before selecting framework shortcuts, and clarified full-form replacement/null-clearing semantics plus required regression coverage. Documentation-only change; reviewed in `PROJECT_MEMORY.md`.

- 2026-08-30 — `frontend/config diagnosis`: confirmed that UniApp can target different backend/database instances depending on build mode; development currently has duplicate local/remote API values and production targets `124.221.241.24:8081`. No runtime code changed.
- 2026-08-29 — `workspace`: added `AGENTS.md` and this shared memory file so future tasks can reuse architecture, commands, conventions, status, and recent decisions. Verified the files are repository-root scoped.
- 2026-08-29 — `admin`: aligned Ant Design and custom CSS with the UniApp forest-green/warm-gold visual style; redesigned the admin login page to match the UniApp brand layout. Verified with `npm run build`.
- 2026-08-28 — `admin`: repaired the npm installation and generated `package-lock.json`; fixed stale-state behavior in product search/reset. Verified TypeScript and production build.
- 2026-08-28 — `backend/auth`: added JWT request authentication, role protection for admin product endpoints, refresh-token rotation, logout revocation, and current-user handling. Verified with 14 Maven tests.
