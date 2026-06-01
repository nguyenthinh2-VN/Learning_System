# Kế hoạch: Phân quyền động cho ADMIN (Dynamic RBAC)

> Trạng thái: **ĐÃ DUYỆT — đang triển khai**
> Liên quan: `docs/permission-matrix.md`, `infrastructure/config/DataInitializer.java`, `infrastructure/config/SecurityConfig.java`, `infrastructure/config/JwtAuthenticationFilter.java`

## 0. Quyết định đã chốt (lock)

| # | Quyết định | Lựa chọn cuối |
|---|-----------|---------------|
| 2.1 | Cơ chế enforce | **C — Hybrid cache** (JWT giữ `role`, filter nạp permission từ cache in-memory theo role, reload khi đổi ma trận) |
| 2.2 | Migration `@PreAuthorize` | **Làm dần song song** — set cả `ROLE_<role>` lẫn permission; migrate endpoint theo nhóm; có integration test trước khi gỡ role-gate |
| 2.3 | Khóa cứng SUPER_ADMIN | **SUPER_ADMIN luôn full quyền, read-only trong UI** — API từ chối mọi thao tác sửa ma trận của role SUPER_ADMIN |
| 2.4 | Domain policy | **Giữ độc lập** — ownership/enrollment vẫn ở code, không đưa vào ma trận động |
| 2.5 | Audit | **Có** — tái dùng pattern JSONL ledger (`PermissionAuditService` → `logs/permission_audit.jsonl`), không thêm bảng DB |
| 3 | API style | **PUT replace-all** — UI ma trận checkbox + nút Lưu (giữ POST/DELETE từng cái là tùy chọn sau) |
| 8 | Mô hình domain | **Giữ `Role` mỏng** + aggregate `RolePermissions` riêng cho luồng quản trị/cache (tránh đụng hàng loạt chỗ `Role.reconstitute` từ JWT) |

## 1. Tổng quan yêu cầu

Hiện tại hệ thống enforce phân quyền **tĩnh** bằng 2 cơ chế:

1. **Role-gate cứng** trong code: `@PreAuthorize("hasAnyRole('STAFF','SUPER_ADMIN')")` rải rác trên controller.
2. **Domain policy**: `CourseOwnershipPolicy`, `LessonAuthorizationService` cho các luật theo ownership/enrollment.

Bảng `permissions` (24 quyền) và `role_permissions` đã được seed (xem `DataInitializer`) nhưng **chưa được dùng để enforce** — chúng mới chỉ là tài liệu.

**Mục tiêu:** Cho phép `SUPER_ADMIN` (chủ sở hữu quyền `MANAGE_ROLE`) **tự gán / gỡ permission cho từng role** qua giao diện admin, và hệ thống **enforce theo permission động** thay vì role-gate cứng. Khi admin đổi ma trận quyền, hành vi phân quyền thay đổi **không cần deploy lại code**.

**Phạm vi (in-scope):**
- API quản trị: xem danh sách permission, xem ma trận role→permission, gán/gỡ permission cho role.
- Chuyển enforcement từ `hasRole(...)` → `hasAuthority('PERMISSION')` dựa trên permission load từ DB.
- JWT mang permission của user (hoặc load runtime) để `@PreAuthorize` đọc được.
- FE: trang "Phân quyền" cho SUPER_ADMIN + menu sidebar gate theo permission thay vì role cứng.

**Ngoài phạm vi (out-of-scope, ghi nhận để sau):**
- Tạo / xóa **role mới** động (chỉ gán quyền cho 5 role có sẵn). Xem mục 9.
- Gán permission **trực tiếp cho user** (user-level override). Hiện chỉ role-level.
- Phân quyền động cho **domain policy** (ownership/enrollment vẫn giữ logic code).

---

## 2. Quyết định thiết kế cần CHỐT trước khi code

> Đây là phần quan trọng nhất cần bạn duyệt. Mỗi quyết định ảnh hưởng lớn đến khối lượng việc.

