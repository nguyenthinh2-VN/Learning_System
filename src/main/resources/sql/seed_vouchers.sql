-- ============================================================
-- Seed Vouchers — Learning System Spring
-- Chạy sau khi đã có data courses (seed_data.sql / data.sql)
-- ============================================================

-- 1. PERCENT / ALL_COURSES — giảm 20%, áp dụng tất cả khóa học
INSERT IGNORE INTO vouchers (code, type, value, status, scope, valid_from, valid_to,
    min_order_amount, max_discount, usage_limit, usage_per_user, created_at, updated_at)
VALUES (
    'WELCOME20', 'PERCENT', 20.00, 'ACTIVE', 'ALL_COURSES',
    '2026-01-01 00:00:00', '2026-12-31 23:59:59',
    0.00,       -- không yêu cầu đơn tối thiểu
    100000.00,  -- giảm tối đa 100k
    100,        -- tổng 100 lượt dùng
    1,          -- mỗi user 1 lần
    NOW(), NOW()
);

-- 2. FIXED / ALL_COURSES — giảm thẳng 50k, đơn tối thiểu 100k
INSERT IGNORE INTO vouchers (code, type, value, status, scope, valid_from, valid_to,
    min_order_amount, max_discount, usage_limit, usage_per_user, created_at, updated_at)
VALUES (
    'SAVE50K', 'FIXED', 50000.00, 'ACTIVE', 'ALL_COURSES',
    '2026-01-01 00:00:00', '2026-12-31 23:59:59',
    100000.00,  -- đơn tối thiểu 100k
    0.00,       -- max_discount không áp dụng cho FIXED
    50,
    2,          -- mỗi user 2 lần
    NOW(), NOW()
);

-- 3. PERCENT / SPECIFIC — giảm 30% chỉ cho khóa "Spring Boot Clean Architecture"
INSERT IGNORE INTO vouchers (code, type, value, status, scope, valid_from, valid_to,
    min_order_amount, max_discount, usage_limit, usage_per_user, created_at, updated_at)
VALUES (
    'SPRING30', 'PERCENT', 30.00, 'ACTIVE', 'SPECIFIC_COURSES',
    '2026-01-01 00:00:00', '2026-12-31 23:59:59',
    0.00,
    200000.00,
    20,
    1,
    NOW(), NOW()
);

-- Gán SPRING30 cho course "Spring Boot Clean Architecture"
INSERT IGNORE INTO voucher_courses (voucher_id, course_id)
SELECT v.id, c.id
FROM vouchers v, courses c
WHERE v.code = 'SPRING30'
  AND c.title = 'Spring Boot Clean Architecture';

-- 4. PERCENT / SPECIFIC — giảm 50% cho "Thiết kế Database chuẩn" + "Vue.js 3 thực chiến"
INSERT IGNORE INTO vouchers (code, type, value, status, scope, valid_from, valid_to,
    min_order_amount, max_discount, usage_limit, usage_per_user, created_at, updated_at)
VALUES (
    'COMBO50', 'PERCENT', 50.00, 'ACTIVE', 'SPECIFIC_COURSES',
    '2026-01-01 00:00:00', '2026-12-31 23:59:59',
    0.00,
    150000.00,
    30,
    1,
    NOW(), NOW()
);

INSERT IGNORE INTO voucher_courses (voucher_id, course_id)
SELECT v.id, c.id
FROM vouchers v, courses c
WHERE v.code = 'COMBO50'
  AND c.title IN ('Thiết kế Database chuẩn', 'Vue.js 3 thực chiến');

-- 5. FIXED / ALL_COURSES — đã HẾT HẠN (để test validation)
INSERT IGNORE INTO vouchers (code, type, value, status, scope, valid_from, valid_to,
    min_order_amount, max_discount, usage_limit, usage_per_user, created_at, updated_at)
VALUES (
    'EXPIRED100K', 'FIXED', 100000.00, 'ACTIVE', 'ALL_COURSES',
    '2025-01-01 00:00:00', '2025-12-31 23:59:59',  -- đã hết hạn
    0.00, 0.00, 999, 1,
    NOW(), NOW()
);

-- 6. PERCENT / ALL_COURSES — status INACTIVE (soft-delete, để test)
INSERT IGNORE INTO vouchers (code, type, value, status, scope, valid_from, valid_to,
    min_order_amount, max_discount, usage_limit, usage_per_user, created_at, updated_at)
VALUES (
    'DISABLED10', 'PERCENT', 10.00, 'INACTIVE', 'ALL_COURSES',
    '2026-01-01 00:00:00', '2026-12-31 23:59:59',
    0.00, 0.00, 100, 1,
    NOW(), NOW()
);

-- ============================================================
-- Kết quả sau khi chạy:
--
-- WELCOME20   PERCENT 20%   ALL_COURSES  max 100k  — dùng được
-- SAVE50K     FIXED   50k   ALL_COURSES  min 100k  — dùng được
-- SPRING30    PERCENT 30%   SPECIFIC     chỉ Spring Boot
-- COMBO50     PERCENT 50%   SPECIFIC     DB + Vue.js
-- EXPIRED100K FIXED   100k  ALL_COURSES  — hết hạn → lỗi
-- DISABLED10  PERCENT 10%   ALL_COURSES  — inactive → lỗi
-- ============================================================
