-- =============================================================
-- DDL: Tạo schema cho Leave Management System
-- DB: camunda | Engine: PostgreSQL 13
-- Thứ tự: đảm bảo đúng dependency (không có circular FK)
-- =============================================================

-- 1. leave_types (không phụ thuộc ai)
CREATE TABLE IF NOT EXISTS leave_types (
    id             UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    code           VARCHAR(50)  NOT NULL UNIQUE,
    name           VARCHAR(100) NOT NULL,
    requires_proof BOOLEAN      NOT NULL DEFAULT FALSE,
    is_paid        VARCHAR(20)
);

-- 2. departments (manager_id sẽ thêm sau khi employees tồn tại)
CREATE TABLE IF NOT EXISTS departments (
    id         UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    code       VARCHAR(50) NOT NULL UNIQUE,
    name       VARCHAR(50) NOT NULL
);

-- 3. employees (phụ thuộc departments)
CREATE TABLE IF NOT EXISTS employees (
    id             UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    full_name      VARCHAR(255) NOT NULL,
    email          VARCHAR(255) NOT NULL UNIQUE,
    position_level INTEGER      NOT NULL,
    department_id  UUID         NOT NULL REFERENCES departments(id),
    manager_id     UUID         REFERENCES employees(id),
    status         VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at     TIMESTAMP    NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_emp_department ON employees (department_id);
CREATE INDEX IF NOT EXISTS idx_emp_manager    ON employees (manager_id);
CREATE INDEX IF NOT EXISTS idx_emp_status     ON employees (status);

-- 4. Thêm cột manager_id vào departments (sau khi employees tồn tại)
ALTER TABLE departments
    ADD COLUMN IF NOT EXISTS manager_id UUID REFERENCES employees(id);
CREATE INDEX IF NOT EXISTS idx_dept_manager ON departments (manager_id) WHERE manager_id IS NOT NULL;

-- 5. holidays (độc lập)
CREATE TABLE IF NOT EXISTS holidays (
    id           BIGSERIAL    PRIMARY KEY,
    holiday_date DATE         NOT NULL UNIQUE,
    description  VARCHAR(255)
);

-- 6. leave_balances (phụ thuộc employees + leave_types)
CREATE TABLE IF NOT EXISTS leave_balances (
    id            UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
    employee_id   UUID           NOT NULL REFERENCES employees(id),
    leave_type_id UUID           NOT NULL REFERENCES leave_types(id),
    year          INTEGER        NOT NULL,
    total_days    NUMERIC(4, 1)  NOT NULL,
    used_days     NUMERIC(4, 1)  NOT NULL DEFAULT 0.0,
    pending_days  NUMERIC(4, 1)  NOT NULL DEFAULT 0.0,
    version       INTEGER        NOT NULL DEFAULT 0,
    CONSTRAINT uq_lb_emp_type_year UNIQUE (employee_id, leave_type_id, year)
);
CREATE INDEX IF NOT EXISTS idx_lb_employee_year ON leave_balances (employee_id, year);

-- 7. leave_requests (phụ thuộc employees + leave_types)
CREATE TABLE IF NOT EXISTS leave_requests (
    id                    UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
    full_name             VARCHAR(255)   NOT NULL,
    business_key          VARCHAR(100)   NOT NULL UNIQUE,
    employee_id           UUID           NOT NULL REFERENCES employees(id),
    current_assignee_id   UUID           REFERENCES employees(id),
    current_assignee_name VARCHAR(255),
    leave_type_id         UUID           NOT NULL REFERENCES leave_types(id),
    start_date            TIMESTAMP      NOT NULL,
    leave_session         VARCHAR(20)    NOT NULL,
    end_date              TIMESTAMP      NOT NULL,
    total_working_days    NUMERIC(4, 1)  NOT NULL,
    reason                TEXT,
    status                VARCHAR(20),
    attachment_url        VARCHAR(500),
    updated_at            TIMESTAMP,
    created_at            TIMESTAMP      NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_lr_employee_status  ON leave_requests (employee_id, status);
CREATE INDEX IF NOT EXISTS idx_lr_assignee_status  ON leave_requests (current_assignee_id, status);
CREATE INDEX IF NOT EXISTS idx_lr_dates            ON leave_requests (start_date, end_date);

-- 8. approval_histories (lưu leave_request_id dạng raw UUID, không FK)
CREATE TABLE IF NOT EXISTS approval_histories (
    id               UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    leave_request_id UUID        NOT NULL,
    approver_email   VARCHAR(255) NOT NULL,
    level            INTEGER     NOT NULL,
    action           VARCHAR(20) NOT NULL,
    comment          TEXT,
    created_at       TIMESTAMP   NOT NULL DEFAULT NOW()
);
