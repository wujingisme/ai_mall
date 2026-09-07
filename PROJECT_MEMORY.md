# AI Mall 项目共享记忆

最后更新：2026-09-07

本文档是 AI Mall 仓库内所有任务共用的项目记忆。开始处理本仓库任务前必须先阅读，发生实质性变更后必须同步更新。如本文档内容已过时，以源代码和 `api/openapi.yaml` 为最终依据。本文档及后续新增的变更日志统一使用中文书写；技术名称、代码标识、命令、路径和 API 可保留原文。

## 项目结构

| 路径 | 用途 | 技术栈 |
| --- | --- | --- |
| `frontend/` | 消费者端应用，包括 H5、App 和小程序目标 | UniApp、Vue 3、TypeScript |
| `admin/` | 商家/管理端网页控制台 | React 18、TypeScript、Vite、Ant Design |
| `backend/` | REST API 后端 | Java 17、Spring Boot 3、Spring Security、MyBatis-Plus |
| `api/openapi.yaml` | 共享 API 契约 | OpenAPI 3 |
| `database/schema.sql` | 数据库结构 | MySQL 8 |

## 运行环境与端口

- 后端：`http://localhost:8080`
- Admin 开发服务器：`http://localhost:5174`；Vite 将 `/api` 代理到 `http://localhost:8080`。
- UniApp 的 API 目标由 `frontend/.env.*` 控制。截至 2026-08-31，`.env.development` 中有一条生效的 `VITE_API_BASE_URL=http://127.0.0.1:8080` 和多条已注释示例，`.env.production` 指向 `http://124.221.241.24:8081`。判断数据写入了哪个数据库前，必须先确认当前构建模式。
- 前端出现“无法连接服务器”通常表示后端未监听 8080 端口、后端启动失败（常见原因是数据库配置），或前端启动时没有使用开发代理。

### 常用命令

```powershell
# 后端
cd E:\ai_mall\backend
mvn spring-boot:run

# 后端测试（使用仓库内 Maven 缓存，避免访问受限的 C:\.m2）
mvn "-Dmaven.repo.local=E:\ai_mall\.m2\repository" test

# React 管理端
cd E:\ai_mall\admin
npm install
npm run dev
npm run build

# UniApp H5
cd E:\ai_mall\frontend
npm install
npm run dev:h5
```

后端启动需要可连接的 MySQL 数据库以及必要的环境变量/配置值。严禁在本文档中记录真实数据库密码或 JWT 密钥。

## 自动化部署

- `.github/workflows/deploy-backend.yml` 当前已关闭代码推送后的自动部署：原有 `push` 配置保留为带中文说明的注释，便于后续取消注释恢复。现在只有通过 `workflow_dispatch` 手动运行时，才会使用 Java 17 构建并测试 Spring Boot JAR、通过 SSH 上传并调用部署脚本；生产部署仍保持串行执行。
- `deploy/mall-backend-deploy.sh` 会上传到 `/www/wwwroot/mymall/backend/deploy/`；它保留服务器上的 `backend.env`，以 `www` 用户运行 JAR，保留带时间戳的 JAR 备份，执行商品列表健康检查，并在启动失败时回滚到上一个 JAR。除非修改 `JAVA_BIN`，否则默认使用当前 OpenCloudOS JDK 路径 `/www/server/java/jdk-17.0.8/bin/java`。
- 工作流需要 GitHub Actions 密钥 `DEPLOY_HOST`、`DEPLOY_PORT`、`DEPLOY_USER`、`DEPLOY_PATH`、`DEPLOY_SSH_KEY` 和 `DEPLOY_KNOWN_HOSTS`。数据库密码、JWT 密钥、微信 AppSecret 和服务器环境文件均不得存入 GitHub。
- 一次性服务器准备和完整 GitHub 配置记录在 `公共知识/GitHub Actions自动部署.md`。自动化代码已在仓库中就绪，但完成服务器准备和 GitHub 密钥配置前不会生效；前端、Admin 和微信发布不属于该工作流范围。

## API 与认证

- API 基础前缀：`/api/v1`。
- 消费者端公开商品接口：`/api/v1/shop/products/**`。
- Admin 商品接口：`/api/v1/products/**`；需要 `ADMIN` 或 `OPERATOR` 权限。
- 公开认证接口包括注册、登录、刷新令牌和退出登录。
- 微信小程序登录使用 `POST /api/v1/auth/wechat/login`：前端只提交 `uni.login` 的一次性 code，后端使用环境变量中的 AppID/AppSecret 调用微信 code2Session，以 OpenID 创建或复用 CUSTOMER 用户，再签发商城自己的 JWT/refresh token。AppID 可保存在 `frontend/src/manifest.json`；AppSecret 严禁进入前端或 Git 仓库。
- 微信 code2Session 适配层限长读取原始响应并用 Jackson 独立解析，不依赖外部 `Content-Type`；无效 code 映射为 401，上游不可用、未配置或非法响应映射为 503。日志只能记录脱敏的状态、异常类型或微信 errcode，不得记录 AppSecret、code、完整 URL、原始正文或外部 errmsg。
- `/api/v1/auth/me` 需要有效的 Bearer access token。
- access token 是已签名的 JWT，并包含 `roles` 声明。
- refresh token 是不透明令牌，以哈希形式保存，刷新时轮换，退出登录时撤销。
- Admin Axios 逻辑位于 `admin/src/utils/request.ts`；会话存储位于 `admin/src/utils/auth.ts`。
- Admin API 来源由 `admin/.env.development` 和 `admin/.env.production` 配置；开发环境下 Vite 将 `/api` 代理到已配置的开发地址，生产环境使用已配置的服务器地址及其 `/api` 反向代理。
- 学习和部署参考文档默认存放在 `公共知识/` 下。
- UniApp 请求与会话逻辑位于 `frontend/src/utils/` 下。
- 消费者端启动后直接打开公开商品首页。除非提供安全的内部重定向地址，否则登录成功后默认进入首页；公开首页、详情页和个人页无需登录即可浏览，进入购物车需要会话，最终权限仍以后端校验为准。
- 消费者端账号密码登录和微信登录处理函数会在登录或已保存会话恢复期间同步拒绝重复进入。UI 禁用提供视觉反馈，函数级保护则避免快速点击或键盘确认造成重复请求、重复保存会话和重复显示成功提示。
- 前端导航策略集中在 `frontend/src/utils/navigation.ts`。会话新鲜度和并发 `/auth/me` 请求去重集中在 `frontend/src/utils/session-validation.ts`；默认运行时校验窗口为 60 秒。后台会话校验可以清除过期凭据，但不会强制公开页面跳转到登录页。
- 前端 TypeScript 路径别名在 `frontend/tsconfig.json` 中配置为 `@/* -> ./src/*`，不使用已弃用的 `baseUrl`，因此兼容 TypeScript 6 过渡以及 TypeScript 7 移除该选项后的行为。

