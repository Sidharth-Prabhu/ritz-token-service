# 🔌 RITZ Wallet — External Project Integration Guide

This guide describes how to integrate your external application (e.g. Canteen POS, Bookstore, Web/Mobile App) with **RITZ Wallet** without requiring your users to register or log in with passwords.

---

## 🔑 1. API Credentials & Base URL

* **Subdomain URL (Public):** `https://ritz.rit-services.in`
* **Local / VPS Direct URL:** `http://localhost:8085`
* **Master API Key:**
  ```http
  X-API-KEY: ritz-secret-api-key-2026
  ```

> All backend endpoints accept this API key in the `X-API-KEY` header (or as `Authorization: Bearer ritz-secret-api-key-2026`).

---

## 🚀 2. Seamless Portal Access (No Separate Login)

When sending users to the RITZ Wallet portal from your application (for wallet viewing or recharging), append their phone number to the URL:

```text
https://ritz.rit-services.in/?phone=9876543210&name=Rahul+Verma
```

### Supported Query Parameters:
| Parameter | Type | Required | Description |
|---|---|:---:|---|
| `phone` / `mobile` | String (10 digits) | **Yes** | User's mobile number. Automatically finds or provisions their wallet. |
| `name` | String | No | User's full name (used on first registration). |
| `student_id` | String | No | Optional student/employee ID in your system. |

### How It Works:
1. User clicks *"Recharge Wallet"* inside your app.
2. Your app redirects them to `https://ritz.rit-services.in/?phone=USER_PHONE&name=USER_NAME`.
3. RITZ Wallet **automatically authenticates or creates their wallet** instantly on page load.
4. The user sees their balance, previous recharge history, and can immediately top up.
5. Once recharged, the new balance is instantly available to your application via REST API.

---

## 📡 3. REST API Reference for Your Application

All application endpoints require the `X-API-KEY` header.

### 3.1 Check User Balance by Phone Number
```http
GET /api/v1/students/phone/{phoneNumber}/balance
X-API-KEY: ritz-secret-api-key-2026
```

#### Example Response (`200 OK`):
```json
{
  "student_id": "STU9123456789",
  "student_name": "Rahul Verma",
  "mobile_number": "9123456789",
  "balance": 250,
  "currency": "RITZ",
  "can_transact": true,
  "status": "ACTIVE"
}
```

---

### 3.2 Validate Balance Before Order Placement
Verifies if the user has sufficient tokens without debiting.

```http
POST /api/v1/transactions/validate
X-API-KEY: ritz-secret-api-key-2026
Content-Type: application/json

{
  "phone_number": "9123456789",
  "amount": 75
}
```

#### Example Response (`200 OK`):
```json
{
  "valid": true,
  "student_id": "STU9123456789",
  "available_balance": 250,
  "required_amount": 75,
  "wallet_status": "ACTIVE"
}
```

---

### 3.3 Deduct / Spend Tokens (Purchase)
Deducts tokens from the student's wallet using either `phone_number` or `student_id`.

```http
POST /api/v1/transactions/debit
X-API-KEY: ritz-secret-api-key-2026
Idempotency-Key: ORD-CAFE-901-ATTEMPT-1
Content-Type: application/json

{
  "phone_number": "9123456789",
  "amount": 75,
  "reference_id": "ORD-CAFE-901",
  "merchant_id": "TILLO",
  "description": "Lunch Combo Meal"
}
```

#### Headers Explained:
* `X-API-KEY`: `ritz-secret-api-key-2026`
* `Idempotency-Key`: Unique string per order (e.g. order UUID). If network times out, retrying with the same key will **never double-deduct**; it returns the cached result.

#### Example Response (`200 OK`):
```json
{
  "success": true,
  "transaction_id": "RZTXN-5E7DE6BF-8",
  "reference_id": "ORD-CAFE-901",
  "amount": 75,
  "previous_balance": 250,
  "current_balance": 175,
  "status": "COMPLETED"
}
```

