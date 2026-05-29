-- Migration: Thêm cột avatar_url vào bảng users
-- Chạy thủ công trên MySQL trước khi khởi động ứng dụng (sau khi đã có bảng users)

ALTER TABLE users ADD COLUMN avatar_url VARCHAR(500) NULL;
