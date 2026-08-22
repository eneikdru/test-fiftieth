#!/usr/bin/env bash
set -euo pipefail

TEST_DIR="$(pwd)/tmp_test_backup"
TEST_UPLOADS="${TEST_DIR}/uploads"
TEST_BACKUPS="${TEST_DIR}/backups"

echo "=== Running Backup Verification Test ==="

# Cleanup any leftover test files
rm -rf "${TEST_DIR}"
mkdir -p "${TEST_UPLOADS}" "${TEST_BACKUPS}"

# 1. Prepare sample upload files
echo "Creating sample document files..."
echo "Sample protocol document content" > "${TEST_UPLOADS}/protocol_01.pdf"
echo "Sample outbreak report content" > "${TEST_UPLOADS}/report_01.docx"

# 2. Test Backup Execution
echo "Executing backup script..."
ALLOW_MOCK_BACKUP=1 \
UPLOADS_DIR="${TEST_UPLOADS}" \
BACKUP_DIR="${TEST_BACKUPS}" \
BACKUP_RETENTION_DAYS=3 \
OVERRIDE_TIMESTAMP="20260822_120000" \
bash scripts/backup.sh

# Verify output archives exist
DB_FILE="${TEST_BACKUPS}/db_epidemiology_db_20260822_120000.sql.gz"
UPLOADS_FILE="${TEST_BACKUPS}/uploads_20260822_120000.tar.gz"

if [ ! -f "${DB_FILE}" ]; then
    echo "ERROR: Database backup file ${DB_FILE} was not created!" >&2
    exit 1
fi

if [ ! -f "${UPLOADS_FILE}" ]; then
    echo "ERROR: Uploads backup file ${UPLOADS_FILE} was not created!" >&2
    exit 1
fi

echo "Verified: DB backup archive created (${DB_FILE})"
echo "Verified: Uploads backup archive created (${UPLOADS_FILE})"

# 3. Test Retention Policy Purging
echo "Testing retention policy logic..."

# Create old dummy backups
OLD_DB_FILE="${TEST_BACKUPS}/db_epidemiology_db_20260801_100000.sql.gz"
OLD_UPLOADS_FILE="${TEST_BACKUPS}/uploads_20260801_100000.tar.gz"
touch "${OLD_DB_FILE}" "${OLD_UPLOADS_FILE}"

# Set modification time of old files to 10 days ago using touch -d or perl/python
if command -v python3 &> /dev/null; then
    python3 -c "import os, time; ten_days_ago = time.time() - (10 * 86400); os.utime('${OLD_DB_FILE}', (ten_days_ago, ten_days_ago)); os.utime('${OLD_UPLOADS_FILE}', (ten_days_ago, ten_days_ago))"
else
    touch -d "10 days ago" "${OLD_DB_FILE}" "${OLD_UPLOADS_FILE}" || true
fi

echo "Triggering backup script with RETENTION_DAYS=3..."
ALLOW_MOCK_BACKUP=1 \
UPLOADS_DIR="${TEST_UPLOADS}" \
BACKUP_DIR="${TEST_BACKUPS}" \
BACKUP_RETENTION_DAYS=3 \
OVERRIDE_TIMESTAMP="20260822_120500" \
bash scripts/backup.sh

# Verify old backups were deleted
if [ -f "${OLD_DB_FILE}" ] || [ -f "${OLD_UPLOADS_FILE}" ]; then
    echo "ERROR: Old backup files were not purged by retention policy!" >&2
    exit 1
fi

# Verify recent backups still exist
if [ ! -f "${DB_FILE}" ] || [ ! -f "${UPLOADS_FILE}" ]; then
    echo "ERROR: Recent backup files were wrongly deleted!" >&2
    exit 1
fi

echo "Verified: Old backups (>3 days) purged, recent backups retained."

# Clean up test artifacts
rm -rf "${TEST_DIR}"

echo "SUCCESS: Backup verification test passed completely!"