### 2.1. Cách enforce permission — **3 phương án**

| PA | Cách làm | Ưu | Nhược |
|----|----------|-----|-------|
| **A. JWT mang permissions** | Khi login, load permissions của role → nhúng vào claim `permissions` trong JWT. `JwtAuthenticationFilter` set thành `GrantedAuthority`. | Không query DB mỗi request (nhanh). | Token cũ giữ quyền cũ tới khi hết hạn — đổi ma trận **không có hiệu lực ngay** với user đang đăng nhập. |
| **B. Load runtime mỗi request** | JWT chỉ giữ `role` (như hiện tại). Filter query `role_permissions` → set authorities mỗi request (có cache). | Đổi ma trận **có hiệu lực gần như tức thì** (sau khi clear cache). | Thêm 1 query/cache lookup mỗi request. |
| **C. Hybrid** | JWT giữ role; filter load permission từ **cache in-memory** (`Map<roleName, Set<permission>>`), cache được refresh khi admin đổi ma trận. | Nhanh như A, tươi như B. | Phức tạp hơn: cần cache invalidation. |

**Khuyến nghị: Phương án C (Hybrid)** — vì tính năng này chính là "đổi quyền không cần deploy", nên hiệu lực tức thì là yêu cầu cốt lõi; đồng thời cache theo role (chỉ 5 role) rất rẻ. Token vẫn chỉ chứa `role` → không phình size, không lộ danh sách quyền.

- [ ] **CHỐT:** Chọn A / B / **C** ?

### 2.2. `hasRole` vs `hasAuthority` — chiến lược migration

Spring phân biệt: `hasRole('X')` ⇔ authority `ROLE_X`; `hasAuthority('Y')` ⇔ authority `Y` (không prefix).
Hiện filter set `new SimpleGrantedAuthority("ROLE_" + role)`.

Kế hoạch: filter set **cả hai** — `ROLE_<role>` (giữ tương thích) **và** toàn bộ permission (không prefix). Controller dần chuyển `@PreAuthorize` sang `hasAuthority('PERMISSION')`.

- [ ] **CHỐT:** Migrate **toàn bộ** controller sang `hasAuthority` trong phase này, hay chỉ các endpoint admin mới + giữ role-gate cũ song song (an toàn, làm dần)?
  - Khuyến nghị: **giữ song song** — set cả ROLE_ và permission, migrate endpoint theo từng nhóm, có integration test bao phủ trước khi gỡ role-gate.

### 2.3. Permission nào được phép chỉnh, permission nào "khóa cứng"?

Một số quyền nếu gỡ nhầm sẽ tự khóa chính mình (vd gỡ `MANAGE_ROLE` của SUPER_ADMIN → không ai sửa quyền được nữa).

- [ ] **CHỐT:** SUPER_ADMIN luôn giữ **tối thiểu** `MANAGE_ROLE` (không cho phép gỡ qua API)? Khuyến nghị **có** — chặn ở use case (`SUPER_ADMIN` + `MANAGE_ROLE` là cặp bất biến).
- [ ] **CHỐT:** Có cho phép sửa ma trận của chính role `SUPER_ADMIN` không, hay SUPER_ADMIN luôn full quyền (read-only trong UI)? Khuyến nghị: **SUPER_ADMIN luôn full, không chỉnh** — tránh tình huống tự khóa.

### 2.4. Domain policy có bị ảnh hưởng?

`CourseOwnershipPolicy`, `LessonAuthorizationService` enforce theo ownership/enrollment, **không** theo bảng permission. Giữ nguyên — permission động chỉ thay role-gate ở tầng controller. Một endpoint vẫn có thể qua được `hasAuthority` nhưng bị domain policy chặn (vd INSTRUCTOR xem lesson của course người khác).

- [ ] **CHỐT:** Đồng ý giữ domain policy độc lập, không đưa vào ma trận động?

### 2.5. Audit

