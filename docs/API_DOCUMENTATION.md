# RITZ Wallet - REST API Reference & Specification
**Rajalakshmi Institute of Technology (RIT)**  
**Base URL:** `http://localhost:8085` or `https://ritz.ritchennai.edu.in`  
**API Version:** `v1`  
**OpenAPI Specification:** `http://localhost:8085/v3/api-docs`  
**Swagger UI:** `http://localhost:8085/swagger-ui.html`

---

## 1. Authentication & Security Conventions

### Authentication Headers

#### Human User Session (Student / Administrators)
Pass JWT bearer token in the `Authorization` header:
```http
Authorization: Bearer <jwt_access_token>
```

#### Campus Application Integration (Tillo / ERP / Stationery / Transport)
Pass registered Application API credentials:
```http
X-API-KEY: tillo-canteen-app-001
X-API-SECRET: tillo_live_secret_key_rit2026
```
*Alternatively, pass a pre-issued Application Token: `Authorization: Bearer <application_access_token>`*

#### Idempotency Key
All mutating transaction endpoints (`/api/v1/transactions/debit`, `/api/v1/transactions/refund`, etc.) require an idempotency key:
```http
Idempotency-Key: a3f88c20-7b24-4340-84c1-6385bb4a1234
```

### Standard Error Response Format
```json
{
  "timestamp": "2026-11-17T06:00:00Z",
  "status": 400,
  "error": "BAD_REQUEST",
  "message": "Student RIT2024001 has insufficient funds. Available: 40 tokens, Requested: 80 tokens",
  "path": "/api/v1/transactions/debit"
}
```

---

## 2. Authentication Endpoints

### 2.1 User Login
`POST /api/v1/auth/login`

**Request Body:**
```json
{
  "username": "RIT2026001",
  "password": "StudentPass2026!"
}
```

**Response (HTTP 200 OK):**
```json
{
  "token": "eyJhbGciOiJIUzUxMiIsIn...",
  "role": "STUDENT",
  "username": "RIT2026001",
  "fullName": "Aditya Sharma",
  "studentId": "RIT2026001",
  "expiresIn": 86400
}
```

### 2.2 Student Registration
`POST /api/v1/auth/register`

**Request Body:**
```json
{
  "username": "RIT2024099",
  "password": "SecurePassword123!",
  "fullName": "Kavya Sundaram",
  "email": "kavya.s@ritchennai.edu.in",
  "department": "Computer Science & Engineering",
  "yearOfStudy": 2,
  "phoneNumber": "+91 98765 43210"
}
```

**Response (HTTP 201 Created):**
```json
{
  "message": "Student account and wallet successfully created.",
  "studentId": "RIT2024099",
  "walletId": "WLT-RIT-A8C3411B",
  "status": "ACTIVE"
}
```

### 2.3 Current Session Profile
`GET /api/v1/auth/me`  
*Requires Authorization Header*

---

## 3. Student Wallet Endpoints

### 3.1 Get Current Student Wallet
`GET /api/v1/wallet`  
*Authorized Role: `STUDENT`*

**Response (HTTP 200 OK):**
```json
{
  "wallet_id": "WLT-RIT-D1B2443D",
  "student_id": "RIT2024001",
  "balance": 520,
  "currency": "TOKENS",
  "fiat_equivalent_inr": 520.00,
  "status": "ACTIVE",
  "created_at": "2026-11-16T15:34:00Z",
  "last_transaction_at": "2026-11-17T05:55:00Z"
}
```

### 3.2 Get Wallet Transaction History
`GET /api/v1/wallet/transactions?page=0&size=20`  
*Authorized Role: `STUDENT`*

**Response (HTTP 200 OK):**
```json
{
  "content": [
    {
      "transaction_id": "RZTXN-A3B912C0",
      "reference_id": "TILLO-ORD-90214",
      "type": "DEBIT",
      "amount": 80,
      "previous_balance": 600,
      "current_balance": 520,
      "status": "COMPLETED",
      "description": "Lunch Meal - Canteen Counter 2",
      "initiating_application": "TILLO",
      "timestamp": "2026-11-17T05:55:00Z"
    }
  ],
  "totalElements": 1,
  "totalPages": 1
}
```

### 3.3 Initiate Wallet Recharge
`POST /api/v1/wallet/recharge`  
*Authorized Role: `STUDENT`*

**Request Body:**
```json
{
  "amount_inr": 500.00,
  "notes": "Weekly canteen top-up"
}
```

**Response (HTTP 200 OK):**
```json
{
  "recharge_id": "RCHG-7C129A84",
  "amount_inr": 500.00,
  "tokens_to_credit": 500,
  "gateway_provider": "SANDBOX",
  "gateway_order_id": "order_sandbox_7C129A84",
  "status": "CREATED",
  "payment_checkout_payload": {
    "key": "rzp_test_placeholder_key",
    "amount": 50000,
    "currency": "INR",
    "name": "RITZ Wallet Top-up",
    "description": "Recharge 500 RITZ Tokens",
    "order_id": "order_sandbox_7C129A84"
  }
}
```

### 3.4 Get Recharge Status
`GET /api/v1/wallet/recharge/{rechargeId}`

---

## 4. Application Integration API

