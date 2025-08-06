-- 建立資料庫
CREATE DATABASE IF NOT EXISTS aidemo CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE aidemo;

-- 建立用戶表
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '用戶ID',
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用戶名',
    email VARCHAR(100) NOT NULL COMMENT '電子郵件',
    password VARCHAR(255) NOT NULL COMMENT '密碼',
    age INT DEFAULT NULL COMMENT '年齡',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '建立時間',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新時間',
    INDEX idx_username (username),
    INDEX idx_email (email),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用戶表';

-- 插入測試資料
INSERT INTO users (username, email, password, age) VALUES
('admin', 'admin@example.com', 'admin123', 25),
('user1', 'user1@example.com', 'password123', 30),
('user2', 'user2@example.com', 'password456', 28),
('test', 'test@example.com', 'test123', 22);
