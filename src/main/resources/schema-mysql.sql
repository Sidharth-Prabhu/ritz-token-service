-- ============================================================================
-- RITZ Wallet - MySQL 8+ Database Schema
-- Rajalakshmi Institute of Technology (RIT) Campus Payment Infrastructure
-- ============================================================================

CREATE DATABASE IF NOT EXISTS `ritz_wallet` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `ritz_wallet`;

-- 1. Users table (Authentication & Roles)
CREATE TABLE IF NOT EXISTS `users` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `username` VARCHAR(64) NOT NULL UNIQUE,
    `password_hash` VARCHAR(255) NOT NULL,
    `full_name` VARCHAR(128) NOT NULL,
    `email` VARCHAR(128) NOT NULL UNIQUE,
    `role` VARCHAR(32) NOT NULL,
    `student_id` VARCHAR(64) NULL,
    `merchant_id` VARCHAR(64) NULL,
    `application_id` VARCHAR(64) NULL,
    `is_active` BOOLEAN NOT NULL DEFAULT TRUE,
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_users_role` (`role`),
    INDEX `idx_users_student_id` (`student_id`),
    INDEX `idx_users_merchant_id` (`merchant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. Students table
CREATE TABLE IF NOT EXISTS `students` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `student_id` VARCHAR(64) NOT NULL UNIQUE,
    `name` VARCHAR(128) NOT NULL,
    `email` VARCHAR(128) NOT NULL UNIQUE,
    `mobile_number` VARCHAR(20) NOT NULL,
    `department` VARCHAR(64) NOT NULL,
    `batch_year` INT NOT NULL,
    `status` VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_students_department` (`department`),
    INDEX `idx_students_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. Wallets table (Student balances in integer minor units)
CREATE TABLE IF NOT EXISTS `wallets` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `wallet_id` VARCHAR(64) NOT NULL UNIQUE,
    `student_id` VARCHAR(64) NOT NULL UNIQUE,
    `balance_minor_units` BIGINT NOT NULL DEFAULT 0,
    `currency` VARCHAR(16) NOT NULL DEFAULT 'RITZ',
    `status` VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    `last_transaction_at` TIMESTAMP NULL,
    `version` BIGINT NOT NULL DEFAULT 0,
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_wallets_status` (`status`),
    INDEX `idx_wallets_student_id` (`student_id`),
    CONSTRAINT `fk_wallets_student` FOREIGN KEY (`student_id`) REFERENCES `students` (`student_id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. Double-Entry Ledger Accounts
CREATE TABLE IF NOT EXISTS `ledger_accounts` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `account_number` VARCHAR(64) NOT NULL UNIQUE,
    `account_name` VARCHAR(128) NOT NULL,
    `account_type` VARCHAR(32) NOT NULL, -- ASSET, LIABILITY, EQUITY, REVENUE, EXPENSE
    `balance_minor_units` BIGINT NOT NULL DEFAULT 0,
    `currency` VARCHAR(16) NOT NULL DEFAULT 'RITZ',
    `status` VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_ledger_accounts_type` (`account_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5. Double-Entry Ledger Entries (Strictly Immutable)
