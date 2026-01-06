USE demo_db;
CREATE TABLE users (
    -- 1. ID là UUID nên dùng BINARY(16) để tối ưu hiệu suất (nhanh hơn VARCHAR)
    id BINARY(16) NOT NULL PRIMARY KEY,

    -- 2. Các cột thông tin (Dùng snake_case để mapping với camelCase của Java)
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(255),
    phone VARCHAR(20),
    password_hash VARCHAR(255) NOT NULL,

    -- 3. Trạng thái hoạt động
    active BOOLEAN DEFAULT TRUE,

    -- 4. Audit logging (Tự động lưu thời gian tạo và cập nhật)
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    -- 5. Đảm bảo dữ liệu không trùng lặp (Constraints)
    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT uk_users_phone UNIQUE (phone)
);