- [ ] **CHỐT:** Có ghi audit log mỗi lần đổi ma trận (ai, role nào, thêm/gỡ permission gì, lúc nào) không? Khuyến nghị **có** — tái dùng pattern ledger (JSONL) hoặc bảng `permission_audit`. Tính năng quyền là nhạy cảm.

---

## 3. Phân quyền cho chính tính năng này

| Hành động | Endpoint | Permission yêu cầu |
|-----------|----------|--------------------|
| Xem danh sách tất cả permission | `GET /api/v1/admin/permissions` | `MANAGE_ROLE` |
| Xem ma trận role→permission | `GET /api/v1/admin/roles/permissions` | `MANAGE_ROLE` |
| Xem permission của 1 role | `GET /api/v1/admin/roles/{roleName}/permissions` | `MANAGE_ROLE` |
| Cập nhật permission cho 1 role (set toàn bộ) | `PUT /api/v1/admin/roles/{roleName}/permissions` | `MANAGE_ROLE` |
| Gán 1 permission | `POST /api/v1/admin/roles/{roleName}/permissions/{permName}` | `MANAGE_ROLE` |
| Gỡ 1 permission | `DELETE /api/v1/admin/roles/{roleName}/permissions/{permName}` | `MANAGE_ROLE` |

> Theo ma trận hiện tại, chỉ `SUPER_ADMIN` có `MANAGE_ROLE`. Sau khi enforce động, ai được admin gán `MANAGE_ROLE` cũng vào được.

- [ ] **CHỐT API style:** Dùng `PUT .../permissions` (gửi nguyên danh sách, replace toàn bộ — UI dạng checkbox lưu 1 lần) hay POST/DELETE từng cái (toggle ngay)? Khuyến nghị **PUT replace toàn bộ** cho UI ma trận checkbox + bấm "Lưu"; giữ POST/DELETE là tùy chọn.

---

## 4. Mô hình dữ liệu

**Không cần đổi schema** — `roles`, `permissions`, `role_permissions` đã đủ. Chỉ thêm thao tác ghi vào `role_permissions`.

Tùy chọn (nếu chốt 2.5 = có audit):

```sql
-- Bảng audit (tùy chọn)
CREATE TABLE permission_audit (
  id          BIGINT PRIMARY KEY AUTO_INCREMENT,
  actor_id    BIGINT       NOT NULL,        -- user thực hiện
  role_name   VARCHAR(50)  NOT NULL,        -- role bị sửa
  permission  VARCHAR(100) NOT NULL,
  action      VARCHAR(10)  NOT NULL,        -- GRANT | REVOKE
  created_at  TIMESTAMP    NOT NULL
);
```

> Hoặc tái dùng JSONL ledger (như `PurchaseLedgerService`) với event `PERMISSION_GRANTED` / `PERMISSION_REVOKED` để khỏi thêm bảng. — chốt ở 2.5.

---

## 5. Các file cần tạo mới / cập nhật (theo Clean Architecture)

### 5A. Domain Layer

```
domain/model/
  └── Role.java                              (UPDATE — thêm Set<Permission> permissions + method
                                              hasPermission(name), grant(p), revoke(p); hoặc giữ Role
                                              mỏng và tạo aggregate RolePermissions riêng — xem ghi chú 8)
domain/service/
  └── RolePermissionPolicy.java              (NEW — luật bất biến: SUPER_ADMIN không gỡ MANAGE_ROLE,
                                              không sửa ma trận SUPER_ADMIN; chốt 2.3)
domain/exception/
  ├── PermissionNotFoundException.java       (NEW — 404)
  ├── RoleNotFoundException.java             (NEW / reuse — đã có ROLE_NOT_FOUND ở ErrorCode)
  └── ProtectedPermissionException.java      (NEW — 400/409, khi cố gỡ quyền bất biến)
```

### 5B. Application Layer

