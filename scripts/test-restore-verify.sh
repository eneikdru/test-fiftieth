#!/usr/bin/env bash
set -euo pipefail

# This script wraps test-restore.sh and acts as the verification command.
echo "Running restore verification tests..."
bash scripts/test-restore.sh

# Verify the offsite sync mechanism manually
TEST_DIR="$(pwd)/tmp_test_offsite"
mkdir -p "${TEST_DIR}/backups"
mkdir -p "${TEST_DIR}/offsite"
mkdir -p "${TEST_DIR}/uploads"

echo "Verifying backup offsite synchronization..."
ALLOW_MOCK_BACKUP=1 UPLOADS_DIR="${TEST_DIR}/uploads" BACKUP_DIR="${TEST_DIR}/backups" OFFSITE_BACKUP_DIR="${TEST_DIR}/offsite" BACKUP_RETENTION_DAYS=7 OVERRIDE_TIMESTAMP="20260822_150000" bash scripts/backup.sh > /dev/null

if ls "${TEST_DIR}/offsite"/db_*.sql.gz 1> /dev/null 2>&1; then
    echo "DB backup found offsite."
else
    echo "ERROR: Offsite DB backup sync failed."
    kill -TERM $$
fi

rm -rf "${TEST_DIR}/backups"
mkdir -p "${TEST_DIR}/backups"

echo "Verifying restore from offsite sync..."
ALLOW_MOCK_RESTORE=1 UPLOADS_DIR="${TEST_DIR}/uploads" BACKUP_DIR="${TEST_DIR}/backups" OFFSITE_BACKUP_DIR="${TEST_DIR}/offsite" bash scripts/restore.sh > /dev/null

if ls "${TEST_DIR}/backups"/db_*.sql.gz 1> /dev/null 2>&1; then
    echo "DB backup pulled from offsite successfully."
else
    echo "ERROR: Restore failed to pull from offsite directory."
    kill -TERM $$
fi

rm -rf "${TEST_DIR}"

echo "SUCCESS: Backup and Restore successfully validated."