#### If Insufficient Balance (`400 Bad Request`):
```json
{
  "success": false,
  "error": "Insufficient wallet balance. Available: 50 RITZ tokens"
}
```

---

### 3.4 Refund a Transaction
Restores tokens to the user's wallet with an immutable compensating ledger entry.

```http
POST /api/v1/transactions/refund
X-API-KEY: ritz-secret-api-key-2026
Idempotency-Key: REF-ORD-CAFE-901
Content-Type: application/json

{
  "transaction_id": "RZTXN-5E7DE6BF-8",
  "amount": 75,
  "reason": "Order cancelled by kitchen",
  "reference_id": "REF-901"
}
```

#### Example Response (`200 OK`):
```json
{
  "success": true,
  "transaction_id": "RZTXN-REF-F9801F80",
  "reference_id": "REF-901",
  "amount": 75,
  "previous_balance": 175,
  "current_balance": 250,
  "status": "COMPLETED"
}
```

---

## 💻 4. Code Examples

### Node.js / TypeScript Example
```javascript
const axios = require('axios');

const RITZ_API = 'https://ritz.rit-services.in';
const API_KEY = 'ritz-secret-api-key-2026';

// 1. Check user balance
async function checkBalance(phoneNumber) {
  const res = await axios.get(`${RITZ_API}/api/v1/students/phone/${phoneNumber}/balance`, {
    headers: { 'X-API-KEY': API_KEY }
  });
  return res.data.balance;
}

// 2. Deduct tokens on purchase
async function deductTokens(phoneNumber, amount, orderId, description) {
  const res = await axios.post(`${RITZ_API}/api/v1/transactions/debit`, {
    phone_number: phoneNumber,
    amount: amount,
    reference_id: orderId,
    description: description
  }, {
    headers: {
      'X-API-KEY': API_KEY,
      'Idempotency-Key': `IDEM-${orderId}`
    }
  });
  return res.data;
}

// 3. Generate portal recharge link
function getRechargePortalUrl(phoneNumber, studentName) {
  return `${RITZ_API}/?phone=${encodeURIComponent(phoneNumber)}&name=${encodeURIComponent(studentName)}`;
}
```

### Python Example
```python
import requests

RITZ_API = "https://ritz.rit-services.in"
API_KEY = "ritz-secret-api-key-2026"
HEADERS = {"X-API-KEY": API_KEY, "Content-Type": "application/json"}

def get_balance(phone_number: str) -> int:
    url = f"{RITZ_API}/api/v1/students/phone/{phone_number}/balance"
    resp = requests.get(url, headers=HEADERS)
    resp.raise_for_status()
    return resp.json()["balance"]

def spend_tokens(phone_number: str, amount: int, order_id: str, description: str = "") -> dict:
    url = f"{RITZ_API}/api/v1/transactions/debit"
    headers = {**HEADERS, "Idempotency-Key": f"IDEM-{order_id}"}
    payload = {
        "phone_number": phone_number,
        "amount": amount,
        "reference_id": order_id,
        "description": description
    }
    resp = requests.post(url, json=payload, headers=headers)
    resp.raise_for_status()
    return resp.json()

def get_recharge_link(phone_number: str, name: str) -> str:
    return f"{RITZ_API}/?phone={phone_number}&name={requests.utils.quote(name)}"
```

---

## 🔒 5. Concurrency & Idempotency Guarantees
* **Row-Level Locking**: Simultaneous requests for the same student wallet are handled sequentially using pessimistic database locking (`SELECT FOR UPDATE`), preventing double-spend and overdrafts.
* **Idempotency**: Providing `Idempotency-Key` guarantees that duplicate requests (e.g. from network retries or user double-clicking) return the cached original result without debiting multiple times.
* **Double-Entry Ledger**: Every debit, recharge, and refund automatically writes balanced accounting entries to the immutable ledger.