```
application/repository/
  ├── PermissionRepository.java              (NEW — findAll, findByName, existsByName)
  └── RolePermissionRepository.java          (NEW — findPermissionsByRoleName, setPermissions(role, perms),
                                              grant, revoke, findMatrix())
                                              (hoặc mở rộng RoleRepository hiện có)

application/dto/Permission/
  ├── PermissionOutput.java                  (NEW — id, name, description)
  ├── RolePermissionsOutput.java             (NEW — roleName + List<PermissionOutput>)
  ├── PermissionMatrixOutput.java            (NEW — List<RolePermissionsOutput> cho toàn ma trận)
  └── UpdateRolePermissionsInput.java        (NEW — roleName, List<String> permissionNames, actorId)

application/usecase/Permission/
  ├── GetAllPermissionsUseCase.java          (NEW)
  ├── GetPermissionMatrixUseCase.java        (NEW — trả toàn bộ ma trận role→permission)
  ├── GetRolePermissionsUseCase.java         (NEW — 1 role)
  └── UpdateRolePermissionsUseCase.java      (NEW — replace permission của 1 role; áp RolePermissionPolicy;
                                              ghi audit; refresh cache — chốt 2.1)
```

### 5C. Adapter Layer

```
adapter/repository/jpa/role_permissionEntity/
  ├── RolePermissionJpaEntity.java           (UPDATE — bổ sung factory để tạo entity; hiện constructor
                                              protected nên DataInitializer phải dùng native query)
  ├── JpaPermissionRepository.java           (NEW — Spring Data: findByName, findAll)
  └── JpaRolePermissionRepository.java       (NEW — findByRole_Name, deleteByRole_Id, existsByRole_IdAndPermission_Id)

adapter/repository/
  ├── PermissionRepositoryImpl.java          (NEW)
  └── RolePermissionRepositoryImpl.java      (NEW — map entity ↔ domain)

adapter/dto/request/Permission/
  └── UpdateRolePermissionsRequest.java      (NEW — { permissions: ["VIEW_COURSE", ...] })

adapter/dto/response/
  ├── PermissionResponse.java                (NEW)
  └── PermissionMatrixResponse.java          (NEW)

adapter/controller/
  └── AdminPermissionController.java         (NEW — /api/v1/admin/permissions, /api/v1/admin/roles/**)
```

### 5D. Infrastructure Layer

```
infrastructure/config/
  ├── JwtAuthenticationFilter.java           (UPDATE — set thêm authorities là permission, không chỉ ROLE_)
  ├── SecurityConfig.java                    (UPDATE nếu cần — đã có @EnableMethodSecurity rồi)
  ├── PermissionCacheService.java            (NEW — chốt 2.1 PA C: Map<roleName, Set<String>> + reload())
  └── DataInitializer.java                   (giữ — đã seed 24 permission; KHÔNG đổi)

infrastructure/exception/
  ├── ErrorCode.java                         (UPDATE — PERMISSION_NOT_FOUND, PROTECTED_PERMISSION)
  └── GlobalExceptionHandler.java            (UPDATE — handler cho exception mới)

infrastructure/service/ (tùy chọn — chốt 2.5)
  └── PermissionAuditService.java            (NEW — ghi audit GRANT/REVOKE)
```

### 5E. Frontend (fe/src)

```
api/adminApi.js                              (UPDATE — getPermissionsApi, getPermissionMatrixApi,
                                              updateRolePermissionsApi)
pages/admin/AdminPermissionsPage.jsx         (NEW — bảng ma trận checkbox role × permission, nút Lưu)
components/layout/AdminSidebar.jsx           (UPDATE — thêm menu "Phân quyền" (MANAGE_ROLE);
                                              dần chuyển gate item.roles → item.permission)
context/AuthContext.jsx                      (UPDATE — lưu permissions của user để FE gate UI;
                                              cần BE trả permissions ở /users/me/profile hoặc login)
App.jsx                                       (UPDATE — route /admin/permissions)
lib/permissions.js                           (NEW — helper hasPermission(user, perm) cho FE)
```

---

