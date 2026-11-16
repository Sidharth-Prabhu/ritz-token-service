# 🏛️ Ritz Token Microservice — Technical Architecture & Deployment Documentation

Comprehensive technical documentation and operational runbook for the **Ritz Token Service**, a standalone high-performance digital token ledger and wallet microservice engineered for high-concurrency environments and hosted on a **Proxmox VE Virtual Machine / LXC Container**.

---

## 📑 Table of Contents

1. [Executive Summary](#1-executive-summary)
2. [Architecture Overview](#2-architecture-overview)
3. [Tokenomics & Core Engine Mechanics](#3-tokenomics--core-engine-mechanics)
4. [Data Model & Database Schema](#4-data-model--database-schema)
5. [Concurrency & Double-Spend Prevention](#5-concurrency--double-spend-prevention)
6. [API Specification & Protocols](#6-api-specification--protocols)
7. [Proxmox VE Deployment Guide](#7-proxmox-ve-deployment-guide)
8. [Tillo POS Client Integration](#8-tillo-pos-client-integration)
9. [Disaster Recovery, Health Monitoring & Auditing](#9-disaster-recovery-health-monitoring--auditing)

---

## 1. Executive Summary

The **Ritz Token Microservice** decouples the digital currency and wallet ecosystem from the central Point-of-Sale (POS) and ordering backend. By isolating the ledger onto a dedicated Proxmox VM, the architecture achieves:

* **Fault Isolation**: Token minting, burning, and wallet reconciliations run in a dedicated runtime and database instance. POS operations cannot starve or destabilize token accounting.
* **Peak Rush-Hour Resilience**: Concurrency controls, HikariCP connection tuning, and batch-inserted serialized units handle thousands of simultaneous canteen food transactions without ledger desynchronization.
* **Complete Verifiability**: Every single Ritz token minted is assigned a cryptographic SHA-256 hash, enabling continuous public and administrative auditability.

---

## 2. Architecture Overview

```
                                  +-----------------------------+
                                  |    Tillo POS Application    |
                                  | (Admin / Ordering / Counter)|
                                  +--------------+--------------+
                                                 |
                                                 | HTTPS / REST (X-API-KEY)
                                                 v
                       ================ Proxmox VE Host ================
                      ||                                               ||
                      ||    +-------------------------------------+    ||
                      ||    |     Ritz Token Microservice         |    ||
                      ||    |  (Spring Boot 3.4 / OpenJDK 17)     |    ||
                      ||    +------------------+------------------+    ||
                      ||                       |                        ||
                      ||                       | HikariCP (JDBC)        ||
                      ||                       v                        ||
                      ||    +-------------------------------------+    ||
                      ||    |        PostgreSQL 16 Database       |    ||
                      ||    |   (token_wallets, token_units,      ||   ||
                      ||    |    token_transactions)              ||   ||
                      ||    +-------------------------------------+    ||
                      ||                                               ||
                       =================================================
```

### Components
1. **Host Environment**: Proxmox VE Virtual Machine (Ubuntu 22.04/24.04 LTS or Debian 12) or privileged/unprivileged LXC container.
2. **Application Layer**: Java 17, Spring Boot 3.4.1, Spring Data JPA, Spring Security, Springdoc OpenAPI.
3. **Storage Layer**: PostgreSQL 16 with optimized indexing on owner IDs, unit statuses, and transaction timestamps.
4. **Security Filter**: Header-based `X-API-KEY` or Bearer authentication guarding all ledger-altering mutations.

---

## 3. Tokenomics & Core Engine Mechanics

The Ritz token system operates as a **1:1 closed-loop digital campus currency**:

```
                       +-----------------------+
                       |  Top-Up Request (INR) |
                       +-----------+-----------+
                                   |
                                   v  MINTING
               +---------------------------------------+
               |  Generate SHA-256 Token Hash(es)      |
               |  Batch INSERT into `token_units`      |
               |  Credit User Balance in `token_wallet`|
               |  Record in `token_transactions`       |
               +-------------------+-------------------+
                                   |
                                   | Tokens Circulating
                                   v
                       +-----------------------+
                       |  Order Spend (Punched)|
                       +-----------+-----------+
                                   |
                                   v  BURNING
               +---------------------------------------+
               |  Pessimistic Lock on User Wallet      |
               |  Select N active units from DB        |
               |  DELETE / Burn records from table     |
               |  Deduct Balance from `token_wallet`   |
               |  Log in `token_transactions`          |
               +-------------------+-------------------+
                                   |
                                   v  REFUND (If Cancelled)
               +---------------------------------------+
               |  Re-mint fresh SHA-256 unit hashes    |
               |  Credit Balance + Log Refund Transaction
               +---------------------------------------+
```

1. **Minting (Top-Up)**:
   - When a user adds balance (via Razorpay UPI, Cashier, or Admin Grant), each whole integer unit generates a unique SHA-256 hash.
   - Units are written in a high-speed JDBC batch (`INSERT INTO token_units`).
   - The user's wallet balance is credited and an immutable `TOPUP` transaction entry is saved.
2. **Burning (Spending)**:
   - When paying for food, active token units belonging to the user are queried (`SELECT ... WHERE owner_id = :id AND status = 'ACTIVE' LIMIT :n`).
   - The identified units are physically burned (`DELETE FROM token_units WHERE id IN (...)`).
   - The wallet balance is decremented and an immutable `SPEND` record is created.
3. **Reminting (Refunds)**:
   - To prevent reuse of burned keys or replay attacks, refunds do **not** resurrect old hashes.
   - Fresh cryptographic hashes are minted and credited to the user's wallet, linked to the original order reference.

---

## 4. Data Model & Database Schema

### Table: `token_wallets`
Stores the fast-path aggregate balance for each user.
```sql
CREATE TABLE token_wallets (
    user_id BIGINT PRIMARY KEY,
    user_name VARCHAR(150),
    mobile_number VARCHAR(30),
    balance NUMERIC(19, 2) NOT NULL DEFAULT 0.00,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL
);
```

### Table: `token_units`
Stores individual serialized digital token units.
```sql
CREATE TABLE token_units (
    id BIGSERIAL PRIMARY KEY,
    token_hash VARCHAR(64) UNIQUE NOT NULL,
    owner_id BIGINT,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    spent_at TIMESTAMP WITHOUT TIME ZONE,
    order_ref VARCHAR(100)
);

CREATE INDEX idx_token_units_owner_status ON token_units (owner_id, status);
CREATE INDEX idx_token_units_hash ON token_units (token_hash);
```

### Table: `token_transactions`
Audit trail of every balance shift.
```sql
CREATE TABLE token_transactions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    user_name VARCHAR(255),
    mobile_number VARCHAR(255),
    amount NUMERIC(19, 2) NOT NULL,
    type VARCHAR(20) NOT NULL, -- TOPUP, SPEND, REFUND, TRANSFER, ADJUSTMENT
    description VARCHAR(500),
    reference_id VARCHAR(100),
    balance_after NUMERIC(19, 2),
    timestamp TIMESTAMP WITHOUT TIME ZONE NOT NULL
);

CREATE INDEX idx_token_tx_user_time ON token_transactions (user_id, timestamp DESC);
CREATE INDEX idx_token_tx_ref ON token_transactions (reference_id);
```

---

## 5. Concurrency & Double-Spend Prevention

During peak break hours, hundreds of students may punch orders simultaneously. The service employs **Pessimistic Write Locking** (`SELECT ... FOR UPDATE`):

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT w FROM TokenWallet w WHERE w.userId = :userId")
Optional<TokenWallet> findByUserIdWithLock(@Param("userId") Long userId);
```

1. Any spend, refund, or top-up acquires an exclusive lock on that user's specific row.
2. Concurrent requests for the same wallet queue up in Postgres; overdraft attempts fail immediately with `INSUFFICIENT_TOKENS` (HTTP 409 Conflict).
3. Database isolation level guarantees that non-conflicting users execute in parallel without lock contention.

---

## 6. API Specification & Protocols

Default Port: `8085`  
Required Authentication Header: `X-API-KEY: <RITZ_API_KEY>`

### Summary Table

| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `GET` | `/api/v1/tokens/balance/{userId}` | Get user token balance |
| `GET` | `/api/v1/tokens/wallet/{userId}` | Get detailed wallet object |
| `GET` | `/api/v1/tokens/wallets` | List all wallets |
| `POST` | `/api/v1/tokens/topup` | Mint tokens & credit wallet |
| `POST` | `/api/v1/tokens/spend` | Burn tokens & debit wallet |
| `POST` | `/api/v1/tokens/refund` | Remint tokens for cancelled order |
| `GET` | `/api/v1/tokens/transactions/{userId}`| User transaction history |
| `GET` | `/api/v1/tokens/transactions` | System-wide transaction history |
| `GET` | `/api/v1/tokens/stats` | System circulation metrics |
| `GET` | `/api/v1/tokens/circulation` | Paginated serialized unit list |
| `GET` | `/actuator/health` | Service health status |

### Request & Response Examples

#### 1. Top-Up (Mint)
**`POST /api/v1/tokens/topup`**
```json
{
  "userId": 42,
  "userName": "Alex Mercer",
  "mobileNumber": "9876543210",
  "amount": 250,
  "referenceId": "PAY_RZP_99812",
  "description": "UPI Wallet Topup"
}
```
**Response (200 OK):**
```json
{
  "success": true,
  "userId": 42,
  "newBalance": 750.00,
  "amountCredited": 250,
  "message": "Successfully added 250 Ritz Tokens"
}
```

#### 2. Spend (Burn)
**`POST /api/v1/tokens/spend`**
```json
{
  "userId": 42,
  "amount": 80,
  "orderRef": "ORD-CANTEEN-5431",
  "description": "Lunch Meal Combo"
}
```
**Response (200 OK):**
```json
{
  "success": true,
  "userId": 42,
  "newBalance": 670.00,
  "amountDeducted": 80,
  "orderRef": "ORD-CANTEEN-5431",
  "message": "Successfully spent 80 Ritz Tokens"
}
```

#### 3. Global Stats
**`GET /api/v1/tokens/stats`**
```json
{
  "totalCirculation": 184500.00,
  "activeWallets": 1284,
  "totalUsers": 1890,
  "serializedUnitsTotal": 184500,
  "activeSerializedUnits": 184500
}
```

---

## 7. Proxmox VE Deployment Guide

### Option A: Deployment via Docker Compose (Recommended)

1. **Create an LXC Container or VM**:
   - OS: Ubuntu 24.04 or Debian 12 (minimal).
   - CPU: 2 Cores.
   - RAM: 2 GB - 4 GB.
   - Storage: 20 GB SSD.
2. **Install Docker & Git on the VM**:
   ```bash
   sudo apt update && sudo apt install -y docker.io docker-compose git
   sudo systemctl enable --now docker
   ```
3. **Clone or Copy the Service**:
   ```bash
   git clone <REPO_URL> /opt/ritz-token-service
   cd /opt/ritz-token-service
   ```
4. **Configure Secrets**:
   Edit `docker-compose.yml` or set environment variables:
   ```yaml
   POSTGRES_PASSWORD: <YOUR_STRONG_DB_PASSWORD>
   RITZ_API_KEY: <GENERATE_LONG_SECURE_TOKEN>
   ```
5. **Start the Stack**:
   ```bash
   docker-compose up -d --build
   ```
6. **Verify Running Containers**:
   ```bash
   docker ps
   curl -s http://localhost:8085/actuator/health
   # Returns: {"status":"UP"}
   ```

---

### Option B: Native Bare-Metal / Systemd Deployment

1. **Install Java 17 and PostgreSQL**:
   ```bash
   sudo apt update
   sudo apt install -y openjdk-17-jre-headless postgresql postgresql-contrib
   ```
2. **Create Database & User**:
   ```bash
   sudo -u postgres psql
   ```
   ```sql
   CREATE DATABASE ritz_token_db;
   CREATE USER ritz_admin WITH ENCRYPTED PASSWORD 'RitzSecurePass2026';
   GRANT ALL PRIVILEGES ON DATABASE ritz_token_db TO ritz_admin;
   \q
   ```
3. **Build and Place the Application**:
   ```bash
   ./mvnw clean package -DskipTests
   sudo mkdir -p /opt/ritz-token-service
   sudo cp target/ritz-token-service-1.0.0.jar /opt/ritz-token-service/ritz-token-service.jar
   sudo useradd -r -s /bin/false ritz
   sudo chown -R ritz:ritz /opt/ritz-token-service
   ```
4. **Install & Start Systemd Service**:
   ```bash
   sudo cp deployment/ritz-token-service.service /etc/systemd/system/
   sudo systemctl daemon-reload
   sudo systemctl enable --now ritz-token-service
   ```

---

## 8. Tillo POS Client Integration

To enable communication between the existing Tillo POS backend and the standalone Ritz Proxmox VM, configure `backend/src/main/resources/application.properties` (or environment variables in `.env`):

```properties
# Enable external microservice mode (bypasses local ledger)
ritz.service.enabled=true

# IP and port of your Proxmox VM
ritz.service.base-url=http://192.168.1.150:8085/api/v1/tokens

# Shared Secret API Key configured on the VM
ritz.service.api-key=ritz-secret-api-key-2026
```

### Fallback Resilience
If `ritz.service.enabled` is set to `false`, the POS backend automatically operates using its internal JPA fallback logic, ensuring offline continuity if the VM is ever undergoing planned host maintenance.

---

## 9. Disaster Recovery, Health Monitoring & Auditing

### Live Interactive Documentation (Swagger)
Access Swagger UI directly in your browser:
```
http://<PROXMOX_VM_IP>:8085/swagger-ui.html
```

### Automated Database Backup Cron
Add a daily database dump cron job on the Proxmox VM:
```bash
0 2 * * * docker exec ritz-postgres pg_dump -U ritz_admin ritz_token_db | gzip > /backups/ritz_backup_$(date +\%Y\%m\%d).sql.gz
```

### Proxmox Backup Server (PBS) Integration
Because this service is hosted on a Proxmox VM, enable Proxmox Backup Server (PBS) scheduled snapshots to guarantee point-in-time recovery for the entire OS and database volume.