CREATE TABLE IF NOT EXISTS `ledger_entries` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `entry_id` VARCHAR(64) NOT NULL UNIQUE,
    `transaction_id` VARCHAR(64) NOT NULL,
    `account_number` VARCHAR(64) NOT NULL,
    `entry_type` VARCHAR(16) NOT NULL, -- DEBIT, CREDIT
    `amount_minor_units` BIGINT NOT NULL,
    `balance_after_minor_units` BIGINT NOT NULL,
    `description` VARCHAR(255) NULL,
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_ledger_entries_txn` (`transaction_id`),
    INDEX `idx_ledger_entries_account` (`account_number`),
    INDEX `idx_ledger_entries_created` (`created_at`),
    CONSTRAINT `fk_ledger_entries_account` FOREIGN KEY (`account_number`) REFERENCES `ledger_accounts` (`account_number`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 6. Transactions table
CREATE TABLE IF NOT EXISTS `transactions` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `transaction_id` VARCHAR(64) NOT NULL UNIQUE,
    `reference_id` VARCHAR(128) NULL,
    `idempotency_key` VARCHAR(128) NULL,
    `source_account` VARCHAR(64) NOT NULL,
    `destination_account` VARCHAR(64) NOT NULL,
    `amount_minor_units` BIGINT NOT NULL,
    `currency` VARCHAR(16) NOT NULL DEFAULT 'RITZ',
    `transaction_type` VARCHAR(32) NOT NULL, -- RECHARGE, DEBIT, CREDIT, REFUND, REVERSAL, SETTLEMENT, ADJUSTMENT
    `status` VARCHAR(32) NOT NULL, -- PENDING, PROCESSING, COMPLETED, FAILED, REVERSED
    `initiating_application_id` VARCHAR(64) NULL,
    `merchant_id` VARCHAR(64) NULL,
    `student_id` VARCHAR(64) NULL,
    `description` VARCHAR(255) NULL,
    `previous_balance_minor_units` BIGINT NULL,
    `current_balance_minor_units` BIGINT NULL,
    `failure_reason` VARCHAR(255) NULL,
    `metadata` JSON NULL,
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_tx_ref` (`reference_id`),
    INDEX `idx_tx_idempotency` (`idempotency_key`),
    INDEX `idx_tx_student` (`student_id`),
    INDEX `idx_tx_merchant` (`merchant_id`),
    INDEX `idx_tx_app` (`initiating_application_id`),
    INDEX `idx_tx_created` (`created_at`),
    INDEX `idx_tx_type` (`transaction_type`),
    INDEX `idx_tx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 7. Recharge Orders
CREATE TABLE IF NOT EXISTS `recharge_orders` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `order_id` VARCHAR(64) NOT NULL UNIQUE,
    `student_id` VARCHAR(64) NOT NULL,
    `amount_rupees` DECIMAL(12, 2) NOT NULL,
    `token_amount` BIGINT NOT NULL,
    `gateway_order_id` VARCHAR(128) NULL,
    `payment_gateway` VARCHAR(32) NOT NULL DEFAULT 'RAZORPAY',
    `status` VARCHAR(32) NOT NULL DEFAULT 'CREATED', -- CREATED, ATTEMPTED, PAID, FAILED, EXPIRED
    `payment_id` VARCHAR(128) NULL,
    `signature` VARCHAR(255) NULL,
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_recharge_student` (`student_id`),
    INDEX `idx_recharge_gateway_order` (`gateway_order_id`),
    INDEX `idx_recharge_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 8. Payment Records
CREATE TABLE IF NOT EXISTS `payment_records` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `payment_id` VARCHAR(128) NOT NULL UNIQUE,
    `order_id` VARCHAR(64) NOT NULL,
    `amount_minor` BIGINT NOT NULL,
    `currency` VARCHAR(16) NOT NULL DEFAULT 'INR',
    `gateway` VARCHAR(32) NOT NULL DEFAULT 'RAZORPAY',
    `gateway_status` VARCHAR(32) NOT NULL,
    `payment_method` VARCHAR(32) NULL,
    `error_code` VARCHAR(64) NULL,
    `error_description` TEXT NULL,
    `raw_response` JSON NULL,
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_payments_order` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 9. Applications (External integrations)
CREATE TABLE IF NOT EXISTS `applications` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `application_id` VARCHAR(64) NOT NULL UNIQUE,
    `application_name` VARCHAR(128) NOT NULL,
    `description` VARCHAR(255) NULL,
    `api_key` VARCHAR(128) NOT NULL UNIQUE,
    `hashed_api_secret` VARCHAR(255) NOT NULL,
    `status` VARCHAR(32) NOT NULL DEFAULT 'ACTIVE', -- ACTIVE, SUSPENDED, REVOKED
    `rate_limit_per_minute` INT NOT NULL DEFAULT 300,
    `webhook_url` VARCHAR(255) NULL,
    `created_by` VARCHAR(64) NULL,
    `last_request_at` TIMESTAMP NULL,
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_app_api_key` (`api_key`),
    INDEX `idx_app_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 10. Application Permissions
CREATE TABLE IF NOT EXISTS `application_permissions` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `application_id` VARCHAR(64) NOT NULL,
    `permission_scope` VARCHAR(64) NOT NULL,
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_app_perm` (`application_id`, `permission_scope`),
    CONSTRAINT `fk_app_perm_app` FOREIGN KEY (`application_id`) REFERENCES `applications` (`application_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 11. Merchants
CREATE TABLE IF NOT EXISTS `merchants` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `merchant_id` VARCHAR(64) NOT NULL UNIQUE,
    `name` VARCHAR(128) NOT NULL,
    `description` VARCHAR(255) NULL,
    `category` VARCHAR(32) NOT NULL, -- FOOD, STATIONERY, TRANSPORT, CAMPUS_SERVICES, OTHER
    `associated_application_id` VARCHAR(64) NULL,
    `contact_email` VARCHAR(128) NULL,
    `contact_phone` VARCHAR(32) NULL,
    `status` VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    `settlement_account_number` VARCHAR(64) NULL,
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_merchant_cat` (`category`),
    INDEX `idx_merchant_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 12. Merchant Accounts
