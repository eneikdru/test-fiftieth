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
DB_BACKUP_FILE="${DB_BACKUP_FILE:-}"
UPLOADS_BACKUP_FILE="${UPLOADS_BACKUP_FILE:-}"
ALLOW_MOCK_RESTORE="${ALLOW_MOCK_RESTORE:-0}"

echo "=== Starting Automated Restore process at $(date) ==="
echo "Target Database: ${POSTGRES_HOST}:${POSTGRES_PORT}/${POSTGRES_DB}"
echo "Target Uploads Directory: ${UPLOADS_DIR}"

if [ -z "${DB_BACKUP_FILE}" ]; then
    DB_BACKUP_FILE="$(ls -t "${BACKUP_DIR}"/db_"${POSTGRES_DB}"_*.sql.gz 2>/dev/null | head -n 1 || true)"
fi

if [ -z "${UPLOADS_BACKUP_FILE}" ]; then
    UPLOADS_BACKUP_FILE="$(ls -t "${BACKUP_DIR}"/uploads_*.tar.gz 2>/dev/null | head -n 1 || true)"
fi

# 1. Restore Database
if [ -n "${DB_BACKUP_FILE}" ] && [ -f "${DB_BACKUP_FILE}" ]; then
    echo "Restoring database from ${DB_BACKUP_FILE}..."
    if command -v psql &> /dev/null; then
        gunzip -c "${DB_BACKUP_FILE}" | PGPASSWORD="${POSTGRES_PASSWORD}" psql \
            -h "${POSTGRES_HOST}" \
            -p "${POSTGRES_PORT}" \
            -U "${POSTGRES_USER}" \
            -d "${POSTGRES_DB}"
        echo "Database restore completed successfully."
    elif [ -n "${SQLITE_DB_PATH:-}" ]; then
        echo "Restoring SQLite database to '${SQLITE_DB_PATH}'..."
        mkdir -p "$(dirname "${SQLITE_DB_PATH}")"
        rm -f "${SQLITE_DB_PATH}"
        if command -v sqlite3 &> /dev/null; then
            gunzip -c "${DB_BACKUP_FILE}" | sqlite3 "${SQLITE_DB_PATH}"
        elif command -v python3 &> /dev/null; then
            gunzip -c "${DB_BACKUP_FILE}" | python3 -c "import sqlite3, sys; conn = sqlite3.connect('${SQLITE_DB_PATH}'); conn.executescript(sys.stdin.read()); conn.commit()"
        else
            echo "ERROR: Neither sqlite3 nor python3 found to restore SQLite database." >&2
            exit 1
        fi
        echo "Database restore completed successfully."
    elif [ "${ALLOW_MOCK_RESTORE}" -eq 1 ]; then
        echo "psql not found in environment, performing mock database restore..."
        RESTORE_TARGET_DIR="${RESTORE_MOCK_DIR:-${UPLOADS_DIR}}"
        mkdir -p "${RESTORE_TARGET_DIR}"
        gunzip -c "${DB_BACKUP_FILE}" > "${RESTORE_TARGET_DIR}/restored_db_dump.sql"
        echo "Mock database restore completed."
    else
        echo "ERROR: psql or sqlite3 utility not found and ALLOW_MOCK_RESTORE is not enabled." >&2
        exit 1
    fi
else
    echo "ERROR: Database backup archive '${DB_BACKUP_FILE}' not found!" >&2
    exit 1
fi

# 2. Restore File Uploads
if [ -n "${UPLOADS_BACKUP_FILE}" ] && [ -f "${UPLOADS_BACKUP_FILE}" ]; then
    echo "Restoring file uploads from ${UPLOADS_BACKUP_FILE} into ${UPLOADS_DIR}..."
    mkdir -p "${UPLOADS_DIR}"
    tar -xzf "${UPLOADS_BACKUP_FILE}" -C "${UPLOADS_DIR}"
    echo "File uploads restore completed successfully."
else
    echo "ERROR: Uploads backup archive '${UPLOADS_BACKUP_FILE}' not found!" >&2
    exit 1
fi

echo "=== Restore completed successfully at $(date) ==="
