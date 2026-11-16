# 🏛️ Ritz Token Microservice - Proxmox VM & Standalone Deployment

This is the standalone **Ritz Token Ledger & Digital Wallet Microservice**, extracted from the Tillo POS ecosystem for independent hosting on a **Proxmox Virtual Machine / LXC container**.

---

## 🚀 Overview & Features

- **Serialized Unit Ledger**: Physical minting and SHA-256 hashed unit tracking for every circulating token.
- **Atomic Balance Operations**: Pessimistic concurrency locking to eliminate double-spend and race conditions during peak canteen rush hours.
- **RESTful Integration API**: Secure REST endpoints authenticated via `X-API-KEY`.
- **OpenAPI / Swagger Documentation**: Interactive test console at `/swagger-ui.html`.
- **Turnkey Proxmox Deployment**: Included `Dockerfile`, `docker-compose.yml`, and Linux `systemd` unit files.

---

## 🛠️ API Specification

Base Path: `/api/v1/tokens`  
Header: `X-API-KEY: <your-configured-api-key>`

### 1. Get User Balance
* **GET** `/api/v1/tokens/balance/{userId}`
* **Response**:
```json
{
  "userId": 101,
  "balance": 250.00,
  "currency": "Ritz Token"
}
```

### 2. Top-Up (Mint Tokens)
* **POST** `/api/v1/tokens/topup`
* **Request**:
```json
{
  "userId": 101,
  "userName": "John Doe",
  "mobileNumber": "9876543210",
  "amount": 100,
  "referenceId": "RAZORPAY_PAY_12345",
  "description": "UPI Wallet Topup"
}
```
* **Response**:
```json
{
  "success": true,
  "userId": 101,
  "newBalance": 350.00,
  "amountCredited": 100,
  "message": "Successfully added 100 Ritz Tokens"
}
```

### 3. Spend (Burn Tokens)
* **POST** `/api/v1/tokens/spend`
* **Request**:
```json
{
  "userId": 101,
  "amount": 40,
  "orderRef": "ORD-2026-9921",
  "description": "Lunch Canteen Order"
}
```
* **Response**:
```json
{
  "success": true,
  "userId": 101,
  "newBalance": 310.00,
  "amountDeducted": 40,
  "orderRef": "ORD-2026-9921",
  "message": "Successfully spent 40 Ritz Tokens"
}
```

### 4. Refund (Remint Tokens)
* **POST** `/api/v1/tokens/refund`
* **Request**:
```json
{
  "userId": 101,
  "amount": 40,
  "orderRef": "ORD-2026-9921",
  "reason": "Out of stock item"
}
```

### 5. Global Stats & Circulation
* **GET** `/api/v1/tokens/stats`: System-wide circulation, active wallets, serialized totals.
* **GET** `/api/v1/tokens/circulation?page=0&size=20`: Paginated serialized hashes.
* **GET** `/api/v1/tokens/transactions/{userId}`: User transaction history.
* **GET** `/api/v1/tokens/transactions`: Complete system transaction ledger.
* **GET** `/api/v1/tokens/wallets`: All user balances.

---

## 🖥️ Proxmox VM Setup Guide

### Option 1: Docker Compose on Ubuntu/Debian VM
1. Spin up an Ubuntu 22.04 / 24.04 or Debian 12 VM in Proxmox.
2. Install Docker and Docker Compose:
   ```bash
   sudo apt update && sudo apt install -y docker.io docker-compose
   ```
3. Copy this project folder onto the VM (`/opt/ritz-token-service`).
4. Set your passwords in `docker-compose.yml`.
5. Start the service:
   ```bash
   docker-compose up -d --build
   ```
6. Verify service health:
   ```bash
   curl http://localhost:8085/actuator/health
   ```

### Option 2: Native Systemd Service
1. Install Java 17 and PostgreSQL:
   ```bash
   sudo apt update && sudo apt install -y openjdk-17-jre postgresql
   ```
2. Create database and user:
   ```sql
   CREATE DATABASE ritz_token_db;
   CREATE USER ritz_admin WITH ENCRYPTED PASSWORD 'YourPassword';
   GRANT ALL PRIVILEGES ON DATABASE ritz_token_db TO ritz_admin;
   ```
3. Copy the built jar to `/opt/ritz-token-service/ritz-token-service.jar`.
4. Copy `deployment/ritz-token-service.service` to `/etc/systemd/system/`.
5. Enable and start:
   ```bash
   sudo systemctl daemon-reload
   sudo systemctl enable --now ritz-token-service
   ```

---

## 🔗 How Tillo POS Backend Connects to This VM

In Tillo POS Backend `application.properties`:
```properties
ritz.service.enabled=true
ritz.service.base-url=http://<PROXMOX_VM_IP>:8085/api/v1/tokens
ritz.service.api-key=ritz-secret-api-key-2026
```
