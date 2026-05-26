-- Migration: Tạo bảng wallet_transactions
-- Chạy thủ công trên MySQL trước khi khởi động ứng dụng

CREATE TABLE IF NOT EXISTS wallet_transactions (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id        BIGINT NOT NULL,
    reference_code VARCHAR(32) NOT NULL,
    amount         DECIMAL(15,2) NOT NULL,
    status         ENUM('PENDING','COMPLETED','EXPIRED','FAILED') NOT NULL DEFAULT 'PENDING',
    source         ENUM('MOCK','VIETQR','ADMIN') NOT NULL,
    note           VARCHAR(255),
    created_at     TIMESTAMP NOT NULL,
    completed_at   TIMESTAMP NULL,
    expired_at     TIMESTAMP NOT NULL,
    CONSTRAINT uk_wallet_tx_ref UNIQUE (reference_code),
    CONSTRAINT fk_wallet_tx_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE INDEX IF NOT EXISTS idx_wallet_tx_ref ON wallet_transactions(reference_code);
CREATE INDEX IF NOT EXISTS idx_wallet_tx_user ON wallet_transactions(user_id);
CREATE INDEX IF NOT EXISTS idx_wallet_tx_status ON wallet_transactions(status);
