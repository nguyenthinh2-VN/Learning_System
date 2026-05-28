# Kế hoạch redesign Admin UI

**Ngày:** 2026-05-28
**Phạm vi:** `fe/src/pages/admin/*` và `fe/src/components/layout/Admin*.jsx`
**Mục tiêu:** Sửa lỗi UI sidebar admin, áp dụng dark theme (nền tối, chữ sáng), giữ nguyên các mục menu hiện có.

---

## 1. Phân tích lỗi hiện tại

Từ ảnh screenshot, sidebar admin đang gặp các lỗi:

| Lỗi | Nguyên nhân |
|---|---|
| Icon và text trong menu item bị xếp **dọc** (icon ở trên, text ở dưới) | `AdminSidebar.jsx` dùng `asChild` với `<Link>` bên trong `SidebarMenuButton`, nhưng implementation shadcn ở dự án này chạy trên `@base-ui/react` dùng `useRender` (đã thấy ở `AppSidebar.jsx`). Pattern đúng phải là `render={<Link to="..." />}`. Hậu quả: class `flex items-center gap-2` không apply lên Link, các phần tử bên trong stack theo chiều dọc. |
| Logo "LearnSpace / Admin Portal" bị cắt nửa, chữ "Admin Portal" bị wrap | Cùng nguyên nhân trên. |
| Footer "Super Admin Test / Quản trị viên" thụt lề so với menu chính | Cùng nguyên nhân, layout flex không hoạt động. |
| Nút "Đăng xuất" nằm dưới user info nhưng UI lủng củng | Footer chỉ cần 1 button user, nút logout đặt ở dropdown menu hoặc làm icon button bên cạnh. |

Ngoài ra:

- `AdminLayout.jsx` đang wrap toàn bộ tree bằng `<div className="dark">` để force dark mode — OK nhưng cần đảm bảo CSS `--sidebar`, `--background`, `--foreground` trong `.dark` đúng theo yêu cầu (đã verify trong `index.css` — dark theme có sẵn).
- Mỗi page admin có header riêng với `SidebarTrigger` — tốt, giữ nguyên.

---

## 2. Phương án sửa

### 2.1. Sidebar (`AdminSidebar.jsx`)

- **Đổi toàn bộ `asChild` thành `render={<Link to="..." />}`** giống `AppSidebar.jsx`.
- **Tách user info và logout** ở footer:
  - Hàng 1: avatar + tên + role
  - Hàng 2: button "Đăng xuất" riêng (full-width, variant ghost)
- Giữ nguyên 3 nhóm menu:
  - **Tổng quan**: Dashboard
  - **Quản lý**: Người dùng, Khóa học, Chờ duyệt
  - **Tài chính**: Voucher, Cộng tiền
- Giữ nguyên `collapsible="icon"` để có thể thu nhỏ.
- Logo: gradient indigo-purple kèm icon `ShieldCheck`.

### 2.2. Layout (`AdminLayout.jsx`)

- Giữ class `.dark` ở wrapper để force dark mode toàn admin.
- Bỏ `<div className="flex-1 bg-background min-h-screen">` thừa quanh `<Outlet />` (đã có `bg-background` từ `SidebarInset`).
- Wrap thêm `Toaster` từ `sonner` nếu chưa có (kiểm tra `App.jsx`).

### 2.3. Trang Overview (`AdminOverviewPage.jsx`)

- Giữ structure: Header sticky + Welcome + Stats grid + Quick actions.
- Cải thiện:
  - **StatCard**: thêm gradient subtle, border màu `border` thay vì mặc định, hover state nâng nhẹ.
  - **QuickAction**: card style nhất quán với StatCard (rounded-xl, border, hover bg-accent/50).
  - Thêm chip nhỏ hiển thị role bên cạnh tên user.

### 2.4. Các trang còn lại

Giữ nguyên cấu trúc, chỉ cập nhật style nhất quán:

- `AdminCoursesPage` — bảng dạng row, filter chips
- `AdminPendingCoursesPage` — list card lớn
- `AdminUsersPage` — modal thêm user + placeholder bảng
- `AdminVouchersPage` — bảng row + modal form
- `AdminTopUpPage` — form + result card

Các page này hiện đã OK với dark theme (do `.dark` ở layout). Chỉ cần đảm bảo:

- Không hardcode màu trắng/đen
- Dùng token semantic: `bg-card`, `bg-muted`, `text-foreground`, `text-muted-foreground`, `border-border`

---

## 3. Cấu trúc file sẽ thay đổi

```
fe/src/components/layout/AdminSidebar.jsx     # Sửa: asChild → render
fe/src/components/layout/AdminLayout.jsx      # Sửa: bỏ wrapper thừa
fe/src/pages/admin/AdminOverviewPage.jsx      # Refine StatCard + QuickAction
```

5 page admin còn lại không đổi struct, chỉ kiểm tra contrast trong dark mode.

---

## 4. Mock UI để review

File mock: `docs/plan/admin-ui-mock.html`

- Standalone HTML với Tailwind CDN
- Hiển thị 2 màn hình: **Sidebar (expanded + collapsed)** và **Overview Page**
- Mở trực tiếp trong browser để xem trước layout
- Đầy đủ dark theme với token màu giống production

---

## 5. Tiêu chí nghiệm thu

- [ ] Sidebar: icon và text cùng dòng, gap đều
- [ ] Footer sidebar: avatar + tên + role hiển thị đúng, nút logout riêng và rõ ràng
- [ ] Logo "LearnSpace" + "Admin Portal" cùng card, không bị wrap
- [ ] Toàn bộ admin chạy dark mode (nền `oklch(0.145 0 0)`, chữ `oklch(0.985 0 0)`)
- [ ] Hover/active state rõ ràng trên menu items
- [ ] Collapsed mode (Ctrl+B) hoạt động, hiện tooltip
- [ ] Stats card và Quick action card có viền `border-border`, hover state đẹp
- [ ] `npm run build` pass

---

## 6. Quy trình thực hiện

1. ✅ Tạo plan này và mock UI
2. ⏸️  **Đợi user review mock + plan**
3. Sửa `AdminSidebar.jsx` (đổi `asChild` → `render`)
4. Sửa `AdminLayout.jsx` (bỏ wrapper thừa, kiểm tra Toaster)
5. Refine `AdminOverviewPage.jsx`
6. Smoke test 5 page còn lại (chỉ điều chỉnh nếu lệch dark theme)
7. Build kiểm tra: `npm run build`