## 当前 UI 方向

两个客户端应保持统一的视觉语言：

- 深森林绿：约 `#173f34`、`#245d4d`、`#28604f`。
- 暖金色强调色：约 `#e4bd6d` / `#e4c57d`。
- 柔和灰绿色页面背景：`#f3f6f4` / `#f6f7f4`。
- 主文本色：`#17362d`；次要文本使用低饱和灰绿色。
- 使用圆角卡片、克制的绿色阴影和以商品为核心的简洁布局。

React Admin 主题配置位于 `admin/src/main.tsx`，应用 CSS 位于 `admin/src/styles.css`。登录页有意采用与 UniApp 登录页一致的双栏品牌布局。

## 重要实现说明

- **代码注释约定**：以后新增或修改前后端代码都必须补充关键中文注释。前端至少说明组件/函数职责、数据流、状态变化、接口参数和非直观的边界处理；后端必须更详细地说明分层职责、请求链路、权限与数据归属、异常路径、事务/并发/幂等、数据库影响，以及关键实现为什么这样选择。注释应解释业务原因，避免只重复代码字面含义。

- **增量提交约定**：用户希望以后代码尽量拆成多次、小范围、可独立验证的本地 Git 提交。推荐按数据库/契约、后端业务、测试、前端页面或文档等边界拆分；每次提交前后说明变更范围和验证结果，默认不推送远程。提交应保持可理解、可回退，避免把多个无关功能塞进一个大提交。
- **提交信息约定**：提交信息保留并统一使用 Conventional Commits 前缀，例如 `feat:`（功能）、`fix:`（修复）、`test:`（测试）、`docs:`（文档）、`refactor:`（重构）。这是项目协作规范，不是 Git 强制行为；已有历史提交可能沿用旧格式，不为统一格式重写已推送历史。

- **文件路径约束**：后续任何新文件、缓存、构建产物、可视化文件和临时文件均不得写入 C 盘；默认使用 `E:\ai_mall` 或用户明确指定的其他非 C 盘路径。已存在的 C 盘文件不得未经确认删除。

- **用户背景与讲解方式**：用户主要是前端开发者，尚不熟悉数据库、后端开发和运维。讨论 MySQL、Java/Spring、Linux、服务器、部署、Nginx、宝塔、权限、进程、端口等主题时，要用前端开发者容易理解的语言解释每个新概念；说明步骤用途、具体操作位置、成功时应看到的结果，以及是否会影响本地或生产服务。不得直接给出未解释的运维命令，也不得默认用户具备后端或运维知识。
- 在修改任何代码、配置、API、数据库、构建或工作流前，先向用户提供设计方案并等待明确批准。方案应说明预期行为、受影响模块/文件、API 与数据语义、兼容性或迁移影响、验证计划，以及可选方案各自的优点、缺点、风险和适用场景。给出推荐方案及理由，用户确认后才能实施。只读诊断和解释无需事先批准。
- **用户确认流程**：每个会创建、修改、删除、移动、安装、配置或执行内容的新请求，都必须先简要复述对目标和预定操作的理解，然后停止并等待用户以“是的”“确认”或“可以”等方式明确确认。即使请求看起来很清楚，也不能直接开始操作。无需采取操作的只读解释可以直接回答。
- 实现方案应依据明确的 API 与数据语义选择，不能只追求最短的框架写法。编码前需考虑正确性、`null`/空值/缺失值状态、失败路径、安全性、可维护性和可能的扩展点。
- 工程优先级是正确性和长期设计，而非实现方便或代码行数。选择框架捷径前，应明确评估可维护性、可扩展性、可观测性、可测试性、协议偏差、兼容性和运行故障模式。对外部服务，应把状态码、响应头、正文、超时、畸形/非标准响应、代理行为、敏感日志和服务方未来变化视为核心设计问题；供应商专用适配应隔离处理，不得为此削弱整个应用的行为约束。
- 商品完整表单编辑表示完整替换全部可编辑字段：可选字段提交为空时必须持久化为 SQL `NULL`。除非接口契约有意采用选择性更新，否则不得使用会静默忽略 `null` 的 ORM 辅助方法。
- 适合使用 MyBatis-Plus Lambda 更新包装器时，应通过 `Wrappers.lambdaUpdate(Entity.class)` 构造，明确实体类型并统一代码风格。如果自定义 SQL 或其他持久化机制更符合操作语义，本约定不妨碍选择更合适的方案。
- 对实质性行为变更，应补充正常更新、清空已有可选值、校验/冲突以及相关授权路径的回归测试。
- **用户验证偏好**：普通的纯源码修改后，可以执行快速编译或类型检查来发现语法和类型错误，但不要自动运行耗时测试套件、完整构建、依赖安装或大范围验证。必须准确说明已检查和未检查的内容。对于认证、授权、支付、金额计算、数据库迁移、并发/数据完整性等高风险变更，应先解释建议增加的验证，再由用户决定，除非用户已明确要求执行。