CREATE TABLE IF NOT EXISTS `merchant_accounts` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `merchant_id` VARCHAR(64) NOT NULL UNIQUE,
    `total_earned_minor_units` BIGINT NOT NULL DEFAULT 0,
    `pending_settlement_minor_units` BIGINT NOT NULL DEFAULT 0,
    `settled_minor_units` BIGINT NOT NULL DEFAULT 0,
    `last_settlement_at` TIMESTAMP NULL,
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `fk_merch_acc_merch` FOREIGN KEY (`merchant_id`) REFERENCES `merchants` (`merchant_id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 13. Refunds
CREATE TABLE IF NOT EXISTS `refunds` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `refund_id` VARCHAR(64) NOT NULL UNIQUE,
    `original_transaction_id` VARCHAR(64) NOT NULL,
    `student_id` VARCHAR(64) NOT NULL,
    `merchant_id` VARCHAR(64) NULL,
    `amount_minor_units` BIGINT NOT NULL,
    `reason` VARCHAR(255) NOT NULL,
    `status` VARCHAR(32) NOT NULL DEFAULT 'REQUESTED', -- REQUESTED, APPROVED, REJECTED, COMPLETED
    `approved_by` VARCHAR(64) NULL,
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_refund_orig_tx` (`original_transaction_id`),
    INDEX `idx_refund_student` (`student_id`),
    INDEX `idx_refund_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 14. Settlements
CREATE TABLE IF NOT EXISTS `settlements` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `settlement_id` VARCHAR(64) NOT NULL UNIQUE,
    `merchant_id` VARCHAR(64) NOT NULL,
    `amount_minor_units` BIGINT NOT NULL,
    `status` VARCHAR(32) NOT NULL DEFAULT 'PENDING', -- PENDING, PROCESSING, COMPLETED, FAILED
    `bank_reference` VARCHAR(128) NULL,
    `settlement_date` TIMESTAMP NULL,
    `notes` VARCHAR(255) NULL,
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_settle_merchant` (`merchant_id`),
    INDEX `idx_settle_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 15. Webhook Events (Deduplication & Auditing)
CREATE TABLE IF NOT EXISTS `webhook_events` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `event_id` VARCHAR(128) NOT NULL UNIQUE,
    `provider` VARCHAR(32) NOT NULL,
    `event_type` VARCHAR(64) NOT NULL,
    `payload_hash` VARCHAR(128) NOT NULL,
    `status` VARCHAR(32) NOT NULL, -- RECEIVED, PROCESSED, FAILED, DUPLICATE
    `processed_at` TIMESTAMP NULL,
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_webhook_provider` (`provider`),
    INDEX `idx_webhook_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 16. In-App Notifications
CREATE TABLE IF NOT EXISTS `notifications` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `recipient_id` VARCHAR(64) NOT NULL,
    `recipient_type` VARCHAR(32) NOT NULL, -- STUDENT, ADMIN, MERCHANT
    `title` VARCHAR(128) NOT NULL,
    `message` TEXT NOT NULL,
    `type` VARCHAR(32) NOT NULL, -- RECHARGE_SUCCESS, RECHARGE_FAILED, PURCHASE, REFUND, LOW_BALANCE, WALLET_SUSPENDED, SYSTEM
    `is_read` BOOLEAN NOT NULL DEFAULT FALSE,
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_notif_recipient` (`recipient_id`),
    INDEX `idx_notif_created` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 17. Audit Logs
CREATE TABLE IF NOT EXISTS `audit_logs` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `actor_id` VARCHAR(64) NOT NULL,
    `actor_role` VARCHAR(32) NOT NULL,
    `action` VARCHAR(64) NOT NULL,
    `resource_type` VARCHAR(64) NOT NULL,
    `resource_id` VARCHAR(64) NULL,
    `ip_address` VARCHAR(64) NULL,
    `details` TEXT NULL,
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_audit_actor` (`actor_id`),
    INDEX `idx_audit_resource` (`resource_type`, `resource_id`),
    INDEX `idx_audit_created` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 18. Idempotency Records
CREATE TABLE IF NOT EXISTS `idempotency_records` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `idempotency_key` VARCHAR(128) NOT NULL,
    `scope` VARCHAR(64) NOT NULL,
    `request_hash` VARCHAR(64) NOT NULL,
    `http_status` INT NOT NULL,
    `response_body` TEXT NOT NULL,
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_idempotency` (`idempotency_key`, `scope`),
    INDEX `idx_idempotency_created` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 19. System Configurations
CREATE TABLE IF NOT EXISTS `system_configs` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `config_key` VARCHAR(64) NOT NULL UNIQUE,
    `config_value` VARCHAR(255) NOT NULL,
    `description` VARCHAR(255) NULL,
    `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
