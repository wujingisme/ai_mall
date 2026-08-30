# AI Mall shared project memory

Last updated: 2026-08-31

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
- UniApp API destination is controlled by `frontend/.env.*`. As of 2026-08-30, `.env.development` contains duplicate `VITE_API_BASE_URL` declarations (remote first, local second, so the last value normally wins), while `.env.production` points to `http://124.221.241.24:8081`. Confirm the active build mode before deciding which database received a write.
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

## API and authentication

- Base API prefix: `/api/v1`.
- Public consumer product endpoints: `/api/v1/shop/products/**`.
- Admin product endpoints: `/api/v1/products/**`; require `ADMIN` or `OPERATOR` authority.
- Public auth endpoints include register, login, refresh, and logout.
- `/api/v1/auth/me` requires a valid Bearer access token.
- Access tokens are signed JWTs and include a `roles` claim.
- Refresh tokens are opaque, stored as hashes, rotated on refresh, and revoked on logout.
- Admin Axios logic lives in `admin/src/utils/request.ts`; session storage lives in `admin/src/utils/auth.ts`.
- UniApp request/session logic lives under `frontend/src/utils/`.

## Current UI direction

Both clients should share the same visual language:

- Deep forest green: approximately `#173f34`, `#245d4d`, `#28604f`.
- Warm gold accent: approximately `#e4bd6d` / `#e4c57d`.
- Soft gray-green page background: `#f3f6f4` / `#f6f7f4`.
- Primary text: `#17362d`; secondary text uses muted gray-green.
- Rounded cards, restrained green shadows, and clean product-focused layouts.

The React admin theme is configured in `admin/src/main.tsx`, with application CSS in `admin/src/styles.css`. Its login page intentionally mirrors the UniApp login page's two-panel brand treatment.

## Important implementation notes

- Before making any code, configuration, API, database, build, or workflow change, present the proposed design to the user first and wait for explicit approval. The proposal should identify the intended behavior, affected modules/files, API and data semantics, compatibility or migration impact, verification plan, and the viable options with each option's advantages, disadvantages, risks, and suitable use cases. State a recommendation and its rationale, then implement only after the user confirms. Read-only diagnosis and explanation may proceed without approval.
- Choose implementations from explicit API and data semantics, not merely the shortest framework helper. Before coding, consider correctness, null/empty/absent states, failure paths, security, maintainability, and likely extension points.
- A full-form product edit is a full replacement of editable fields: optional fields submitted empty must be persisted as SQL `NULL`. Do not use an ORM helper that silently ignores `null` unless that selective-update behavior is intentionally part of the endpoint contract.
- When a MyBatis-Plus lambda update wrapper is the appropriate design, construct it with `Wrappers.lambdaUpdate(Entity.class)` so the entity type is explicit and the style is consistent. This convention does not override choosing custom SQL or another persistence mechanism when that better matches the operation.
- For material behavior changes, add regression coverage for normal updates, clearing an existing optional value, validation/conflicts, and relevant authorization paths.

- React admin routes are defined in `admin/src/App.tsx`.
- Admin product CRUD UI is in `admin/src/pages/ProductsPage.tsx`.
- Product filtering uses explicit query overrides when search/reset changes page or filter state, avoiding stale React state in immediate requests.
- Product `PUT /api/v1/products/{id}` is a full replacement of all admin-editable fields. `ProductService` uses `Wrappers.lambdaUpdate(Product.class)` with an explicit `.set(...)` list so optional `imageUrl` and `description` values normalized to `null` are written as SQL `NULL`; do not replace this with default `updateById()` selective-update behavior.
- JWT parsing is implemented in `backend/src/main/java/com/aimall/auth/service/JwtService.java`.
- JWT request authentication is implemented under `backend/src/main/java/com/aimall/auth/security/` and registered by `SecurityConfig`.
- Keep consumer product reads on `/shop/products`; do not expose admin CRUD endpoints for consumer use.
- API shape changes must also update `api/openapi.yaml` and affected TypeScript types/clients.

## Verification baseline

As of 2026-08-29:

- `admin`: `npm run build` succeeds. Vite reports a non-blocking large-chunk warning (the main JS bundle is over 500 kB).
- `backend`: Maven test suite succeeds with 15 tests when using the repository-local Maven cache command above.

## Known follow-ups

- Admin bundle can be reduced later with route-level dynamic imports/manual chunks.
- When diagnosing login connectivity, first check whether port 8080 is listening and inspect backend startup logs before changing frontend code.
- Clean up the duplicate UniApp development API environment values and define an explicit per-target strategy: H5 development can use the Vite `/api` proxy, while a real device/emulator must use an address reachable from that device. `localhost` on a device is not the development PC.
- Do not assume a successful UI route guard secures an API; authorization must remain enforced by Spring Security.

## Change log

- 2026-08-31 — `backend/MyBatis-Plus convention`: standardized appropriate lambda update wrapper construction on `Wrappers.lambdaUpdate(Entity.class)` with explicit entity typing; updated product replacement accordingly. Verified with all 15 Maven tests passing.
- 2026-08-31 — `backend/product`: changed full-form product updates from `updateById()` to an explicit `LambdaUpdateWrapper` covering all seven editable fields, allowing empty image URLs and descriptions to clear existing database values as SQL `NULL`; added a service regression test that asserts the complete update set and both null parameters. Verified with all 15 Maven tests passing.
- 2026-08-31 — `workspace/change workflow`: recorded the user requirement that implementation proposals, including alternatives, advantages, disadvantages, risks, recommendation rationale, and verification, must be reviewed and explicitly approved before any code, configuration, API, database, build, or workflow modification. Documentation-only change; reviewed in `PROJECT_MEMORY.md`.
- 2026-08-31 — `workspace/engineering conventions`: recorded the requirement to evaluate correctness and extensibility before selecting framework shortcuts, and clarified full-form replacement/null-clearing semantics plus required regression coverage. Documentation-only change; reviewed in `PROJECT_MEMORY.md`.

- 2026-08-30 — `frontend/config diagnosis`: confirmed that UniApp can target different backend/database instances depending on build mode; development currently has duplicate local/remote API values and production targets `124.221.241.24:8081`. No runtime code changed.
- 2026-08-29 — `workspace`: added `AGENTS.md` and this shared memory file so future tasks can reuse architecture, commands, conventions, status, and recent decisions. Verified the files are repository-root scoped.
- 2026-08-29 — `admin`: aligned Ant Design and custom CSS with the UniApp forest-green/warm-gold visual style; redesigned the admin login page to match the UniApp brand layout. Verified with `npm run build`.
- 2026-08-28 — `admin`: repaired the npm installation and generated `package-lock.json`; fixed stale-state behavior in product search/reset. Verified TypeScript and production build.
- 2026-08-28 — `backend/auth`: added JWT request authentication, role protection for admin product endpoints, refresh-token rotation, logout revocation, and current-user handling. Verified with 14 Maven tests.
