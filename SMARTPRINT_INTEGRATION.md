# SMARTPRINT Integration Guide — RITZ Campus Wallet

This document contains credentials and integration instructions for **Smartprint** (Campus Automated Cloud & Kiosk Printing Services) to communicate with **RITZ Wallet**.

---

## 1. Production API Credentials

| Parameter | Value |
| :--- | :--- |
| **Application ID** | `SMARTPRINT` |
| **Merchant ID** | `SMARTPRINT` |
| **API Base URL** | `https://ritz.rit-services.in` |
| **Local Base URL** | `http://localhost:8085` |
| **X-API-KEY** | `rit_live_key_473c087cdb1c0f8190d30ac57042a008` |
| **X-API-SECRET** | `hWAjFr6ZygcEGCAUPDEU1YlC4VYT92q5OEeH-lIhWMI` |
| **Status** | `ACTIVE` |

> [!IMPORTANT]
> Keep `X-API-SECRET` secure in your backend environment variables (`SMARTPRINT_RITZ_API_KEY` and `SMARTPRINT_RITZ_API_SECRET`). Never expose it in client-side JavaScript or mobile apps.

---

## 2. Granted Permission Scopes

`SMARTPRINT` has been provisioned with the following permissions:
- `TRANSACTION_DEBIT` — Deduct tokens for printing/copying orders.
- `TRANSACTION_CREDIT` — Credit student wallets.
- `TRANSACTION_REFUND` — Process refunds for cancelled or failed print jobs.
- `TRANSACTION_VALIDATE` — Check transaction status and idempotency.
- `BALANCE_READ` — Check student token balance prior to releasing print jobs.
- `MERCHANT_READ` — Inquire Smartprint merchant ledger balance and settlement status.

---

## 3. Redirecting Users to RITZ Wallet for Top-Up

When a student has insufficient tokens or needs to top up their wallet from Smartprint, redirect them directly with their mobile number or student ID. The session is automatically authenticated and loaded:

```
https://ritz.rit-services.in/?phone=9876543210&name=Student+Name
```

---

## 4. REST API Integration Endpoints

All requests must include the authentication headers:
```http
X-API-KEY: rit_live_key_473c087cdb1c0f8190d30ac57042a008
X-API-SECRET: hWAjFr6ZygcEGCAUPDEU1YlC4VYT92q5OEeH-lIhWMI
Content-Type: application/json
```

---

### A. Check Student Wallet Balance

Check student balance before accepting or queueing print jobs. Supports lookup by 10-digit mobile number or student registration number.

#### Lookup by Phone Number:
```http
GET /api/v1/students/phone/{phoneNumber}/balance
```

**Example:**
```bash
curl -s -X GET https://ritz.rit-services.in/api/v1/students/phone/9876543210/balance \
  -H "X-API-KEY: rit_live_key_473c087cdb1c0f8190d30ac57042a008" \
  -H "X-API-SECRET: hWAjFr6ZygcEGCAUPDEU1YlC4VYT92q5OEeH-lIhWMI"
```

**Response (`200 OK`):**
```json
{
  "student_id": "RIT2026001",
  "student_name": "Aditya Sharma",
  "mobile_number": "9876543210",
  "balance": 225,
  "currency": "RITZ",
  "can_transact": true,
  "status": "ACTIVE"
}
```

#### Lookup by Student ID:
```http
GET /api/v1/students/{studentId}/balance
```

---

### B. Deduct Tokens for Print Job (Debit)

Deducts tokens from the student's wallet and credits `LIAB-MERCH-SMARTPRINT` in the immutable double-entry ledger.

```http
POST /api/v1/transactions/debit
Idempotency-Key: <UNIQUE_JOB_ID>
```

#### Request Body:
```json
{
  "phone_number": "9876543210",
  "amount": 10,
  "reference_id": "PRINT-JOB-2026-9941",
  "merchant_id": "SMARTPRINT",
  "description": "Smartprint B&W 5 pages document printing"
}
```

*Note: You can pass either `"phone_number"` or `"student_id"`.*

