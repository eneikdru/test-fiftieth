#!/usr/bin/env bash
set -euo pipefail

TEST_DIR="$(pwd)/tmp_test_restore"
ORIGINAL_UPLOADS="${TEST_DIR}/original_uploads"
BACKUP_DIR="${TEST_DIR}/backups"
RESTORED_UPLOADS="${TEST_DIR}/restored_uploads"
RESTORED_DB_DIR="${TEST_DIR}/restored_db"

echo "=== Running Backup Restore Verification Test ==="

# Cleanup any leftover test directory
rm -rf "${TEST_DIR}"
mkdir -p "${ORIGINAL_UPLOADS}" "${BACKUP_DIR}" "${RESTORED_UPLOADS}" "${RESTORED_DB_DIR}"

# 1. Prepare initial dataset at time of backup
echo "Preparing original dataset for backup..."
DOC1_NAME="protocol_2026_01.pdf"
DOC1_CONTENT="Epidemiological Outbreak Investigation Protocol 2026 - Confidential"
DOC2_NAME="report_surveillance.docx"
DOC2_CONTENT="Surveillance Report Data 2026 - Institute of Epidemiology"

echo "${DOC1_CONTENT}" > "${ORIGINAL_UPLOADS}/${DOC1_NAME}"
echo "${DOC2_CONTENT}" > "${ORIGINAL_UPLOADS}/${DOC2_NAME}"

# 2. Run Backup to create recent backup archive
echo "Executing backup script..."
ALLOW_MOCK_BACKUP=1 \
UPLOADS_DIR="${ORIGINAL_UPLOADS}" \
BACKUP_DIR="${BACKUP_DIR}" \
BACKUP_RETENTION_DAYS=7 \
OVERRIDE_TIMESTAMP="20260822_150000" \
bash scripts/backup.sh

DB_BACKUP_FILE="${BACKUP_DIR}/db_epidemiology_db_20260822_150000.sql.gz"
UPLOADS_BACKUP_FILE="${BACKUP_DIR}/uploads_20260822_150000.tar.gz"

if [ ! -f "${DB_BACKUP_FILE}" ] || [ ! -f "${UPLOADS_BACKUP_FILE}" ]; then
    echo "ERROR: Required backup files were not created!" >&2
    exit 1
fi

echo "Verified: Backup archives generated successfully."

# 3. Provision fresh environment (Simulate disaster recovery / fresh environment)
echo "Provisioning fresh target environment..."
# Ensure target directories are completely clean/empty
rm -rf "${RESTORED_UPLOADS}" "${RESTORED_DB_DIR}"
mkdir -p "${RESTORED_UPLOADS}" "${RESTORED_DB_DIR}"

# Verify fresh environment is empty
if [ "$(ls -A "${RESTORED_UPLOADS}")" ]; then
    echo "ERROR: Target uploads environment is not empty!" >&2
    exit 1
fi

# 4. Execute Restore process into fresh environment
echo "Executing restore script..."
ALLOW_MOCK_RESTORE=1 \
RESTORE_MOCK_DIR="${RESTORED_DB_DIR}" \
UPLOADS_DIR="${RESTORED_UPLOADS}" \
BACKUP_DIR="${BACKUP_DIR}" \
DB_BACKUP_FILE="${DB_BACKUP_FILE}" \
UPLOADS_BACKUP_FILE="${UPLOADS_BACKUP_FILE}" \
bash scripts/restore.sh

# 5. Assert data state matches state at time of backup
echo "Verifying restored data integrity..."

# Check restored documents exist
if [ ! -f "${RESTORED_UPLOADS}/${DOC1_NAME}" ] || [ ! -f "${RESTORED_UPLOADS}/${DOC2_NAME}" ]; then
    echo "ERROR: Restored files missing in target uploads directory!" >&2
    exit 1
fi

# Check document content exact match
RESTORED_DOC1_CONTENT="$(cat "${RESTORED_UPLOADS}/${DOC1_NAME}")"
RESTORED_DOC2_CONTENT="$(cat "${RESTORED_UPLOADS}/${DOC2_NAME}")"

if [ "${RESTORED_DOC1_CONTENT}" != "${DOC1_CONTENT}" ]; then
    echo "ERROR: Restored ${DOC1_NAME} content does not match original state!" >&2
    exit 1
fi

if [ "${RESTORED_DOC2_CONTENT}" != "${DOC2_CONTENT}" ]; then
    echo "ERROR: Restored ${DOC2_NAME} content does not match original state!" >&2
    exit 1
fi

# Verify database restore file in mock target
if [ ! -f "${RESTORED_DB_DIR}/restored_db_dump.sql" ]; then
    echo "ERROR: Restored database dump not found in fresh environment target!" >&2
    exit 1
fi

echo "Verified: Restored file uploads match original backup state."
echo "Verified: Restored database dump present and valid."

# Clean up temporary test artifacts
rm -rf "${TEST_DIR}"

echo "SUCCESS: Verified backup restore test passed completely!"