- React Admin 路由定义在 `admin/src/App.tsx`。
- Admin 商品 CRUD 界面位于 `admin/src/pages/ProductsPage.tsx`。
- 搜索或重置改变页码/筛选状态时，商品筛选会显式覆盖查询参数，避免立即请求读取到过期的 React 状态。
- 商品 `PUT /api/v1/products/{id}` 会完整替换所有 Admin 可编辑字段。`ProductService` 使用 `Wrappers.lambdaUpdate(Product.class)` 和明确的 `.set(...)` 列表，使归一化为 `null` 的可选字段 `imageUrl`、`description` 写入 SQL `NULL`；不得改回默认 `updateById()` 的选择性更新行为。
- 订单库存基础新增 `product.reserved_stock`；消费者商品、购物车和订单预览的可售库存为 `stock - reserved_stock`。Admin 完整修改商品时，如总库存小于已有预留量，会拒绝并返回 `PRODUCT_STOCK_CONFLICT`。正式创建订单现已通过条件 SQL 在事务中预留库存；消费者取消订单则在事务中释放库存。Admin 订单列表和详情查询已可用；取货结算仍是后续工作。
- JWT 解析实现在 `backend/src/main/java/com/aimall/auth/service/JwtService.java`。
- JWT 请求认证实现在 `backend/src/main/java/com/aimall/auth/security/` 下，并由 `SecurityConfig` 注册。
- 消费者商品读取继续使用 `/shop/products`；不得向消费者开放 Admin CRUD 接口。
- API 结构发生变化时，必须同时更新 `api/openapi.yaml` 以及受影响的 TypeScript 类型/客户端。
- 优惠券模板管理接口位于 `/api/v1/admin/coupon-templates`，需要 `SUPER_ADMIN`、`ADMIN` 或 `OPERATOR` 权限。模板初始状态为 `DRAFT`；只有草稿可以完整替换，激活后核心规则锁定，停用操作幂等且不撤销已发放的用户优惠券。Admin 页面位于 `/coupon-templates`，支持列表筛选、草稿创建/编辑、详情、激活和停用。第二批不包含用户优惠券、发放、分享、领取或订单核销表。
- 人工发券使用 `POST /api/v1/admin/coupon-grants`，包含全局唯一幂等键、持久化的 `coupon_grant` 审计记录，以及 `user_coupon` 中不可变的规则快照。同一事务在获取模板行更新锁后，有条件地预留模板库存并检查单用户领取上限。Admin 用户选择使用不含敏感信息的摘要接口 `GET /api/v1/admin/customers`；消费者端 `GET /api/v1/me/coupons[/{id}]` 的数据归属仅由 JWT 决定。过期状态根据 `valid_until` 动态推导，不通过数据库定时状态维护。
- Admin 用户管理页面位于 `/users`，对应 `/api/v1/admin/customers/manage`；它列出/搜索具有 `CUSTOMER` 角色的用户，包括有意配置的 `ADMIN,CUSTOMER` 双角色账号，仅显示 `wechatBound` 布尔值而不暴露 OpenID，并展示优惠券摘要。纯消费者账号支持启用/停用；混合员工账号可见，但前后端都拒绝对其执行消费者启停，避免意外禁用员工权限。所有操作均需要 Admin 角色。
- 已登录的后端角色可通过 `POST /api/v1/admin/accounts/me/customer-role` 开通消费者身份；该接口只修改当前 JWT 用户的角色，保留现有员工角色，操作幂等，且需在消费者端重新登录才能获得新的 `CUSTOMER` 声明。
- Admin 订单查询页面位于 `/orders`，对应 `/api/v1/admin/orders` 和 `/api/v1/admin/orders/{id}`；支持状态、订单号、客户 ID 筛选、服务端分页、详情抽屉和商品快照展示。后端取货接口已提供 `POST /api/v1/admin/orders/{id}/pickup-verification`，但 Admin 页面仍需接入核销按钮和弹窗。
- 优惠券分享/领取作为独立扩展实现：可用且允许分享的用户优惠券会生成随机令牌，其 SHA-256 哈希存入 `coupon_share`；公开解析只展示预览，登录后领取会检查创建者身份、模板是否启用、有效期、库存、单用户上限，以及唯一的 `(share_id, claimant_user_id)` 领取记录。分享目前不会奖励创建者。已有第三批数据库在运行该功能前需要执行一次性迁移 `database/migrations/20260831_coupon_share.sql`。
- 消费者商品列表标签切换时保留数据 30 秒；下拉刷新、搜索和“换一批”会强制刷新。商品卡片图片使用小程序懒加载。
- `FRONTEND_OPTIMIZATION_NOTES.md` 是简短的中文复习/面试笔记，只记录前端优化的原因、实现、效果、边界和主要代码位置。相关行为变化时应保持内容简洁并同步更新。
- `功能设计方案.md` 是规划功能的唯一汇总记录。每个功能都作为同一文件中带编号的一级章节；第一章涵盖优惠券模板、人工发放、分享/领取、新用户发券和后续订单核销。新增设计或状态变化时，更新其内部目录和相关章节。
- `后端知识点.md` 是用户基于项目学习的简洁笔记。每完成一个功能，新增一个简短章节，说明请求链路、相关分层、权限行为、适用时的事务目的，以及一个面试回答。除非 access/refresh token 行为发生变化，否则不要重复其基础知识。

## 增量交付路线图

