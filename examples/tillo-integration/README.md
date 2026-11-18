# 🍔 Tillo Campus POS Integration Guide — RITZ Wallet

This directory contains production-ready SDK examples in **JavaScript (Node.js)**, **Python**, and **Java** demonstrating how external campus applications (such as Tillo POS, stationery stores, transport ticketing, or campus ERP) integrate with **RITZ Wallet**.

---

## 🏛️ Core Principles

1. **Direct Balance Modification is Forbidden**: Tillo POS cannot connect directly to the MySQL/PostgreSQL database to change student balances. All financial state transitions must be authorized and processed by RITZ Wallet.
2. **Double-Entry Accounting**: Every debit of tokens at Tillo creates paired debit/credit records in the immutable ledger:
   - Debit: `LIAB-WLT-{student_id}` (student wallet liability decreases)
   - Credit: `LIAB-MERCH-TILLO` (Tillo merchant payable liability increases)
3. **Idempotency is Mandatory**: Always send an `Idempotency-Key` header with your transaction reference. If network drops occur and you retry, RITZ Wallet returns the original transaction record without deducting tokens a second time.

---

## 🔑 Authentication

Include your registered application API key in the `X-API-KEY` header (or as `Bearer <api_key>` in `Authorization`):

```http
POST /api/v1/transactions/debit HTTP/1.1
Host: ritz-wallet.ritchennai.edu.in
Content-Type: application/json
X-API-KEY: tillo_app_live_key_2026
Idempotency-Key: TILLO-ORDER-1024
```

---

## 📦 API Specification

### 1. Deduct Tokens (Canteen Purchase)
* **Endpoint**: `POST /api/v1/transactions/debit`
* **Request Payload**:
```json
{
  "student_id": "RIT2026001",
  "amount": 80,
  "currency": "RITZ",
  "reference_id": "TILLO-ORDER-1024",
  "merchant_id": "TILLO",
  "description": "Lunch canteen meal"
}
```

* **Successful Response (HTTP 200)**:
```json
{
  "success": true,
  "transaction_id": "RZTXN-100024",
  "reference_id": "TILLO-ORDER-1024",
  "amount": 80,
  "previous_balance": 600,
  "current_balance": 520,
  "status": "COMPLETED"
}
```

* **Insufficient Funds Response (HTTP 400)**:
```json
{
  "success": false,
  "error": "Insufficient wallet balance. Available: 40 RITZ tokens"
}
```

---

### 2. Check Student Balance
* **Endpoint**: `GET /api/v1/students/{studentId}/balance`
* **Headers**: `X-API-KEY: <your-key>`
* **Response**:
```json
{
  "student_id": "RIT2026001",
  "student_name": "Aditya Sharma",
  "balance": 520,
  "currency": "RITZ",
  "can_transact": true,
  "status": "ACTIVE"
}
```

---

### 3. Validate Balance Pre-Check (Without Debiting)
* **Endpoint**: `POST /api/v1/transactions/validate`
* **Request**:
```json
{
  "student_id": "RIT2026001",
  "amount": 80
}
```
* **Response**:
```json
{
  "valid": true,
  "student_id": "RIT2026001",
  "available_balance": 520,
  "required_amount": 80,
  "wallet_status": "ACTIVE"
}
```

---

## 🚀 Running the Examples

### Node.js / JavaScript
```bash
cd javascript
npm install
node demoSpend.js
```

### Python
```bash
cd python
pip install requests
python demo_spend.py
```

### Java
```bash
cd java
javac TilloClient.java
java TilloClient
```