*These endpoints are used by campus partners like Tillo, Campus ERP, Stationery, and Transport.*

### 4.1 Process Wallet Debit (Purchase)
`POST /api/v1/transactions/debit`

**Required Headers:**
```http
X-API-KEY: tillo-canteen-app-001
X-API-SECRET: tillo_live_secret_key_rit2026
Idempotency-Key: 9b2d3e14-2391-4e44-8d99-52e1f2b604e1
Content-Type: application/json
```

**Request Body:**
```json
{
  "student_id": "RIT2024001",
  "amount": 80,
  "reference_id": "TILLO-ORD-10023",
  "description": "Lunch Meal - South Indian Thali",
  "merchant_code": "TILLO",
  "metadata": {
    "counter_id": "COUNTER_2",
    "item_count": 1,
    "terminal_operator": "staff_04"
  }
}
```

**Response (HTTP 200 OK):**
```json
{
  "transaction_id": "RZTXN-DEB-71F290A1",
  "reference_id": "TILLO-ORD-10023",
  "student_id": "RIT2024001",
  "amount": 80,
  "previous_balance": 600,
  "current_balance": 520,
  "status": "COMPLETED",
  "type": "DEBIT",
  "timestamp": "2026-11-17T06:05:00Z",
  "idempotency_key": "9b2d3e14-2391-4e44-8d99-52e1f2b604e1",
  "receipt": {
    "merchant_name": "Tillo Canteen & Cafeteria",
    "student_name": "Aravind Swaminathan",
    "debit_amount": 80,
    "remaining_balance": 520
  }
}
```

### 4.2 Validate Balance & Account Pre-check
`POST /api/v1/transactions/validate`

Allows external POS to verify student wallet status and balance sufficiency before rendering an order invoice.

**Request Body:**
```json
{
  "student_id": "RIT2024001",
  "required_amount": 80
}
```

**Response (HTTP 200 OK):**
```json
{
  "valid": true,
  "student_id": "RIT2024001",
  "wallet_status": "ACTIVE",
  "current_balance": 520,
  "sufficient_funds": true
}
```

### 4.3 Process Merchant Refund
`POST /api/v1/transactions/refund`

**Request Body:**
```json
{
  "original_transaction_id": "RZTXN-DEB-71F290A1",
  "amount": 80,
  "reason": "Order cancelled due to item out of stock",
  "merchant_code": "TILLO"
}
```

**Response (HTTP 200 OK):**
```json
{
  "refund_id": "RZTXN-REF-44E201B9",
  "original_transaction_id": "RZTXN-DEB-71F290A1",
  "student_id": "RIT2024001",
  "refunded_amount": 80,
  "new_student_balance": 600,
  "status": "COMPLETED",
  "timestamp": "2026-11-17T06:10:00Z"
}
```

### 4.4 Get Student Balance (Read-Only)
`GET /api/v1/students/{studentId}/balance`

**Headers:**
```http
X-API-KEY: tillo-canteen-app-001
X-API-SECRET: tillo_live_secret_key_rit2026
```

**Response (HTTP 200 OK):**
```json
{
  "student_id": "RIT2024001",
  "balance": 520,
  "status": "ACTIVE",
  "last_updated": "2026-11-17T06:05:00Z"
}
```

---

## 5. Double-Entry Ledger Endpoints

*Authorized Roles: `SUPER_ADMIN`, `FINANCE_ADMIN`*

### 5.1 List Chart of Accounts
`GET /api/v1/ledger/accounts`

### 5.2 List Immutable Journal Entries
`GET /api/v1/ledger/entries?page=0&size=50`

### 5.3 Live Ledger Balancing Verification
`GET /api/v1/ledger/verify`

**Response (HTTP 200 OK):**
```json
{
  "is_balanced": true,
  "total_debits": 250000,
  "total_credits": 250000,
  "discrepancy": 0,
  "total_entry_records": 482,
  "verified_at": "2026-11-17T06:12:00Z"
}
```

---

## 6. Financial Reconciliation & CSV Reporting

*Authorized Roles: `SUPER_ADMIN`, `FINANCE_ADMIN`*

### 6.1 Executive Financial Summary
`GET /api/v1/reports/summary`

### 6.2 3-Way Reconciliation Check
`GET /api/v1/reports/reconciliation/check`

### 6.3 Export CSV Reports
`GET /api/v1/reports/{reportType}/csv`  
Supported types:
- `transactions`
- `recharges`
- `ledger`
- `refunds`
- `settlements`

---

## 7. Webhook Endpoints

### 7.1 Razorpay Payment Webhook
`POST /api/v1/payments/razorpay/webhook`

**Headers:**
```http
X-Razorpay-Signature: <hmac_sha256_hex_digest>
Content-Type: application/json
```

**Payload:** Raw JSON payload emitted by Razorpay when `payment.captured` or `order.paid` occurs. RITZ Wallet recalculates the HMAC signature over the raw request body bytes and credits the student wallet exactly once.

### 7.2 Sandbox Payment Confirmation (Local Dev Mode)
`POST /api/v1/payments/sandbox/confirm`

**Request Body:**
```json
{
  "recharge_id": "RCHG-7C129A84",
  "gateway_payment_id": "pay_sandbox_test_123"
}
```
