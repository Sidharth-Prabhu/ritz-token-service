#!/usr/bin/env bash
# ==============================================================================
# RITZ Wallet - Database Backup & Disaster Recovery Automation Script
# Rajalakshmi Institute of Technology (RIT)
# Supports: MySQL 8+ & PostgreSQL 16+
# ==============================================================================

set -euo pipefail

BACKUP_DIR="${BACKUP_DIR:-/var/backups/ritz-wallet}"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
RETENTION_DAYS=30

mkdir -p "${BACKUP_DIR}"

log() {
    echo "[$(date -u +"%Y-%m-%dT%H:%M:%SZ")] $1"
}

usage() {
    echo "Usage: $0 {backup-mysql|backup-postgres|restore-mysql <file>|restore-postgres <file>|list}"
    exit 1
}

backup_mysql() {
    local DB_HOST="${DB_HOST:-localhost}"
    local DB_PORT="${DB_PORT:-3306}"
    local DB_USER="${DB_USERNAME:-ritz_user}"
    local DB_PASS="${DB_PASSWORD:-RitzWalletPass2026!}"
    local DB_NAME="${DB_NAME:-ritz_wallet}"
    local OUT_FILE="${BACKUP_DIR}/ritz_mysql_${DB_NAME}_${TIMESTAMP}.sql.gz"

    log "Starting MySQL backup for database '${DB_NAME}'..."
    mysqldump \
        --host="${DB_HOST}" \
        --port="${DB_PORT}" \
        --user="${DB_USER}" \
        --password="${DB_PASS}" \
        --single-transaction \
        --quick \
        --routines \
        --triggers \
        "${DB_NAME}" | gzip > "${OUT_FILE}"

    # Compute SHA-256 Checksum for integrity verification
    sha256sum "${OUT_FILE}" > "${OUT_FILE}.sha256"
    log "Backup created successfully: ${OUT_FILE} (Size: $(du -h "${OUT_FILE}" | cut -f1))"

    prune_old_backups "ritz_mysql"
}

backup_postgres() {
    local PG_HOST="${PG_HOST:-localhost}"
    local PG_PORT="${PG_PORT:-5432}"
    local PG_USER="${PG_DB_USER:-ritz_admin}"
    local PGPASSWORD="${PG_DB_PASS:-RitzSecurePassword2026}"
    local PG_DB="${PG_DB_NAME:-ritz_token_db}"
    local OUT_FILE="${BACKUP_DIR}/ritz_postgres_${PG_DB}_${TIMESTAMP}.sql.gz"

    export PGPASSWORD
    log "Starting PostgreSQL backup for database '${PG_DB}'..."
    pg_dump \
        -h "${PG_HOST}" \
        -p "${PG_PORT}" \
        -U "${PG_USER}" \
        -F p \
        "${PG_DB}" | gzip > "${OUT_FILE}"

    sha256sum "${OUT_FILE}" > "${OUT_FILE}.sha256"
    log "Backup created successfully: ${OUT_FILE} (Size: $(du -h "${OUT_FILE}" | cut -f1))"

    prune_old_backups "ritz_postgres"
}

restore_mysql() {
    local RESTORE_FILE="$1"
    if [[ ! -f "${RESTORE_FILE}" ]]; then
        echo "Error: Backup file not found: ${RESTORE_FILE}"
        exit 1
    fi

    local DB_HOST="${DB_HOST:-localhost}"
    local DB_PORT="${DB_PORT:-3306}"
    local DB_USER="${DB_USERNAME:-ritz_user}"
    local DB_PASS="${DB_PASSWORD:-RitzWalletPass2026!}}"
    local DB_NAME="${DB_NAME:-ritz_wallet}"

    log "Verifying checksum if present..."
    if [[ -f "${RESTORE_FILE}.sha256" ]]; then
        sha256sum -c "${RESTORE_FILE}.sha256"
    fi

    log "Restoring MySQL database '${DB_NAME}' from ${RESTORE_FILE}..."
    gunzip < "${RESTORE_FILE}" | mysql \
        --host="${DB_HOST}" \
        --port="${DB_PORT}" \
        --user="${DB_USER}" \
        --password="${DB_PASS}" \
        "${DB_NAME}"

    log "MySQL restoration complete."
}

restore_postgres() {
    local RESTORE_FILE="$1"
    if [[ ! -f "${RESTORE_FILE}" ]]; then
        echo "Error: Backup file not found: ${RESTORE_FILE}"
        exit 1
    fi

    local PG_HOST="${PG_HOST:-localhost}"
    local PG_PORT="${PG_PORT:-5432}"
    local PG_USER="${PG_DB_USER:-ritz_admin}"
    local PGPASSWORD="${PG_DB_PASS:-RitzSecurePassword2026}"
    local PG_DB="${PG_DB_NAME:-ritz_token_db}"

    export PGPASSWORD
    log "Restoring PostgreSQL database '${PG_DB}' from ${RESTORE_FILE}..."
    gunzip < "${RESTORE_FILE}" | psql \
        -h "${PG_HOST}" \
        -p "${PG_PORT}" \
        -U "${PG_USER}" \
        -d "${PG_DB}"

    log "PostgreSQL restoration complete."
}

prune_old_backups() {
    local PREFIX="$1"
    log "Pruning backups older than ${RETENTION_DAYS} days for ${PREFIX}..."
    find "${BACKUP_DIR}" -name "${PREFIX}_*.sql.gz*" -mtime +"${RETENTION_DAYS}" -delete
    log "Pruning completed."
}

list_backups() {
    echo "=========================================="
    echo "RITZ Wallet Available Backups in ${BACKUP_DIR}:"
    echo "=========================================="
    ls -lh "${BACKUP_DIR}"/*.sql.gz 2>/dev/null || echo "No backups found."
}

case "${1:-}" in
    backup-mysql)
        backup_mysql
        ;;
    backup-postgres)
        backup_postgres
        ;;
    restore-mysql)
        [[ -z "${2:-}" ]] && usage
        restore_mysql "$2"
        ;;
    restore-postgres)
        [[ -z "${2:-}" ]] && usage
        restore_postgres "$2"
        ;;
    list)
        list_backups
        ;;
    *)
        usage
        ;;
esac
