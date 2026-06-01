# Quy ước phân quyền (Authorization Conventions)

> Áp dụng cho MỌI chức năng mới có liên quan đến quyền truy cập. Đọc kỹ trước khi
> thêm endpoint hoặc use case mới. Tham chiếu: `docs/permission-matrix.md`,
> `docs/plan/plan-dynamic-permission.md`.

## Nguyên tắc cốt lõi

Hệ thống đã chuyển sang **phân quyền động (Dynamic RBAC)**. Khi build chức năng mới:

1. **MẶC ĐỊNH: enforce theo PERMISSION động, KHÔNG hard-code role.**
   - Dùng `@PreAuthorize("hasAuthority('TÊN_PERMISSION')")` trên controller.
   - KHÔNG dùng `@PreAuthorize("hasRole('SUPER_ADMIN')")` / `hasAnyRole(...)` cho
     các chức năng nghiệp vụ có thể được admin uỷ quyền.
   - Mỗi permission là một quyền hạt mịn (granular), seed trong `DataInitializer`
     và gán cho role qua bảng `role_permissions` (chỉnh được runtime qua UI Phân quyền).

2. **NGOẠI LỆ: chỉ giữ rule cố định khi quyền đó về bản chất KHÔNG được uỷ quyền.**
   Ví dụ hợp lệ để hard-code:
   - Hành động chỉ dành cho chính chủ tài nguyên (self-service): sửa profile của
     chính mình, đổi mật khẩu của chính mình — xác định bằng `userId` trong JWT,
     không phải permission.
   - Bất biến an toàn hệ thống: `SUPER_ADMIN` luôn full quyền và không bị gỡ
     `MANAGE_ROLE` (chống tự khóa). Xem `RolePermissionPolicy`.
   - Khi hard-code, PHẢI ghi comment giải thích vì sao quyền này không uỷ quyền được.

3. **Domain policy (ownership / enrollment) tách biệt với permission.**
   - Quyền "vào được endpoint" = permission (controller `@PreAuthorize`).
   - Quyền "thao tác trên tài nguyên cụ thể" = domain policy theo ngữ cảnh
     (`CourseOwnershipPolicy`, `LessonAuthorizationService`): ví dụ INSTRUCTOR chỉ
     sửa course của chính mình, MEMBER chỉ xem lesson đã enrolled.
   - Một request có thể QUA permission nhưng vẫn bị domain policy chặn (403). Đây là
     thiết kế đúng: hai tầng khác mục đích.

## Một tầng enforce — KHÔNG nhân đôi

- Authorization đặt ở **controller** bằng `@PreAuthorize("hasAuthority(...)")`.
- **KHÔNG** lặp lại kiểm tra role/permission trong use case (vd
  `if (!CourseOwnershipPolicy.hasFullAccess(role)) throw ...`). Use case chỉ chứa
  business rule + domain policy theo ngữ cảnh (ownership, priceLock, enrollment).
- Lý do: nếu use case còn chặn cứng theo role, admin gán permission động sẽ KHÔNG
  có tác dụng (đây chính là bug đã gặp với Voucher / Admin Course).

## Cơ chế enforce (Hybrid cache — đã chốt)

- JWT chỉ chứa `role` (KHÔNG nhúng danh sách permission).
- `JwtAuthenticationFilter` mỗi request nạp permission của role từ
  `PermissionCacheService` (cache in-memory) và set authorities gồm **cả**:
  - `ROLE_<role>` (giữ tương thích `hasRole`/`hasAnyRole` cũ trong giai đoạn migrate)
  - từng permission (không prefix) cho `hasAuthority('PERMISSION')`.
- Khi đổi ma trận quyền → gọi `permissionCacheService.reload()` → hiệu lực tức thì,
  user không cần đăng nhập lại.

## Checklist khi thêm một chức năng mới

1. Định nghĩa permission mới (nếu cần) trong `DataInitializer.initPermissions()`
   (idempotent theo tên) + gán cho role mặc định trong `assignPermissionsToRoles()`.
2. Thêm permission vào `docs/permission-matrix.md` (bảng + ánh xạ endpoint).
3. Controller: `@PreAuthorize("hasAuthority('PERMISSION_MỚI')")`.
4. Use case: KHÔNG kiểm tra role/permission; chỉ business rule + domain policy.
5. FE: gate menu/nút bằng `hasPermission(adminUser, 'PERMISSION_MỚI')`
   (`fe/src/lib/permissions.js`), KHÔNG gate bằng `roles: [...]`. Thêm nhãn vào
   `PERMISSION_LABELS` + nhóm phù hợp trong `PERMISSION_GROUPS`.
6. FE lấy `permissions` từ login response và `GET /users/me/profile`; admin portal
   tự refresh quyền khi vào (`AdminLayout` gọi `refreshAdminPermissions`).
7. Thêm/đồng bộ tài liệu API trong `docs/API/`.

## Lưu ý nợ kỹ thuật hiện tại (role-name coupling)

Một số chỗ vẫn kiểm tra theo TÊN role cứng (`Role.isStaff()`, `isInstructor()`,
strategy `supports(role)`...). Các chỗ này gắn với 5 role hệ thống cố định.
Khi thêm chức năng mới, ưu tiên permission-based (`hasAuthority(...)`) để không làm
nợ này phình thêm; tránh bổ sung thêm nhánh kiểm tra theo tên role.