- 第一批——真实微信小程序登录：已于 2026-08-31 完成，包括在微信开发者工具中使用真实一次性 code 完成人工验收。完整的 `uni.login → code2Session → OpenID 用户创建/复用 → 商城 JWT/refresh token` 链路可用；密钥仍位于仓库外。
- 第二批——优惠券模板管理：数据库结构、后端、OpenAPI 和 Admin 前端的仓库实现已于 2026-08-31 完成；本地数据库 DDL 执行和运行时验收仍需在具体环境中手动完成。
- 第三批——人工发券和消费者“我的优惠券”：数据库、后端、OpenAPI、Admin 前端和消费者前端的仓库实现已于 2026-08-31 完成；本地运行时验收仍需手动完成。
- 第三批扩展——优惠券分享和领取：仓库代码已于 2026-08-31 完成；标记运行时完成前，需要执行一次性迁移并完成两个微信账号的验收。创建者奖励仍暂缓实现。
- 第四批——微信新用户自动发券：尚未开始。
- 第五批——订单优惠券锁定、核销和退款行为：尚未开始，应等待订单模块完善后再实施。
- 订单取货设计——2026-09-04：新增 `公共知识/订单功能设计方案.md`，用于线上下单、线下取货；不包含物流和真实支付，假设只有一个取货点，并定义订单/明细快照、预留库存、一次性取货码、状态流转、API、分阶段实现和本地验收用例。第一阶段仓库实现已完成：包含 `mall_order`/`order_item` DDL 与迁移、预览 API 和只读“我的订单”API；前端已有带类型的订单 API 客户端、购物车到预览流程以及列表/详情页面。第二阶段库存基础、正式创建订单和消费者提交 UI 已实现；第三阶段取消/释放库存、Admin 订单列表/详情查询、基础 Admin 订单页面以及后端取货核销/结算已实现。取货接口会校验哈希、锁定订单和商品、原子转移 `stock/reserved_stock`，并使正确的重复请求保持幂等。三个订单迁移已在本地执行；其他环境仍需执行 `20260904_order_base.sql`、`20260904_order_inventory.sql` 和 `20260904_order_create.sql`。Admin 核销按钮和优惠券核销仍是后续工作。
- 可选第一批 1.1——仅开发配置启用的模拟微信登录：尚未开始；仅在本地开发无法使用真实微信凭据时需要。

## 验证基线

截至 2026-09-04：

- `admin`：`npm run type-check` 和 `npm run build` 均成功。Admin `/orders` 路由现支持订单筛选、分页、详情抽屉和商品快照展示。顶部为不含 CUSTOMER 的员工账号提供受保护的“开通前台身份”操作；`/users` 表格展示角色，且不向员工账号提供启停操作。Vite 会报告不阻塞构建的大分包警告（主 JS 包超过 500 kB）；新控件的浏览器运行时验收仍待完成。
- `frontend`：`npm run type-check` 和 `npm run build:mp-weixin` 均成功。订单预览页现可提交订单，在网络重试期间保留客户端幂等键并展示一次性取货码；详情页说明该取货码不会再次返回。
- `backend`：使用上述仓库内 Maven 缓存命令时，79 个测试全部通过。订单第一至第三阶段的取消、Admin 订单查询、后端取货核销以及双角色开通契约均通过本地测试验证，包括错误取货码拒绝、重复取货码幂等、库存回滚、匿名访问拒绝、追加角色幂等和员工账号停用保护。此前已在应用三个订单迁移后，通过备用 18080 端口和本地 MySQL 验证消费者订单运行时流程与取消流程；取货核销和双角色开通尚未完成真实浏览器/API 运行时验收。8080 端口可能仍被现有本地后端进程占用，因此使用备用端口验证仍有价值。

## 已知后续事项

- 后续可通过路由级动态导入或手动分包缩小 Admin 构建产物。
- 排查登录连接问题时，应先检查 8080 端口是否监听并查看后端启动日志，再考虑修改前端代码。
- 需要明确 UniApp 各目标平台的 API 策略：H5 开发可使用 Vite `/api` 代理，微信开发者工具可访问 `127.0.0.1`，真机则必须使用微信平台中配置的 HTTPS 请求域名；真机上的 `localhost` 并不是开发电脑。
- 不得认为 UI 路由守卫成功就代表 API 已安全；授权必须继续由 Spring Security 强制执行。
- 将 Admin `/orders` 详情抽屉接入 `POST /api/v1/admin/orders/{id}/pickup-verification`，包括确认弹窗、输入校验、成功后刷新，以及对 `ORDER_PICKUP_CODE_INVALID`、`ORDER_STATE_CONFLICT` 和 `ORDER_INVENTORY_CONFLICT` 的稳定处理。
- 需要同时购物的管理员使用 Admin 顶部“开通前台身份”操作；成功后在消费者端重新登录，使 JWT 包含 `CUSTOMER`，再确认该账号在 `/users` 中显示双角色。

## 变更日志

- 2026-09-07 — `deployment/manual-only-trigger`：注释 `.github/workflows/deploy-backend.yml` 中面向 `main` 及后端相关路径的 `push` 自动触发配置，保留中文恢复说明和 `workflow_dispatch` 手动部署入口；同步更新项目记忆。以后推送代码不会自动发布，手动运行时原有构建、上传、重启、健康检查和回滚流程不变。仅执行工作流文本结构和差异检查，未触发 GitHub Actions、服务器部署或重启。

- 2026-09-07 — `workspace/project-memory-language`：将项目共享记忆中的说明、实施约定、路线图、验证基线、后续事项和历史变更记录统一翻译为中文；补充约定，要求今后新增的项目记忆和变更日志使用中文书写，技术名称、代码标识、命令、路径和 API 保留原文。仅文档变更；已检查主要英文叙述残留，未运行构建或测试。

