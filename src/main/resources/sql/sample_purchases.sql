-- ============================================================================
-- DỮ LIỆU MẪU: Mua khóa học (cho Admin Revenue Dashboard)
-- ----------------------------------------------------------------------------
-- Chạy SAU khi đã có data.sql (roles, users member1/instructor1, courses).
-- An toàn chạy lại nhiều lần (idempotent):
--   - users: INSERT IGNORE theo username (unique)
--   - wallet_transactions: INSERT IGNORE theo reference_code (unique, prefix BUYSMP)
--   - enrollments: chỉ chèn khi chưa tồn tại cặp (user, course)
--
-- Nghiệp vụ khớp app: mua khóa học tạo 1 enrollment (paid_price) + 1
-- wallet_transaction (source=PURCHASE, status=COMPLETED). Khóa 0đ (Internal/free)
-- chỉ tạo enrollment, KHÔNG tạo transaction → coursesSold > paidPurchaseCount.
--
-- Cách chạy:  mysql -u root -p learning_system < sample_purchases.sql
-- ============================================================================

-- 1) Thêm vài học viên External (member1 đã có ở data.sql) ---------------------
INSERT IGNORE INTO users (username, email, password, name, role_id, is_internal, balance, created_at, updated_at)
SELECT s.uname, s.email,
       '$2a$10$UM/SbcTs7rQdG7qm.hs62OpN/po5zWWNDRzLB085Rzj1d0qGC8wqG', -- password123
       s.fullname, r.id, false, 5000000.00, NOW(), NOW()
FROM roles r
JOIN (
    SELECT 'member2' AS uname, 'member2@example.com' AS email, N'Học viên Hai'  AS fullname
    UNION ALL SELECT 'member3', 'member3@example.com', N'Học viên Ba'
    UNION ALL SELECT 'member4', 'member4@example.com', N'Học viên Bốn'
    UNION ALL SELECT 'member5', 'member5@example.com', N'Học viên Năm'
    UNION ALL SELECT 'member6', 'member6@example.com', N'Học viên Sáu'
) s
WHERE r.name = 'MEMBER';

-- 2) Giao dịch mua khóa học (PURCHASE / COMPLETED) — trải theo ngày -----------
--    seq → reference_code BUYSMPxxxxxx ; days_ago → created_at lùi về quá khứ.
INSERT IGNORE INTO wallet_transactions
    (user_id, reference_code, amount, status, source, note, created_at, completed_at, expired_at)
SELECT u.id,
       CONCAT('BUYSMP', LPAD(s.seq, 6, '0')),
       s.amount, 'COMPLETED', 'PURCHASE',
       CONCAT('Mua khóa: ', c.title),
       DATE_SUB(NOW(), INTERVAL s.days_ago DAY),
       DATE_SUB(NOW(), INTERVAL s.days_ago DAY),
       DATE_ADD(DATE_SUB(NOW(), INTERVAL s.days_ago DAY), INTERVAL 100 YEAR)
FROM (
    SELECT 1  AS seq, 'member1' AS uname, 'Spring Boot Clean Architecture' AS ctitle, 500000.00 AS amount, 1  AS days_ago
    UNION ALL SELECT 2,  'member2', 'Spring Boot Clean Architecture', 500000.00, 2
    UNION ALL SELECT 3,  'member3', 'Vue.js 3 thực chiến',            300000.00, 3
    UNION ALL SELECT 4,  'member4', 'Thiết kế Database chuẩn',        200000.00, 5
    UNION ALL SELECT 5,  'member5', 'Spring Boot Clean Architecture', 500000.00, 8
    UNION ALL SELECT 6,  'member6', 'Vue.js 3 thực chiến',            300000.00, 10
    UNION ALL SELECT 7,  'member2', 'Thiết kế Database chuẩn',        200000.00, 12
    UNION ALL SELECT 8,  'member3', 'Spring Boot Clean Architecture', 500000.00, 15
    UNION ALL SELECT 9,  'member1', 'Vue.js 3 thực chiến',            300000.00, 18
    UNION ALL SELECT 10, 'member4', 'Spring Boot Clean Architecture', 500000.00, 22
    UNION ALL SELECT 11, 'member5', 'Thiết kế Database chuẩn',        200000.00, 26
    UNION ALL SELECT 12, 'member6', 'Spring Boot Clean Architecture', 500000.00, 29
    -- Cũ hơn — để biểu đồ theo THÁNG có dữ liệu tháng trước
    UNION ALL SELECT 13, 'member1', 'Thiết kế Database chuẩn',        200000.00, 40
    UNION ALL SELECT 14, 'member2', 'Vue.js 3 thực chiến',            300000.00, 48
    UNION ALL SELECT 15, 'member3', 'Thiết kế Database chuẩn',        200000.00, 55
    UNION ALL SELECT 16, 'member4', 'Vue.js 3 thực chiến',            300000.00, 63
    UNION ALL SELECT 17, 'member5', 'Vue.js 3 thực chiến',            300000.00, 70
    UNION ALL SELECT 18, 'member6', 'Thiết kế Database chuẩn',        200000.00, 78
) s
JOIN users   u ON u.username = s.uname
JOIN courses c ON c.title    = s.ctitle;

