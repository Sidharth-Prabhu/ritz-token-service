# RITZ Wallet - Production Deployment & Operations Guide
**Rajalakshmi Institute of Technology (RIT)**

---

## 1. System Requirements & Prerequisites

### Minimum Hardware Sizing
- **CPU:** 2 vCPUs (x86_64 or ARM64)
- **RAM:** 4 GB RAM (2 GB dedicated to JVM heap, 1 GB to database, 1 GB system/OS buffer)
- **Disk:** 40 GB NVMe / SSD storage
- **Network:** 100 Mbps public network interface with static IPv4

### Operating System & Dependencies
- **OS:** Ubuntu 22.04 LTS / 24.04 LTS or Debian 12
- **Container Runtime:** Docker Engine 24.0+ & Docker Compose v2.20+
- **Host Tools:** `git`, `curl`, `wget`, `gzip`, `openssl`, `ufw`

---

## 2. Docker Compose Deployment (Recommended)

### Step 1: Clone Repository & Setup Environment
```bash
git clone https://github.com/rit-chennai/ritz-token-service.git /opt/ritz-wallet
cd /opt/ritz-wallet

# Copy environment template
cp .env.example .env

# Generate secure random secret for JWT signing
JWT_SECRET_GEN=$(openssl rand -base64 48)
sed -i "s|JWT_SECRET=.*|JWT_SECRET=${JWT_SECRET_GEN}|g" .env

# Edit .env with production passwords and domain settings
nano .env
```

### Step 2: Build and Launch Services
```bash
# Build images and start in background
docker compose up -d --build

# Inspect running containers
docker compose ps
```

You should see 3 containers running and healthy:
1. `ritz-mysql` (MySQL 8.4 on port 3306)
2. `ritz-backend` (Spring Boot 3 on port 8085)
3. `ritz-nginx` (Nginx reverse proxy on port 80 & 443)

### Step 3: Verify Health Checks
```bash
# Verify internal backend health
curl -s http://localhost:8085/actuator/health | jq .

# Verify Nginx reverse proxy endpoint
curl -s http://localhost/actuator/health | jq .
```
Both commands must report `{"status":"UP"}`.

---

## 3. Production Nginx & Let's Encrypt SSL Setup

### Step 1: Open Firewall Ports
```bash
sudo ufw allow 22/tcp
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
sudo ufw enable
```

### Step 2: Obtain SSL Certificate via Certbot
```bash
sudo apt update && sudo apt install -y certbot

# Request certificate using webroot method or standalone mode
sudo certbot certonly --standalone \
  -d ritz.ritchennai.edu.in \
  --email sysadmin@ritchennai.edu.in \
  --agree-tos --no-eff-email
```

### Step 3: Enable HTTPS in `deployment/nginx/nginx.conf`
Uncomment the HTTPS block in `deployment/nginx/nginx.conf`:
```nginx
server {
    listen 443 ssl http2;
    server_name ritz.ritchennai.edu.in;

    ssl_certificate /etc/letsencrypt/live/ritz.ritchennai.edu.in/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/ritz.ritchennai.edu.in/privkey.pem;
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers HIGH:!aNULL:!MD5;
    ...
```

Reload Nginx:
```bash
docker compose exec nginx nginx -s reload
```

---

## 3. Cloudflare DNS & Nginx Reverse Proxy for ritz.rit-services.in

When hosting behind **Cloudflare DNS** (with Orange Cloud proxy enabled), traffic routes through Cloudflare's Edge network to your origin server.

### Automated Setup Script
Run the automated installer script (as root or with sudo):
```bash
sudo ./deployment/setup-nginx.sh
```
This script automatically:
1. Installs Nginx (if not installed).
2. Configures real client IP restoration using official Cloudflare IPv4 & IPv6 CIDRs in `/etc/nginx/conf.d/cloudflare_ips.conf`.
3. Deploys the site configuration `deployment/nginx/ritz.rit-services.in.conf`.
4. Provisions a temporary SSL certificate to immediately enable Cloudflare **Full** SSL mode.
5. Tests configuration syntax and reloads Nginx.