- 2026-09-04 — `auth/dual-role-customer`: 新增 `POST /api/v1/admin/accounts/me/customer-role`，允许当前 ADMIN/OPERATOR/SUPER_ADMIN 账号安全、幂等地给自己追加 `CUSTOMER` 角色；同步 JWT 角色响应、Admin 顶部“开通前台身份”确认入口、用户列表角色展示，以及双角色账号禁止客户启停的前后端保护。同步 OpenAPI、详细中文注释和相关测试；Maven 79 个测试、Admin `npm run type-check` 均通过。未新增数据库表或迁移，未执行真实浏览器验收，未推送新增提交。

- 2026-09-04 — `frontend/auth-login`: 为账号密码登录和微信登录增加共享的函数级同步防重复提交保护，并在已有会话恢复期间禁用两个登录入口，避免快速点击或键盘确认重复发送请求、保存会话和显示两次成功提示；未修改后端接口、令牌或数据库。执行 `npm run type-check` 通过，未运行完整构建或微信开发者工具验收。

- 2026-09-04 — `order/admin-pickup-verification`: 新增后台取货码核销接口 `POST /api/v1/admin/orders/{id}/pickup-verification`，规范化并哈希比对 8 位取货码；在一个事务中锁订单和商品，按明细聚合并结算 `stock/reserved_stock`，再将订单改为 `PICKED_UP`；正确重复核销幂等返回，错误取货码、取消订单、库存不一致返回稳定 409。同步 OpenAPI、异常映射、详细中文注释，以及 Service/Controller/安全测试；Maven 73 个测试全部通过。未新增数据库表或迁移，Admin 页面按钮尚未接入，未推送新增提交。

- 2026-09-04 — `frontend/typescript-config`: 移除 `frontend/tsconfig.json` 已弃用的 `baseUrl`，将 `@/*` 别名改为显式 `./src/*` 路径；`npm run type-check` 通过，未改变业务导入或运行时行为，未推送新增提交。

- 2026-09-04 — `admin/order-page`: 新增 Admin 订单管理页面、订单类型和 API 客户端，接入状态/订单号/客户 ID 筛选、分页、详情抽屉和商品快照展示；同步菜单、路由、订单设计状态和中文关键注释。`npm run type-check`、`npm run build` 均通过；未做浏览器运行时验收，未推送新增提交。

- 2026-09-04 — `admin/order-query`: 新增后台订单分页列表和详情查询、客户非敏感摘要、订单快照映射及 Admin 角色权限；同步 OpenAPI、详细中文后端注释和 9 个查询/安全测试。Maven 64 个测试全部通过；未新增数据库表，未推送新增提交。

- 2026-09-04 — `order/phase3-cancellation`: 新增消费者取消订单接口、订单/商品行锁、按订单明细释放预留库存、重复取消幂等和状态冲突处理；禁止删除有预留库存的商品，避免订单外键置空后无法回收库存；同步 OpenAPI、中文注释与测试（含商品更新加锁读取场景）。Maven 55 个测试全部通过；在 18080 端口完成真实创建→取消→重复取消链路，确认预留库存 0→2→0→0，临时数据已清理；未推送新增提交。

- 2026-09-04 — `frontend/order/create`: 为订单确认页接入正式创建订单 API，增加客户端幂等键、提交中禁用、库存/购物车变化后刷新、首次取货码展示和订单入口；同步订单类型、API 客户端、订单详情提示及公共知识文档。`npm run type-check` 和 `npm run build:mp-weixin` 均通过；未执行微信 DevTools 真机验收。

- 2026-09-04 — `workspace/commit-message-convention`: 记录用户确认保留并统一使用 `feat:`、`fix:`、`test:`、`docs:`、`refactor:` 等提交前缀；不重写已有提交历史。

- 2026-09-04 — `order/phase2-order-create`: 新增正式创建订单事务、幂等请求摘要、购物车/商品行锁、条件预留库存、订单与商品快照、一次性取货码响应和同键参数冲突错误；同步数据库迁移、OpenAPI、中文详细注释与 Service/Controller 测试。执行 `20260904_order_create.sql` 本地迁移并确认字段存在；Maven 47 个测试全部通过；在 18080 端口完成注册/登录、加购、下单、重复提交、同键改参数和详情读取真实接口验收，临时数据已清理；该提交后来已与远程 `main` 同步，本次步骤未显式执行 push。

- 2026-09-04 — `order/phase2-inventory-foundation`: 新增 `product.reserved_stock` 字段及 `reserved_stock <= stock` 数据库约束；购物车、消费端商品、订单预览统一按可售库存计算；Admin 修改商品总库存低于已预留数量时返回 `PRODUCT_STOCK_CONFLICT`；补充库存、预留、Admin 冲突和异常契约测试。执行本地 migration 并检查字段/约束，Maven 40 个测试全部通过，使用 18080 端口启动后端并成功读取商品列表；线上迁移尚未执行。

- 2026-09-04 — `order/phase1-local-acceptance`: 在本机 MySQL 执行 `database/migrations/20260904_order_base.sql` 并确认 `mall_order`、`order_item` 创建成功；启动后端完成注册/登录、加入购物车、订单预览、默认取货点、空请求 400、不存在订单 404、匿名接口 401 验收；执行 Maven 34 个测试全部通过。验收用临时账号和购物车数据已清理，未实现订单创建、库存锁定、支付或物流。

- 2026-09-04 — `workspace/git-ssh`: 修复本机 GitHub SSH `known_hosts` 过期主机记录，确认现有 `id_rsa.pub` 已加入 GitHub 后，将本仓库 `origin` 切换为 `git@github.com:wujingisme/ai_mall.git`，并在本仓库配置使用 `id_rsa`。SSH 只读连接和 `git push origin main` 均成功，结果为 `Everything up-to-date`；未记录私钥或其他敏感凭据。