-- 3) Enrollment tương ứng cho mỗi giao dịch mua (chỉ chèn nếu chưa có) ---------
INSERT INTO enrollments (user_id, course_id, paid_price, enrolled_at)
SELECT u.id, c.id, s.amount, DATE_SUB(NOW(), INTERVAL s.days_ago DAY)
FROM (
    SELECT 'member1' AS uname, 'Spring Boot Clean Architecture' AS ctitle, 500000.00 AS amount, 1  AS days_ago
    UNION ALL SELECT 'member2', 'Spring Boot Clean Architecture', 500000.00, 2
    UNION ALL SELECT 'member3', 'Vue.js 3 thực chiến',            300000.00, 3
    UNION ALL SELECT 'member4', 'Thiết kế Database chuẩn',        200000.00, 5
    UNION ALL SELECT 'member5', 'Spring Boot Clean Architecture', 500000.00, 8
    UNION ALL SELECT 'member6', 'Vue.js 3 thực chiến',            300000.00, 10
    UNION ALL SELECT 'member2', 'Thiết kế Database chuẩn',        200000.00, 12
    UNION ALL SELECT 'member3', 'Spring Boot Clean Architecture', 500000.00, 15
    UNION ALL SELECT 'member1', 'Vue.js 3 thực chiến',            300000.00, 18
    UNION ALL SELECT 'member4', 'Spring Boot Clean Architecture', 500000.00, 22
    UNION ALL SELECT 'member5', 'Thiết kế Database chuẩn',        200000.00, 26
    UNION ALL SELECT 'member6', 'Spring Boot Clean Architecture', 500000.00, 29
    UNION ALL SELECT 'member1', 'Thiết kế Database chuẩn',        200000.00, 40
    UNION ALL SELECT 'member2', 'Vue.js 3 thực chiến',            300000.00, 48
    UNION ALL SELECT 'member3', 'Thiết kế Database chuẩn',        200000.00, 55
    UNION ALL SELECT 'member4', 'Vue.js 3 thực chiến',            300000.00, 63
    UNION ALL SELECT 'member5', 'Vue.js 3 thực chiến',            300000.00, 70
    UNION ALL SELECT 'member6', 'Thiết kế Database chuẩn',        200000.00, 78
) s
JOIN users   u ON u.username = s.uname
JOIN courses c ON c.title    = s.ctitle
WHERE NOT EXISTS (
    SELECT 1 FROM enrollments e WHERE e.user_id = u.id AND e.course_id = c.id
);

-- 4) Vài enrollment khóa MIỄN PHÍ (0đ) — KHÔNG tạo transaction ----------------
--    Minh họa: coursesSold (đếm enrollments) > paidPurchaseCount (đếm tx PURCHASE).
INSERT INTO enrollments (user_id, course_id, paid_price, enrolled_at)
SELECT u.id, c.id, 0.00, DATE_SUB(NOW(), INTERVAL s.days_ago DAY)
FROM (
    SELECT 'member1' AS uname, 'Java Core từ cơ bản đến nâng cao' AS ctitle, 4  AS days_ago
    UNION ALL SELECT 'member2', 'Java Core từ cơ bản đến nâng cao', 20
    UNION ALL SELECT 'member3', 'Java Core từ cơ bản đến nâng cao', 60
) s
JOIN users   u ON u.username = s.uname
JOIN courses c ON c.title    = s.ctitle
WHERE NOT EXISTS (
    SELECT 1 FROM enrollments e WHERE e.user_id = u.id AND e.course_id = c.id
);

-- ============================================================================
-- Kiểm tra nhanh sau khi seed:
--   SELECT source, status, COUNT(*) n, SUM(amount) total
--   FROM wallet_transactions GROUP BY source, status;
--
--   SELECT DATE_FORMAT(created_at,'%Y-%m-%d') d, SUM(amount) rev, COUNT(*) c
--   FROM wallet_transactions WHERE source='PURCHASE' AND status='COMPLETED'
--   GROUP BY d ORDER BY d;
-- ============================================================================
