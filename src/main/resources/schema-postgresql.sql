-- ============================================================================
-- RITZ Wallet - PostgreSQL Database Schema
-- Rajalakshmi Institute of Technology (RIT) Campus Payment Infrastructure
-- ============================================================================

-- 1. Users table (Authentication & Roles)
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(64) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(128) NOT NULL,
    email VARCHAR(128) NOT NULL UNIQUE,
    role VARCHAR(32) NOT NULL,
    student_id VARCHAR(64) NULL,
    merchant_id VARCHAR(64) NULL,
    application_id VARCHAR(64) NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_users_role ON users (role);
CREATE INDEX IF NOT EXISTS idx_users_student_id ON users (student_id);
CREATE INDEX IF NOT EXISTS idx_users_merchant_id ON users (merchant_id);

-- 2. Students table
CREATE TABLE IF NOT EXISTS students (
    id BIGSERIAL PRIMARY KEY,
    student_id VARCHAR(64) NOT NULL UNIQUE,
    name VARCHAR(128) NOT NULL,
    email VARCHAR(128) NOT NULL UNIQUE,
    mobile_number VARCHAR(20) NOT NULL,
    department VARCHAR(64) NOT NULL,
    batch_year INT NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_students_department ON students (department);
CREATE INDEX IF NOT EXISTS idx_students_status ON students (status);

-- 3. Wallets table (Student balances in integer minor units)
CREATE TABLE IF NOT EXISTS wallets (
    id BIGSERIAL PRIMARY KEY,
    wallet_id VARCHAR(64) NOT NULL UNIQUE,
    student_id VARCHAR(64) NOT NULL UNIQUE,
    balance_minor_units BIGINT NOT NULL DEFAULT 0,
    currency VARCHAR(16) NOT NULL DEFAULT 'RITZ',
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    last_transaction_at TIMESTAMP NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_wallets_student FOREIGN KEY (student_id) REFERENCES students (student_id) ON DELETE RESTRICT
);
CREATE INDEX IF NOT EXISTS idx_wallets_status ON wallets (status);
CREATE INDEX IF NOT EXISTS idx_wallets_student_id ON wallets (student_id);

-- 4. Double-Entry Ledger Accounts
CREATE TABLE IF NOT EXISTS ledger_accounts (
    id BIGSERIAL PRIMARY KEY,
    account_number VARCHAR(64) NOT NULL UNIQUE,
    account_name VARCHAR(128) NOT NULL,
    account_type VARCHAR(32) NOT NULL,
    balance_minor_units BIGINT NOT NULL DEFAULT 0,
    currency VARCHAR(16) NOT NULL DEFAULT 'RITZ',
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_ledger_accounts_type ON ledger_accounts (account_type);

-- 5. Double-Entry Ledger Entries (Strictly Immutable)
CREATE TABLE IF NOT EXISTS ledger_entries (
    id BIGSERIAL PRIMARY KEY,
    entry_id VARCHAR(64) NOT NULL UNIQUE,
    transaction_id VARCHAR(64) NOT NULL,
    account_number VARCHAR(64) NOT NULL,
    entry_type VARCHAR(16) NOT NULL,
    amount_minor_units BIGINT NOT NULL,
    balance_after_minor_units BIGINT NOT NULL,
    description VARCHAR(255) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ledger_entries_account FOREIGN KEY (account_number) REFERENCES ledger_accounts (account_number) ON DELETE RESTRICT
);
CREATE INDEX IF NOT EXISTS idx_ledger_entries_txn ON ledger_entries (transaction_id);
CREATE INDEX IF NOT EXISTS idx_ledger_entries_account ON ledger_entries (account_number);
CREATE INDEX IF NOT EXISTS idx_ledger_entries_created ON ledger_entries (created_at);

-- 6. Transactions table
CREATE TABLE IF NOT EXISTS transactions (
    id BIGSERIAL PRIMARY KEY,
    transaction_id VARCHAR(64) NOT NULL UNIQUE,
    reference_id VARCHAR(128) NULL,
    idempotency_key VARCHAR(128) NULL,
    source_account VARCHAR(64) NOT NULL,
    destination_account VARCHAR(64) NOT NULL,
    amount_minor_units BIGINT NOT NULL,
    currency VARCHAR(16) NOT NULL DEFAULT 'RITZ',
    transaction_type VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    initiating_application_id VARCHAR(64) NULL,
    merchant_id VARCHAR(64) NULL,
    student_id VARCHAR(64) NULL,
    description VARCHAR(255) NULL,
    previous_balance_minor_units BIGINT NULL,
    current_balance_minor_units BIGINT NULL,
    failure_reason VARCHAR(255) NULL,
    metadata TEXT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_tx_ref ON transactions (reference_id);
CREATE INDEX IF NOT EXISTS idx_tx_idempotency ON transactions (idempotency_key);
CREATE INDEX IF NOT EXISTS idx_tx_student ON transactions (student_id);
CREATE INDEX IF NOT EXISTS idx_tx_merchant ON transactions (merchant_id);
CREATE INDEX IF NOT EXISTS idx_tx_app ON transactions (initiating_application_id);
CREATE INDEX IF NOT EXISTS idx_tx_created ON transactions (created_at);
CREATE INDEX IF NOT EXISTS idx_tx_type ON transactions (transaction_type);
CREATE INDEX IF NOT EXISTS idx_tx_status ON transactions (status);

-- 7. Recharge Orders
CREATE TABLE IF NOT EXISTS recharge_orders (
    id BIGSERIAL PRIMARY KEY,
    order_id VARCHAR(64) NOT NULL UNIQUE,
    student_id VARCHAR(64) NOT NULL,
    amount_rupees NUMERIC(12, 2) NOT NULL,
    token_amount BIGINT NOT NULL,
    gateway_order_id VARCHAR(128) NULL,
    payment_gateway VARCHAR(32) NOT NULL DEFAULT 'RAZORPAY',
    status VARCHAR(32) NOT NULL DEFAULT 'CREATED',
    payment_id VARCHAR(128) NULL,
    signature VARCHAR(255) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_recharge_student ON recharge_orders (student_id);
CREATE INDEX IF NOT EXISTS idx_recharge_gateway_order ON recharge_orders (gateway_order_id);
CREATE INDEX IF NOT EXISTS idx_recharge_status ON recharge_orders (status);

-- 8. Payment Records
CREATE TABLE IF NOT EXISTS payment_records (
    id BIGSERIAL PRIMARY KEY,
    payment_id VARCHAR(128) NOT NULL UNIQUE,
    order_id VARCHAR(64) NOT NULL,
    amount_minor BIGINT NOT NULL,
    currency VARCHAR(16) NOT NULL DEFAULT 'INR',
    gateway VARCHAR(32) NOT NULL DEFAULT 'RAZORPAY',
    gateway_status VARCHAR(32) NOT NULL,
    payment_method VARCHAR(32) NULL,
    error_code VARCHAR(64) NULL,
    error_description TEXT NULL,
    raw_response TEXT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_payments_order ON payment_records (order_id);

-- 9. Applications
CREATE TABLE IF NOT EXISTS applications (
    id BIGSERIAL PRIMARY KEY,
    application_id VARCHAR(64) NOT NULL UNIQUE,
    application_name VARCHAR(128) NOT NULL,
    description VARCHAR(255) NULL,
    api_key VARCHAR(128) NOT NULL UNIQUE,
    hashed_api_secret VARCHAR(255) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    rate_limit_per_minute INT NOT NULL DEFAULT 300,
    webhook_url VARCHAR(255) NULL,
    created_by VARCHAR(64) NULL,
    last_request_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_app_api_key ON applications (api_key);
CREATE INDEX IF NOT EXISTS idx_app_status ON applications (status);

-- 10. Application Permissions
CREATE TABLE IF NOT EXISTS application_permissions (
    id BIGSERIAL PRIMARY KEY,
    application_id VARCHAR(64) NOT NULL,
    permission_scope VARCHAR(64) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (application_id, permission_scope),
    CONSTRAINT fk_app_perm_app FOREIGN KEY (application_id) REFERENCES applications (application_id) ON DELETE CASCADE
);

-- 11. Merchants
CREATE TABLE IF NOT EXISTS merchants (
    id BIGSERIAL PRIMARY KEY,
    merchant_id VARCHAR(64) NOT NULL UNIQUE,
    name VARCHAR(128) NOT NULL,
    description VARCHAR(255) NULL,
    category VARCHAR(32) NOT NULL,
    associated_application_id VARCHAR(64) NULL,
    contact_email VARCHAR(128) NULL,
    contact_phone VARCHAR(32) NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    settlement_account_number VARCHAR(64) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_merchant_cat ON merchants (category);
CREATE INDEX IF NOT EXISTS idx_merchant_status ON merchants (status);

-- 12. Merchant Accounts
CREATE TABLE IF NOT EXISTS merchant_accounts (
    id BIGSERIAL PRIMARY KEY,
    merchant_id VARCHAR(64) NOT NULL UNIQUE,
    total_earned_minor_units BIGINT NOT NULL DEFAULT 0,
    pending_settlement_minor_units BIGINT NOT NULL DEFAULT 0,
    settled_minor_units BIGINT NOT NULL DEFAULT 0,
    last_settlement_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_merch_acc_merch FOREIGN KEY (merchant_id) REFERENCES merchants (merchant_id) ON DELETE RESTRICT
);

-- 13. Refunds
CREATE TABLE IF NOT EXISTS refunds (
    id BIGSERIAL PRIMARY KEY,
    refund_id VARCHAR(64) NOT NULL UNIQUE,
    original_transaction_id VARCHAR(64) NOT NULL,
    student_id VARCHAR(64) NOT NULL,
    merchant_id VARCHAR(64) NULL,
    amount_minor_units BIGINT NOT NULL,
    reason VARCHAR(255) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'REQUESTED',
    approved_by VARCHAR(64) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_refund_orig_tx ON refunds (original_transaction_id);
CREATE INDEX IF NOT EXISTS idx_refund_student ON refunds (student_id);
CREATE INDEX IF NOT EXISTS idx_refund_status ON refunds (status);

-- 14. Settlements
CREATE TABLE IF NOT EXISTS settlements (
    id BIGSERIAL PRIMARY KEY,
    settlement_id VARCHAR(64) NOT NULL UNIQUE,
    merchant_id VARCHAR(64) NOT NULL,
    amount_minor_units BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    bank_reference VARCHAR(128) NULL,
    settlement_date TIMESTAMP NULL,
    notes VARCHAR(255) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_settle_merchant ON settlements (merchant_id);
CREATE INDEX IF NOT EXISTS idx_settle_status ON settlements (status);

-- 15. Webhook Events
CREATE TABLE IF NOT EXISTS webhook_events (
    id BIGSERIAL PRIMARY KEY,
    event_id VARCHAR(128) NOT NULL UNIQUE,
    provider VARCHAR(32) NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    payload_hash VARCHAR(128) NOT NULL,
    status VARCHAR(32) NOT NULL,
    processed_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_webhook_provider ON webhook_events (provider);
CREATE INDEX IF NOT EXISTS idx_webhook_status ON webhook_events (status);

-- 16. In-App Notifications
CREATE TABLE IF NOT EXISTS notifications (
    id BIGSERIAL PRIMARY KEY,
    recipient_id VARCHAR(64) NOT NULL,
    recipient_type VARCHAR(32) NOT NULL,
    title VARCHAR(128) NOT NULL,
    message TEXT NOT NULL,
    type VARCHAR(32) NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_notif_recipient ON notifications (recipient_id);
CREATE INDEX IF NOT EXISTS idx_notif_created ON notifications (created_at);

-- 17. Audit Logs
CREATE TABLE IF NOT EXISTS audit_logs (
    id BIGSERIAL PRIMARY KEY,
    actor_id VARCHAR(64) NOT NULL,
    actor_role VARCHAR(32) NOT NULL,
    action VARCHAR(64) NOT NULL,
    resource_type VARCHAR(64) NOT NULL,
    resource_id VARCHAR(64) NULL,
    ip_address VARCHAR(64) NULL,
    details TEXT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_audit_actor ON audit_logs (actor_id);
CREATE INDEX IF NOT EXISTS idx_audit_resource ON audit_logs (resource_type, resource_id);
CREATE INDEX IF NOT EXISTS idx_audit_created ON audit_logs (created_at);

-- 18. Idempotency Records
CREATE TABLE IF NOT EXISTS idempotency_records (
    id BIGSERIAL PRIMARY KEY,
    idempotency_key VARCHAR(128) NOT NULL,
    scope VARCHAR(64) NOT NULL,
    request_hash VARCHAR(64) NOT NULL,
    http_status INT NOT NULL,
    response_body TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (idempotency_key, scope)
);
CREATE INDEX IF NOT EXISTS idx_idempotency_created ON idempotency_records (created_at);

-- 19. System Configurations
CREATE TABLE IF NOT EXISTS system_configs (
    id BIGSERIAL PRIMARY KEY,
    config_key VARCHAR(64) NOT NULL UNIQUE,
    config_value VARCHAR(255) NOT NULL,
    description VARCHAR(255) NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