- 2026-09-04 — `order/design`: 新增 `公共知识/订单功能设计方案.md`，确定线上下单、线下取货、不接真实支付和不做物流的范围；设计订单/明细快照、预留库存、取货码、状态流转、接口、事务并发规则、分阶段实施和本地测试场景。仅文档变更，未修改业务代码或数据库。

- 2026-09-04 — `order/phase1`: 新增订单主表 `mall_order`、明细表 `order_item`、迁移 SQL、详细注释的 DTO/Entity/Mapper/Service/Controller/VO 和订单异常；实现 `POST /api/v1/orders/preview`、`GET /api/v1/me/orders`、`GET /api/v1/me/orders/{id}`，后端从 JWT 做用户归属校验，预览按数据库最新价格/状态/库存计算且不写库、不锁库存；同步 OpenAPI、取货点配置、订单设计状态和后端学习笔记。执行仓库本地 Maven 缓存命令，34 个测试全部通过（包含匿名订单接口 401 安全边界测试）；未执行数据库迁移、真实运行时接口验收或前端页面接入。

- 2026-09-04 — `frontend/order/api`: 新增 `frontend/src/types/order.ts` 和 `frontend/src/api/order.ts`，为订单预览、我的订单列表和详情提供带中文关键注释的类型与请求封装；不改变页面行为。执行 `npm run type-check` 通过。

- 2026-09-04 — `frontend/order/preview`: 新增订单预览页，购物车“去结算”改为进入预览，补充订单页路由和登录保护；页面先读取购物车，再只提交商品 ID/数量给后端计算最新金额，并展示固定线下取货点；在当时阶段暂不创建订单、支付或锁库存。执行 `npm run type-check` 通过。

- 2026-09-04 — `frontend/order/list-detail`: 新增我的订单列表和详情页，个人中心订单入口支持全部/待取货/已取货/已取消筛选，补充订单详情和路由登录保护；页面仅展示后端订单与商品快照，不添加取消、取货码或支付操作。执行 `npm run type-check` 和 `npm run build:mp-weixin` 均通过。

- 2026-09-04 — `workspace/incremental-commits`: 记录用户确认的长期协作偏好：后续代码尽量按数据库/契约、后端、测试、前端和文档等小范围拆成多次本地提交；每次提交说明范围与验证结果，默认不推送远程，便于定位和安全回退。

- 2026-09-04 — `workspace/commenting convention`: 记录用户确认的长期代码注释要求：前后端代码都补充关键中文注释，后端额外详细说明分层、请求链路、安全边界、异常、事务、并发、幂等和数据库影响；仅更新项目记忆，未修改业务代码。

- 2026-09-04 — `backend/documentation`: 为后端 Java 主代码和测试代码补充中文类、方法、字段和关键流程注释；新增 `公共知识/后端代码阅读手册.md`，说明前端开发者的阅读路径、请求链路、分层职责、常见故障定位和后端开发顺序；同步扩展 `公共知识/后端知识点.md`。注释和文档不改变运行逻辑；使用仓库本地 Maven 缓存执行 `mvn "-Dmaven.repo.local=E:\\ai_mall\\.m2\\repository" test`，22 个测试全部通过。

- 2026-09-02 — `公共知识可视化`: 重做 `公共知识/优惠券功能设计详细交互版.html`，参考主流电商生命周期补充可用、锁券、支付核销、取消释放、退款回补、订单快照和对账视角；文件仍只写入 E 盘。

- 2026-09-02 — `公共知识可视化`: 新增 `公共知识/优惠券功能设计详细交互版.html`，按数据模型、人工发券、分享领取、订单核销和面试回答五个专题展示优惠券设计；文件写入 E 盘，未修改业务代码。

- 2026-09-02 — `公共知识文档`: 新增 `公共知识/优惠券功能设计详细版.md`，整理优惠券领域模型、接口链路、事务幂等并发、安全边界、测试验收和面试问答；文档依据现有设计与实现编写，未执行代码验证。

- 2026-09-02 — `workspace/file-path-policy`：记录用户要求：后续文件、缓存、构建产物、可视化内容和临时文件均不得写入 C 盘；删除 C 盘已有文件前需要确认。仅文档变更；通过复核本条项目记忆完成验证。

- 2026-09-02 — `workspace/communication preference`：记录数据库、后端和运维主题必须从前端开发者视角清晰讲解，包括术语、用途、执行位置、预期结果和操作风险。仅文档变更；无需运行时验证。

- 2026-09-01 — `deployment documentation`：扩展 `公共知识/GitHub Actions自动部署.md`，形成逐步点击、逐条命令的指南，包含预期输出、成功/失败检查、回滚、不上传内容和后续 `公共知识` 文档的标准格式。仅文档变更；未执行部署或服务器操作。

- 2026-09-01 — `deployment automation`：新增 GitHub Actions 后端部署工作流和服务器端 JAR 部署脚本，支持 SSH 上传、备份、健康检查和回滚；新增 `公共知识/GitHub Actions自动部署.md`，记录一次性服务器准备、SSH 密钥配置、触发、验证和回滚步骤。只执行仓库静态检查；未重启服务器，也未运行 GitHub Actions。

- 2026-09-01 — `公共知识文档`: 删除 `公共知识/本地与线上部署排错.md` 中多余空行，保留命令、分类和安全说明；未做额外验证。

- 2026-09-01 — `公共知识文档`: 新增 `公共知识/本地与线上部署排错.md`，按本地 PowerShell、服务器 Linux、配置、打包、日志和端口分类整理常用命令，并补充换行符、jar 打包和危险命令说明；未记录真实凭据，未做额外验证。

- 2026-09-01 — `公共知识文档`: 新增简洁的 `公共知识/Navicat连接线上数据库.md`，记录通过 SSH 隧道连接线上 MySQL 的命令、Navicat 参数和常见错误；未记录任何真实凭据，文档未做额外验证。

