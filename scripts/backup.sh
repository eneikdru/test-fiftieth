#!/usr/bin/env bash
set -euo pipefail

# Configuration defaults
POSTGRES_HOST="${POSTGRES_HOST:-localhost}"
POSTGRES_PORT="${POSTGRES_PORT:-5432}"
POSTGRES_DB="${POSTGRES_DB:-epidemiology_db}"
POSTGRES_USER="${POSTGRES_USER:-postgres}"
POSTGRES_PASSWORD="${POSTGRES_PASSWORD:-postgres}"
UPLOADS_DIR="${UPLOADS_DIR:-./data/uploads}"
BACKUP_DIR="${BACKUP_DIR:-./backups}"
BACKUP_RETENTION_DAYS="${BACKUP_RETENTION_DAYS:-7}"

TIMESTAMP="${OVERRIDE_TIMESTAMP:-$(date +"%Y%m%d_%H%M%S")}"

echo "=== Starting Automated Backup process at $(date) ==="
echo "Backup Directory: ${BACKUP_DIR}"
echo "Database Target: ${POSTGRES_HOST}:${POSTGRES_PORT}/${POSTGRES_DB}"
echo "Uploads Target: ${UPLOADS_DIR}"
echo "Retention Policy: ${BACKUP_RETENTION_DAYS} days"

# Ensure output directories exist
mkdir -p "${BACKUP_DIR}"
mkdir -p "${UPLOADS_DIR}"

# 1. Database Backup (pg_dump without locking)
DB_BACKUP_FILE="${BACKUP_DIR}/db_${POSTGRES_DB}_${TIMESTAMP}.sql.gz"
echo "Backing up database '${POSTGRES_DB}' to ${DB_BACKUP_FILE}..."

if [ -n "${SQLITE_DB_PATH:-}" ] && [ -f "${SQLITE_DB_PATH}" ] && command -v sqlite3 &> /dev/null; then
    echo "Backing up SQLite database from '${SQLITE_DB_PATH}'..."
    sqlite3 "${SQLITE_DB_PATH}" .dump | gzip > "${DB_BACKUP_FILE}"
    echo "Database backup completed successfully."
elif command -v pg_dump &> /dev/null; then
    PGPASSWORD="${POSTGRES_PASSWORD}" pg_dump \
        -h "${POSTGRES_HOST}" \
        -p "${POSTGRES_PORT}" \
        -U "${POSTGRES_USER}" \
        -d "${POSTGRES_DB}" \
        --clean \
        --if-exists \
        --no-owner \
        --no-privileges \
        | gzip > "${DB_BACKUP_FILE}"
    echo "Database backup completed successfully."
elif [ "${ALLOW_MOCK_BACKUP:-0}" -eq 1 ]; then
    echo "pg_dump not found in environment, creating mock database dump for verification..."
    echo "-- Mock DB Backup for ${POSTGRES_DB} created at ${TIMESTAMP}" | gzip > "${DB_BACKUP_FILE}"
    echo "Mock database backup completed."
else
    echo "ERROR: pg_dump or sqlite3 utility not found and ALLOW_MOCK_BACKUP is not enabled." >&2
    exit 1
fi

# 2. File Uploads Backup
UPLOADS_BACKUP_FILE="${BACKUP_DIR}/uploads_${TIMESTAMP}.tar.gz"
echo "Backing up file uploads from '${UPLOADS_DIR}' to ${UPLOADS_BACKUP_FILE}..."

if [ -d "${UPLOADS_DIR}" ]; then
    tar -czf "${UPLOADS_BACKUP_FILE}" -C "${UPLOADS_DIR}" .
    echo "Uploads backup completed successfully."
else
    echo "WARNING: Uploads directory '${UPLOADS_DIR}' does not exist. Creating empty backup."
    tar -czf "${UPLOADS_BACKUP_FILE}" --files-from /dev/null
fi

# 3. Off-site Synchronization
OFFSITE_BACKUP_DIR="${OFFSITE_BACKUP_DIR:-}"
if [ -n "${OFFSITE_BACKUP_DIR}" ]; then
    echo "Synchronizing backups to off-site storage at '${OFFSITE_BACKUP_DIR}'..."
    mkdir -p "${OFFSITE_BACKUP_DIR}"
    cp -p "${DB_BACKUP_FILE}" "${OFFSITE_BACKUP_DIR}/" || echo "WARNING: Failed to sync DB backup off-site."
    cp -p "${UPLOADS_BACKUP_FILE}" "${OFFSITE_BACKUP_DIR}/" || echo "WARNING: Failed to sync Uploads backup off-site."
    echo "Off-site synchronization completed."
fi

# 4. Retention Policy Cleanup
echo "Cleaning up backups older than ${BACKUP_RETENTION_DAYS} days..."
if [ "${BACKUP_RETENTION_DAYS}" -ge 0 ]; then
    # Delete backup files matching backup patterns modified more than BACKUP_RETENTION_DAYS ago
    find "${BACKUP_DIR}" -maxdepth 1 -type f \( -name "db_*.sql.gz" -o -name "uploads_*.tar.gz" -o -name "backup_*.tar.gz" \) -mtime +"${BACKUP_RETENTION_DAYS}" -exec rm -v {} \; || true
fi

echo "=== Backup completed successfully at $(date) ==="