## 6. Luồng xử lý chi tiết

### 6A. Login → nạp permission (PA C)

```
LoginUseCase.execute
  → xác thực, tạo JWT (giữ nguyên: chỉ claim role, KHÔNG nhúng permissions)
  → trả LoginOutput (+ permissions của role để FE dùng — tùy chọn)

PermissionCacheService (khởi động app + sau mỗi lần đổi ma trận)
  → load toàn bộ role_permissions → Map<roleName, Set<permissionName>>
```

### 6B. Mỗi request → set authorities động

```
JwtAuthenticationFilter.doFilterInternal
  → parse token → role
  → Set<String> perms = permissionCacheService.getPermissions(role)   // O(1) in-memory
  → authorities = [ ROLE_<role> ] ∪ [ perms... ]   // giữ ROLE_ cho tương thích
  → set SecurityContext
@PreAuthorize("hasAuthority('PUBLISH_COURSE')") đọc được perms này.
```

### 6C. Admin cập nhật ma trận

```
PUT /api/v1/admin/roles/STAFF/permissions   body: { permissions: [...] }
  → @PreAuthorize("hasAuthority('MANAGE_ROLE')")
  → UpdateRolePermissionsUseCase.execute(input)
    → RoleRepository.findByName(roleName)          // 404 ROLE_NOT_FOUND
    → validate mọi permissionName tồn tại          // 404 PERMISSION_NOT_FOUND
    → RolePermissionPolicy.check(role, newPerms)    // chặn gỡ quyền bất biến — 2.3
    → RolePermissionRepository.setPermissions(role, perms)  // xóa cũ, insert mới (transactional)
    → permissionCacheService.reload()               // hiệu lực tức thì
    → PermissionAuditService.record(actor, role, diff)  // tùy chọn
    → return ma trận mới của role
```

### 6D. FE gate UI

```
AuthContext lưu user.permissions (lấy từ login / /users/me/profile)
AdminSidebar: item hiển thị nếu hasPermission(user, item.permission)
Trang AdminPermissionsPage: chỉ render khi hasPermission(user, 'MANAGE_ROLE')
```

> Lưu ý bảo mật: FE gate chỉ để ẩn/hiện UI. **Enforcement thật luôn ở BE** (`@PreAuthorize` + permission động). Không tin FE.

---

## 7. Thứ tự triển khai (đề xuất 9 bước)

1. **Repository tầng đọc**: `PermissionRepository` + `RolePermissionRepository` (+ JPA impl) — đọc được permission theo role, đọc full matrix.
2. **PermissionCacheService** (PA C) + tích hợp `JwtAuthenticationFilter` set authorities động (vẫn giữ `ROLE_`). → test: token cũ vẫn vào được endpoint role-gate cũ.
3. **Use case đọc**: `GetAllPermissionsUseCase`, `GetPermissionMatrixUseCase`, `GetRolePermissionsUseCase` + `AdminPermissionController` (GET) + DTO/response.
4. **Domain policy + exception**: `RolePermissionPolicy`, các exception + `ErrorCode` + `GlobalExceptionHandler`.
5. **Use case ghi**: `UpdateRolePermissionsUseCase` + endpoint PUT + `RolePermissionRepository.setPermissions` (transactional) + reload cache.
6. **Audit** (nếu chốt 2.5).
7. **Migrate enforcement**: chuyển `@PreAuthorize` các endpoint admin sang `hasAuthority('PERMISSION')` theo từng nhóm (course-approval → PUBLISH_COURSE/LOCK_COURSE_PRICE, voucher → MANAGE_VOUCHER, user → VIEW_USER/EDIT_USER/CREATE_USER, reports → VIEW_TRANSACTION/VIEW_REVENUE/MANAGE_WALLET). Giữ role-gate cũ tới khi test xanh.
8. **FE**: API + trang ma trận + sidebar gate + AuthContext permissions.
9. **Docs**: cập nhật `permission-matrix.md` (ghi rõ "đã enforce động"), API docs (`docs/API/roles-permissions.md`), business-requirements.