- 2026-08-31 — `admin/environment-config`：将 Admin 开发与生产 API 来源拆分到 `.env.development` 和 `.env.production`；集中构造 Axios base URL，并让 Vite 开发代理读取同一配置。快速 TypeScript 检查通过；未安装依赖。
- 2026-08-31 — `backend/error-observability`：为此前被隐藏的非预期异常增加服务端堆栈日志，同时保持客户端通用响应不变；日志仅在服务端记录异常类型和堆栈。快速 Maven 编译通过；未运行完整测试套件或运行时验证。

- 2026-08-31 — `coupon/share-claim`：新增安全随机分享令牌（仅持久化哈希）、公开预览、登录领取、创建者/自领保护、每次分享唯一性、模板库存/限额检查、迁移 SQL 和小程序分享/领取页面；更新 OpenAPI、设计状态和学习笔记。后端快速编译及前端/Admin 类型检查通过；未执行数据库迁移、运行时 API 或双账号微信验收。
- 2026-08-31 — `admin/coupon-grants/usability`：将单用户上限冲突提示改为通俗中文，并把人工发券原因默认设为可编辑的“活动发放”。快速 Maven 编译和 Admin 类型检查通过。
- 2026-08-31 — `admin/backend/user-management`：新增 Admin 用户列表/搜索/详情、优惠券摘要、启用/停用接口和 `/users` 页面；使用 Admin 角色保护，将目标限制为 CUSTOMER 用户，响应中不包含微信身份字段。快速 Maven 编译和 Admin 类型检查通过；未运行时测试 API 或完整构建。
- 2026-08-31 — `batch 3/frontend/rollback-share-scope`：完成 Admin 人工发券页面、消费者“我的优惠券”列表/详情、类型化客户端和受保护路由；范围复核后移除尚未编译的分享/领取后端、数据库结构和迁移草稿。快速 Maven 编译、Admin 类型检查和前端类型检查通过；未运行时测试 API 或生产构建。
- 2026-08-31 — `backend learning documentation`：新增简洁的 `后端知识点.md`，结合项目解释认证与授权、后端安全边界、Controller/Service/Mapper/Entity 分层、发券事务、并发、幂等和简短面试回答；记录每完成一个功能后扩展该文档的规则。仅文档变更；未执行验证。
- 2026-08-31 — `batch 3/backend/database/api/coupon-grant`：新增 `coupon_grant` 审计表和不可变的 `user_coupon` 快照表；实现幂等 Admin 人工发券、并发安全的模板库存预留、单用户上限检查、不含敏感信息的 Admin 客户搜索，以及归属由 JWT 决定的用户优惠券分页列表/详情 API，并实时推导过期状态；更新 OpenAPI 和功能状态。跳过测试的 Maven 快速编译通过；未执行 DDL、运行时 API 或并发测试。
- 2026-08-31 — `batch 2/admin/coupon-template`：新增带类型的 Admin 优惠券模板 API 客户端和独立 `/coupon-templates` 页面，支持名称/状态筛选、详情、草稿创建/编辑、固定区间或领取后有效期、字符串金额提交、分享标志展示以及受保护的激活/停用操作；新增导航入口并将仓库侧批次标记为完成。`admin` 中的 `npm run type-check` 通过；未执行生产构建、运行时 API 测试或数据库 DDL。
- 2026-08-31 — `batch 2/backend/database/api/coupon-template`：新增带数据库约束的 `coupon_template` 结构；实现 Admin 模板分页创建/获取/列表/完整草稿更新/激活/停用 API；使用字符串金额输入并转换为 `BigDecimal`，明确互斥的有效期模式、条件状态流转、稳定的 400/404/409 错误和 Admin 角色安全控制；更新 OpenAPI 和功能状态。跳过测试的后端快速编译通过；未执行数据库迁移或运行时 API 测试。
- 2026-08-31 — `coupon/design documentation`：更新 `功能设计方案.md` 的功能一，简要说明优惠券模板用途与不可变性、复核后的 Admin/当前用户/分享/领取接口结构、金额表示、发放审计、不透明分享令牌原因、新用户自动发券边界及设计优缺点。仅文档变更；未执行验证。
- 2026-08-31 — `workspace/confirmation workflow`：记录用户要求：每个可执行请求在修改或运行命令前，必须先复述理解并获得明确确认。仅文档变更；未执行验证。
- 2026-08-31 — `feature-design documentation structure`：按要求恢复根目录单一 `功能设计方案.md`；其中包含内部功能目录，并采用 `功能一：优惠券方案设计`、`功能二：XXX方案设计`、`功能三：XXX方案设计` 等标题。仅文档变更；未执行验证。
- 2026-08-31 — `feature-design documentation structure`：曾将根目录单一功能设计文档替换为 `功能设计方案/README.md` 加按编号拆分的功能文件，并把已有优惠券设计移至 `功能设计方案/01-优惠券.md`。仅文档变更；未执行验证。
- 2026-08-31 — `coupon/design documentation`：新增 `功能设计方案.md`，包含简洁的优惠券路线图、核心数据、不透明分享令牌流程、安全/并发规则、规划 API、本地/真实微信测试策略和面试摘要。仅文档变更；未执行验证。
- 2026-08-31 — `frontend/documentation`：将 `FRONTEND_OPTIMIZATION_NOTES.md` 精简为单页要点格式，涵盖启动、导航、请求去重、个人资料渐进刷新、商品缓存、懒加载、安全边界和简短面试摘要。仅文档变更；未执行验证。
- 2026-08-31 — `frontend/documentation`：新增中文学习与面试指南 `FRONTEND_OPTIMIZATION_NOTES.md`，解释已实现的启动、导航、会话校验、缓存、懒加载、安全边界、权衡、度量和未来扩展决策，并包含汇总的原因/实现/效果/成本矩阵。仅文档变更；未执行验证。
- 2026-08-31 — `frontend/startup/navigation/session/performance`：将小程序启动页和登录后默认去向改为商品首页；集中管理路由常量、安全重定向和标签导航；将导航级认证保护限定为私有购物车数据；新增单飞的 60 秒会话校验及不打扰用户的后台失败处理；个人页立即渲染缓存用户数据；商品列表标签数据缓存 30 秒，同时保留强制刷新/搜索行为；启用商品图片懒加载。已有会话和后端 API 保持兼容。仅通过 `npm run type-check` 验证；未执行完整小程序构建或自动化运行时测试。
- 2026-08-31 — `batch 1/manual acceptance`：修正环境变量继承和非标准 `Content-Type` 处理后，在微信开发者工具中成功完成真实微信小程序登录验收。第一批完成，下一步是第二批优惠券模板管理。仅进行人工运行时验收；未执行额外自动化验证。
- 2026-08-31 — `workspace/verification workflow`：细化用户验证偏好：允许快速编译/类型检查，但除非用户要求，否则避免耗时测试套件、完整构建、依赖安装或大范围验证。仅工作流文档更新。
- 2026-08-31 — `backend/auth/wechat compile fix`：修正 HTTP 状态/正文读取和 Jackson 字节数组解析中的受检 `IOException` 处理，将故障保留在脱敏的供应商边界内且不改变 API 行为。使用 `mvn -DskipTests compile` 快速验证，编译成功；未运行测试。
- 2026-08-31 — `backend/api/auth/wechat`：真实出现 `UnknownContentTypeException` 后复核并加固微信集成：以供应商局部适配器替代依赖 `Content-Type` 的自动反序列化，最多读取 16 KiB 加一个溢出字节，使用 Jackson 解析原始字节，校验 HTTP 状态/微信 errcode/OpenID，区分无效凭据与配置/上游故障，并防止外部消息或敏感请求数据进入日志或客户端响应。更新异常映射、OpenAPI、认证契约和 Controller 回归覆盖。最终实现可编译；按用户验证偏好未运行测试。
- 2026-08-31 — `workspace/engineering principles`：强化项目规则：实现方便和更短代码不能主导方案选择；未来设计必须优先考虑正确性、可维护性、可扩展性、可观测性、可测试性、兼容性和明确的外部服务故障处理。仅文档变更；按用户偏好未验证。
- 2026-08-31 — `backend/auth diagnostics`：新增脱敏的微信 code2Session 失败日志，区分 HTTP 状态、网络/根因类型和响应处理类型，不记录 AppSecret、一次性 code、异常消息或完整请求 URL。行为和客户端错误不变。按用户验证偏好未测试或构建。
- 2026-08-31 — `workspace/verification workflow`：记录用户偏好：普通纯源码修改跳过自动测试/构建；安全、金额、数据库和数据完整性相关变更则必须明确提出验证建议。仅文档变更；按用户偏好未验证。
- 2026-08-31 — `batch 1/backend/frontend/api/auth`：完成仓库侧真实微信小程序登录批次：在 OpenAPI 中记录 `/api/v1/auth/wechat/login` 和 `WechatLoginRequest`，修正认证契约，保留微信登录后的安全重定向，并增加 Controller/Service 测试，覆盖成功、空/无效 code、首次用户创建、老用户复用和禁用用户。未向仓库添加 AppSecret。21 个后端测试、前端类型检查和微信小程序生产构建通过；真实 code2Session 验收仍需开发者使用自有凭据在本地微信开发者工具中完成。
- 2026-08-31 — `backend/MyBatis-Plus convention`：将适用的 Lambda 更新包装器统一为带明确实体类型的 `Wrappers.lambdaUpdate(Entity.class)`，并相应更新商品完整替换。15 个 Maven 测试全部通过。
- 2026-08-31 — `backend/product`：将商品完整表单更新从 `updateById()` 改为显式 `LambdaUpdateWrapper`，覆盖全部七个可编辑字段，使空图片 URL 和描述能清除为 SQL `NULL`；新增 Service 回归测试，断言完整更新集合和两个 null 参数。15 个 Maven 测试全部通过。
- 2026-08-31 — `workspace/change workflow`：记录用户要求：任何代码、配置、API、数据库、构建或工作流修改前，必须先评审并明确批准实施方案；方案需包含备选项、优缺点、风险、推荐理由和验证方式。仅文档变更；已在 `PROJECT_MEMORY.md` 中复核。
- 2026-08-31 — `workspace/engineering conventions`：记录选择框架捷径前评估正确性与可扩展性的要求，并明确完整替换/null 清除语义及所需回归覆盖。仅文档变更；已在 `PROJECT_MEMORY.md` 中复核。

- 2026-08-30 — `frontend/config diagnosis`：确认 UniApp 会根据构建模式指向不同后端/数据库实例；开发配置当时存在重复的本地/远程 API 值，生产环境指向 `124.221.241.24:8081`。未修改运行时代码。
- 2026-08-29 — `workspace`：新增 `AGENTS.md` 和本共享记忆文件，使后续任务可复用架构、命令、约定、状态和近期决策。已确认文件作用域为仓库根目录。
- 2026-08-29 — `admin`：使 Ant Design 和自定义 CSS 与 UniApp 的森林绿/暖金视觉风格一致；重新设计 Admin 登录页以匹配 UniApp 品牌布局。`npm run build` 验证通过。
- 2026-08-28 — `admin`：修复 npm 安装并生成 `package-lock.json`；修复商品搜索/重置中的过期状态行为。TypeScript 检查和生产构建通过。
- 2026-08-28 — `backend/auth`：新增 JWT 请求认证、Admin 商品接口角色保护、refresh token 轮换、退出撤销和当前用户处理。14 个 Maven 测试通过。
