#!/usr/bin/env bash
# ==============================================================================
# RITZ Wallet - Nginx Reverse Proxy Setup Script for ritz.rit-services.in
# Configures Nginx with Cloudflare Real-IP restoration and SSL termination
# ==============================================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DOMAIN="ritz.rit-services.in"
NGINX_CONF_DIR="/etc/nginx"
SSL_CERT_DIR="/etc/ssl/certs"
SSL_KEY_DIR="/etc/ssl/private"

log() {
    echo -e "\033[1;32m[RITZ-NGINX]\033[0m $1"
}

warn() {
    echo -e "\033[1;33m[WARNING]\033[0m $1"
}

error() {
    echo -e "\033[1;31m[ERROR]\033[0m $1"
    exit 1
}

# 1. Check Root Privileges
if [[ $EUID -ne 0 ]]; then
    warn "This script requires root privileges to configure /etc/nginx."
    echo "Please execute with sudo: sudo ./deployment/setup-nginx.sh"
    exit 1
fi

# 2. Check / Install Nginx
if ! command -v nginx >/dev/null 2>&1; then
    log "Nginx not found. Installing Nginx via apt..."
    apt-get update -y
    apt-get install -y nginx
fi

# 3. Create Necessary Directories
mkdir -p "${NGINX_CONF_DIR}/conf.d"
mkdir -p "${NGINX_CONF_DIR}/sites-available"
mkdir -p "${NGINX_CONF_DIR}/sites-enabled"
mkdir -p "${SSL_CERT_DIR}"
mkdir -p "${SSL_KEY_DIR}"
mkdir -p /var/www/certbot

# 4. Copy Cloudflare Real IP Configuration
log "Installing Cloudflare Real IP configuration to ${NGINX_CONF_DIR}/conf.d/cloudflare_ips.conf..."
cp "${SCRIPT_DIR}/nginx/cloudflare_ips.conf" "${NGINX_CONF_DIR}/conf.d/cloudflare_ips.conf"

# 5. Check or Create SSL Certificate Placeholder
CERT_PATH="${SSL_CERT_DIR}/${DOMAIN}.pem"
KEY_PATH="${SSL_KEY_DIR}/${DOMAIN}.key"

if [[ ! -f "${CERT_PATH}" || ! -f "${KEY_PATH}" ]]; then
    log "No SSL certificate found at ${CERT_PATH}. Generating a temporary self-signed certificate for Cloudflare Full SSL mode..."
    openssl req -x509 -nodes -days 3650 -newkey rsa:2048 \
        -keyout "${KEY_PATH}" \
        -out "${CERT_PATH}" \
        -subj "/C=IN/ST=TamilNadu/L=Chennai/O=RIT/CN=${DOMAIN}"
    chmod 600 "${KEY_PATH}"
    chmod 644 "${CERT_PATH}"
    log "Temporary certificate generated. (Cloudflare 'Full' mode is now supported out of the box)."
    log "For Cloudflare 'Full (Strict)' mode, replace these with a Cloudflare Origin CA certificate from your Cloudflare dashboard."
fi

# 6. Copy Site Configuration
log "Installing site configuration for ${DOMAIN}..."
cp "${SCRIPT_DIR}/nginx/ritz.rit-services.in.conf" "${NGINX_CONF_DIR}/sites-available/${DOMAIN}.conf"

# Enable site via symlink
ln -sf "${NGINX_CONF_DIR}/sites-available/${DOMAIN}.conf" "${NGINX_CONF_DIR}/sites-enabled/${DOMAIN}.conf"

# Remove default site if present
if [[ -f "${NGINX_CONF_DIR}/sites-enabled/default" ]]; then
    rm -f "${NGINX_CONF_DIR}/sites-enabled/default"
    log "Disabled default Nginx site."
fi

# 7. Test Nginx Configuration Syntax
log "Validating Nginx configuration syntax..."
nginx -t

# 8. Reload / Restart Nginx
log "Enabling and reloading Nginx service..."
systemctl enable nginx
systemctl reload nginx || systemctl restart nginx

log "======================================================================"
log "SUCCESS: Nginx reverse proxy configured for https://${DOMAIN}"
log "Traffic on port 80 & 443 is now proxying to http://127.0.0.1:8085"
log "======================================================================"