---

## 8. Ghi chú kỹ thuật

- **Domain `Role` mỏng hiện tại** chỉ có id/name/description. Có 2 hướng cho permission:
  - (a) Thêm `Set<Permission>` vào `Role` → `Role` trở thành aggregate. Phải sửa mọi chỗ `Role.reconstitute(...)` (nhiều nơi dùng `Role.reconstitute(null, role, null)` từ JWT claim — sẽ không có permissions). **Rủi ro lan rộng.**
  - (b) **Khuyến nghị:** giữ `Role` mỏng; tạo khái niệm riêng `RolePermissions` (roleName + Set<permission>) chỉ dùng trong luồng quản trị + cache. Tránh đụng vào hàng loạt chỗ tái tạo `Role` từ JWT.
- **`@EnableMethodSecurity` đã bật** trong `SecurityConfig` → `@PreAuthorize` dùng được ngay, không cần cấu hình thêm.
- **`RolePermissionJpaEntity` constructor protected** → cần thêm factory `create(role, permission)` hoặc tiếp tục dùng native query trong impl (như `DataInitializer`). Khuyến nghị thêm factory để impl sạch, đúng Clean Architecture.
- **Transaction cho setPermissions**: xóa toàn bộ row cũ của role + insert mới phải trong **một** `@Transactional` để tránh trạng thái nửa vời.
- **Cache invalidation đa-instance**: nếu sau này chạy nhiều instance, cache in-memory mỗi instance sẽ lệch. MVP 1 instance → ổn. Ghi chú để sau (dùng pub/sub hoặc TTL ngắn).
- **Bẫy tự khóa (lockout)**: `RolePermissionPolicy` bắt buộc SUPER_ADMIN luôn có `MANAGE_ROLE`. Cân nhắc thêm test: gỡ `MANAGE_ROLE` của SUPER_ADMIN → phải bị từ chối.
- **JWT giữ nguyên cấu trúc** (PA C) → không ảnh hưởng token đang lưu ở FE (`adminToken`/`publicToken`). Không cần buộc user login lại.
- **Tương thích `hasRole`**: vì filter vẫn set `ROLE_<role>`, mọi `@PreAuthorize("hasAnyRole(...)")` cũ tiếp tục chạy trong giai đoạn migrate → không vỡ.

---

## 9. Hướng mở rộng tương lai (không làm trong phase này)

- **Quản lý role động** (tạo/sửa/xóa role) — cần thêm CRUD `roles` + ràng buộc xóa role đang có user.
- **Permission cấp user** (override theo từng user) — bảng `user_permissions`, hợp nhất với role permissions khi load.
- **Nhóm permission theo module** trong UI cho dễ nhìn (Course / User / Finance / Content).
- **Permission cho domain policy** (vd cấu hình "INSTRUCTOR xem được lesson của instructor khác") — phức tạp, cần thiết kế riêng.

---

## 10. Tiêu chí hoàn thành (Definition of Done)

- [x] SUPER_ADMIN gán/gỡ permission cho role qua UI, lưu thành công, có hiệu lực **ngay** (không cần login lại / deploy lại) — `PermissionCacheService.reload()` sau mỗi update.
- [x] Endpoint quản trị phân quyền (`/admin/permissions`, `/admin/roles/**`) enforce bằng `hasAuthority('MANAGE_ROLE')`, có unit test cho use case (`UpdateRolePermissionsUseCaseTest`).
- [x] Không thể tự khóa: `RolePermissionPolicy` chặn sửa ma trận SUPER_ADMIN (read-only, luôn full quyền).
- [x] Role-gate cũ vẫn hoạt động trong giai đoạn chuyển tiếp — filter set cả `ROLE_<role>` lẫn permission.
- [x] FE: menu "Phân quyền" gate theo `MANAGE_ROLE`; trang ma trận checkbox role × permission; cột SUPER_ADMIN khóa.
- [x] Docs cập nhật: `permission-matrix.md` ghi chú "đã triển khai phân quyền động".
- [x] Audit log ghi đầy đủ mỗi thay đổi ma trận (`PermissionAuditService` → `logs/permission_audit.jsonl`).