### Cloudflare Dashboard Configuration
1. **DNS Management**:
   - Type: `A`
   - Name: `ritz` (resolves to `ritz.rit-services.in`)
   - IPv4 Address: Origin Public IP (e.g., `111.93.231.118`)
   - Proxy status: **Proxied** (Orange Cloud ☁️)
2. **SSL/TLS Settings**:
   - Set encryption mode to **Full** (or **Full (Strict)** if using Cloudflare Origin CA certificate).
   - Under **Edge Certificates**, enable **Always Use HTTPS** and **Automatic HTTPS Rewrites**.
3. **Network Settings**:
   - Enable **WebSockets** (Enabled by default).
   - Enable **gRPC** and **HTTP/3 (with QUIC)** if desired.

---

## 4. Alternative: Cloudflare Tunnel (Zero Trust / Argo Tunnel)

If your campus or college network sits behind a strict NAT firewall where inbound ports 80/443 cannot be forwarded:
1. Use Cloudflare Tunnel (`cloudflared`) to establish an encrypted outbound-only tunnel to Cloudflare.
2. Refer to the configuration template at `deployment/cloudflare/cloudflared-config.example.yml`.
3. No inbound port forwarding or public IP exposure is required.

---

## 5. Standalone Host (Systemd) Deployment

If deploying natively without Docker on an Ubuntu Linux VPS:

### Step 1: Install Java 17 & MySQL
```bash
sudo apt update
sudo apt install -y openjdk-17-jre-headless mysql-server nginx
```

### Step 2: Create Service User & Directory
```bash
sudo useradd -r -s /bin/false -d /opt/ritz-wallet ritz
sudo mkdir -p /opt/ritz-wallet/logs
sudo chown -R ritz:ritz /opt/ritz-wallet
```

### Step 3: Copy Built JAR & Systemd Service
```bash
sudo cp target/ritz-token-service-1.0.0.jar /opt/ritz-wallet/app.jar
sudo cp deployment/ritz-token-service.service /etc/systemd/system/ritz-token-service.service

sudo systemctl daemon-reload
sudo systemctl enable ritz-token-service
sudo systemctl start ritz-token-service
sudo systemctl status ritz-token-service
```

---

## 5. Automated Database Backups & Disaster Recovery

An automated disaster recovery utility is included in `deployment/backup-restore.sh`.

### Manual Backup
```bash
# For MySQL:
./deployment/backup-restore.sh backup-mysql

# For PostgreSQL:
./deployment/backup-restore.sh backup-postgres
```
Backups are saved to `/var/backups/ritz-wallet/` with SHA-256 integrity checksums.

### Schedule Automated Daily Backups via Cron
```bash
sudo crontab -e
```
Add the following entry for midnight backup with 30-day automated pruning:
```cron
0 0 * * * /opt/ritz-wallet/deployment/backup-restore.sh backup-mysql >> /var/log/ritz-backup.log 2>&1
```

### Restoration
```bash
./deployment/backup-restore.sh restore-mysql /var/backups/ritz-wallet/ritz_mysql_ritz_wallet_20261117_000000.sql.gz
```

---

## 6. Key Management & Credential Rotation

### Rotating External Application API Keys
When an external partner (e.g. Tillo canteen operator) requests credential rotation:
1. Log in to Developer Portal or Admin Console as `SUPER_ADMIN` or `APP_DEVELOPER`.
2. Navigate to Applications -> Select Application (e.g., `TILLO`).
3. Click **Rotate Credentials** (or `POST /api/v1/applications/{appId}/rotate-credentials`).
4. Securely transmit the newly generated Secret Key to the authorized application administrator.
5. Previous secret keys are invalidated immediately upon rotation.