**cURL Example:**
```bash
curl -s -X POST https://ritz.rit-services.in/api/v1/transactions/debit \
  -H "X-API-KEY: rit_live_key_473c087cdb1c0f8190d30ac57042a008" \
  -H "X-API-SECRET: hWAjFr6ZygcEGCAUPDEU1YlC4VYT92q5OEeH-lIhWMI" \
  -H "Idempotency-Key: PRINT-JOB-2026-9941" \
  -H "Content-Type: application/json" \
  -d '{
    "phone_number": "9876543210",
    "amount": 10,
    "reference_id": "PRINT-JOB-2026-9941",
    "merchant_id": "SMARTPRINT",
    "description": "Smartprint B&W 5 pages document printing"
  }'
```

**Response (`200 OK`):**
```json
{
  "success": true,
  "transaction_id": "RZTXN-67C69693-E",
  "reference_id": "PRINT-JOB-2026-9941",
  "amount": 10,
  "status": "COMPLETED",
  "previous_balance": 240,
  "current_balance": 230
}
```

---

### C. Refund a Failed or Jammed Print Job

If a printer runs out of paper, jams, or the job fails:

```http
POST /api/v1/transactions/refund
Idempotency-Key: <UNIQUE_REFUND_ID>
```

#### Request Body:
```json
{
  "transaction_id": "RZTXN-67C69693-E",
  "amount": 10,
  "reason": "Printer hardware jam - page not printed",
  "reference_id": "REFUND-JOB-2026-9941"
}
```

---

## 5. Code Implementation Examples

### Python (requests)

```python
import os
import requests
import uuid

RITZ_URL = "https://ritz.rit-services.in"
API_KEY = os.getenv("SMARTPRINT_RITZ_API_KEY", "rit_live_key_473c087cdb1c0f8190d30ac57042a008")
API_SECRET = os.getenv("SMARTPRINT_RITZ_API_SECRET", "hWAjFr6ZygcEGCAUPDEU1YlC4VYT92q5OEeH-lIhWMI")

headers = {
    "X-API-KEY": API_KEY,
    "X-API-SECRET": API_SECRET,
    "Content-Type": "application/json"
}

def check_balance(phone_number: str):
    url = f"{RITZ_URL}/api/v1/students/phone/{phone_number}/balance"
    res = requests.get(url, headers=headers)
    res.raise_for_status()
    return res.json()

def charge_for_print(phone_number: str, pages: int, cost_per_page: int, job_id: str):
    total_tokens = pages * cost_per_page
    url = f"{RITZ_URL}/api/v1/transactions/debit"
    
    debit_headers = {**headers, "Idempotency-Key": f"SP-{job_id}"}
    payload = {
        "phone_number": phone_number,
        "amount": total_tokens,
        "reference_id": f"SP-{job_id}",
        "merchant_id": "SMARTPRINT",
        "description": f"Smartprint: {pages} pages printed"
    }
    
    res = requests.post(url, json=payload, headers=debit_headers)
    if res.status_code == 200:
        return res.json()
    else:
        raise Exception(f"Payment failed: {res.text}")

# Example Usage:
# bal = check_balance("9876543210")
# print("Current balance:", bal["balance"])
# txn = charge_for_print("9876543210", pages=4, cost_per_page=2, job_id="10042")
# print("Transaction ID:", txn["transaction_id"])
```

### Node.js / TypeScript (axios)

```typescript
import axios from 'axios';

const client = axios.create({
  baseURL: 'https://ritz.rit-services.in',
  headers: {
    'X-API-KEY': 'rit_live_key_473c087cdb1c0f8190d30ac57042a008',
    'X-API-SECRET': 'hWAjFr6ZygcEGCAUPDEU1YlC4VYT92q5OEeH-lIhWMI',
    'Content-Type': 'application/json'
  }
});

export async function debitStudentTokens(phone: string, amount: number, jobId: string) {
  const response = await client.post('/api/v1/transactions/debit', {
    phone_number: phone,
    amount,
    reference_id: `SP-${jobId}`,
    merchant_id: 'SMARTPRINT',
    description: `Smartprint job ${jobId}`
  }, {
    headers: {
      'Idempotency-Key': `SP-${jobId}`
    }
  });

  return response.data;
}
```