### Còn lại (giai đoạn sau, không bắt buộc cho lần này)
- [ ] Migrate nốt `@PreAuthorize("hasAnyRole(...)")` ở các controller còn lại sang `hasAuthority('PERMISSION')` (course-approval, voucher, user, reports) + integration test rồi mới gỡ role-gate.
- [ ] `GET /users/me/profile` trả thêm `permissions` để FE public refresh quyền khi F5 (hiện chỉ login mới có).
- [ ] Cache đa-instance (pub/sub hoặc TTL) khi scale ngang.

## 12. Trạng thái triển khai (cập nhật 2026-06-01)

**Backend — DONE (compile + unit test xanh):**
- Domain: `RolePermissions`, `RolePermissionPolicy`, exception `PermissionNotFoundException` / `RoleNotFoundException` / `ProtectedRoleException`.
- Application: `PermissionRepository`, `RolePermissionRepository`; use case `GetAllPermissions` / `GetPermissionMatrix` / `GetRolePermissions` / `UpdateRolePermissions`; DTO `Permission/*`.
- Adapter: `JpaPermissionRepository`, `JpaRolePermissionRepository`, impl tương ứng; `RolePermissionJpaEntity.link()`; `AdminPermissionController`; request/response DTO.
- Infrastructure: `PermissionCacheService` (PA C), `PermissionAuditService`; `JwtAuthenticationFilter` set authorities động; `ErrorCode` + `GlobalExceptionHandler` cho exception mới; `LoginUseCase`/`LoginOutput`/`LoginResponse` trả thêm `permissions`.
- Test: `UpdateRolePermissionsUseCaseTest` (5 ca) + `LoginUseCaseTest` cập nhật — đều xanh.

**Frontend — DONE (vite build xanh):**
- `api/adminApi.js`: `getPermissionsApi`, `getPermissionMatrixApi`, `getRolePermissionsApi`, `updateRolePermissionsApi`.
- `lib/permissions.js`: `hasPermission`, nhãn permission/role.
- `pages/admin/AdminPermissionsPage.jsx`: bảng ma trận checkbox + lưu theo từng role + hoàn tác.
- `App.jsx`: route `/admin/permissions`; `AdminSidebar.jsx`: menu "Phân quyền" gate `MANAGE_ROLE`; `AdminLoginPage.jsx`: lưu `permissions` vào adminUser.

> Lưu ý môi trường: `fe/node_modules` thiếu `recharts` (đã khai trong package.json) — đã chạy `npm install` để build được. Lỗi này không liên quan tính năng.

---

## 11. Tóm tắt các câu hỏi cần bạn duyệt

1. **2.1** — Cơ chế enforce: A (JWT mang quyền) / B (load mỗi request) / **C (hybrid cache)** ? → khuyến nghị C.
2. **2.2** — Migrate toàn bộ `@PreAuthorize` ngay hay làm dần song song role-gate? → khuyến nghị làm dần.
3. **2.3** — Khóa cứng `MANAGE_ROLE` cho SUPER_ADMIN + không cho sửa ma trận SUPER_ADMIN? → khuyến nghị có.
4. **2.4** — Giữ domain policy độc lập, không đưa vào ma trận động? → khuyến nghị có.
5. **2.5** — Có audit log thay đổi quyền không? Bảng riêng hay JSONL ledger? → khuyến nghị có.
6. **3** — API cập nhật theo PUT replace-all hay POST/DELETE từng cái? → khuyến nghị PUT replace-all.
7. **8** — Domain: giữ `Role` mỏng + aggregate `RolePermissions` riêng (b) hay nhồi `Set<Permission>` vào `Role` (a)? → khuyến nghị (b).
