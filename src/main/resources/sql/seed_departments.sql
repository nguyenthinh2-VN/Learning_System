-- Insert Level 1 (Khối)
INSERT INTO departments (code, name, parent_id, created_at, updated_at) VALUES 
('BOD_L1', 'Ban Giám Đốc (BOD)', NULL, NOW(), NOW()),
('HR', 'Nhân sự (Human Resources)', NULL, NOW(), NOW()),
('FIN', 'Tài chính (Finance)', NULL, NOW(), NOW()),
('RND', 'Nghiên cứu & Phát triển (R&D)', NULL, NOW(), NOW()),
('ENG', 'Kỹ thuật & Sản phẩm (Engineering)', NULL, NOW(), NOW()),
('SAL', 'Kinh doanh & Tiếp thị (Sales & Marketing)', NULL, NOW(), NOW()),
('IT', 'Hạ tầng & IT (IT / MIS)', NULL, NOW(), NOW()),
('LEG', 'Pháp chế (Legal)', NULL, NOW(), NOW());

-- Retrieve IDs of Level 1 departments
SET @bod_id = (SELECT id FROM departments WHERE code = 'BOD_L1');
SET @hr_id = (SELECT id FROM departments WHERE code = 'HR');
SET @fin_id = (SELECT id FROM departments WHERE code = 'FIN');
SET @rnd_id = (SELECT id FROM departments WHERE code = 'RND');
SET @eng_id = (SELECT id FROM departments WHERE code = 'ENG');
SET @sal_id = (SELECT id FROM departments WHERE code = 'SAL');
SET @it_id = (SELECT id FROM departments WHERE code = 'IT');
SET @leg_id = (SELECT id FROM departments WHERE code = 'LEG');

-- Insert Level 2 (Phòng ban con)
INSERT INTO departments (code, name, parent_id, created_at, updated_at) VALUES 
('BOD', 'Ban Giám Đốc', @bod_id, NOW(), NOW()),

('HR-LND', 'Tuyển dụng & Đào tạo (TA & L&D)', @hr_id, NOW(), NOW()),
('HR-CNB', 'C&B & Quan hệ lao động', @hr_id, NOW(), NOW()),

('FIN-ACC', 'Kế toán (Accounting)', @fin_id, NOW(), NOW()),
('FIN-FPA', 'Tài chính & Kiểm soát nội bộ (FP&A)', @fin_id, NOW(), NOW()),

('RND-SW', 'Nghiên cứu Phần mềm (SW R&D)', @rnd_id, NOW(), NOW()),
('RND-HW', 'Nghiên cứu Phần cứng (HW R&D)', @rnd_id, NOW(), NOW()),

('ENG-PM', 'Quản lý Sản phẩm (PM)', @eng_id, NOW(), NOW()),
('ENG-QA', 'Đảm bảo chất lượng (QA / QC)', @eng_id, NOW(), NOW()),

('SAL-BD', 'Phát triển Kinh doanh (Sales / BD)', @sal_id, NOW(), NOW()),
('SAL-FAE', 'Kỹ sư Hỗ trợ Ứng dụng (FAE)', @sal_id, NOW(), NOW()),
('SAL-MKT', 'Tiếp thị (Marketing)', @sal_id, NOW(), NOW()),

('IT-SYS', 'Quản trị Hệ thống & Mạng (IT Admin)', @it_id, NOW(), NOW()),

('LEG-COR', 'Pháp chế doanh nghiệp', @leg_id, NOW(), NOW());
