#!/usr/bin/env bash
set -euo pipefail

TEST_DIR="$(pwd)/tmp_test_restore"
ORIGINAL_UPLOADS="${TEST_DIR}/original_uploads"
ORIGINAL_DB_DIR="${TEST_DIR}/original_db"
ORIGINAL_DB="${ORIGINAL_DB_DIR}/epidemiology.db"
BACKUP_DIR="${TEST_DIR}/backups"
RESTORED_UPLOADS="${TEST_DIR}/restored_uploads"
RESTORED_DB_DIR="${TEST_DIR}/restored_db"
RESTORED_DB="${RESTORED_DB_DIR}/restored_epidemiology.db"

echo "=== Running Backup Restore Verification Test ==="

# Cleanup any leftover test directory
rm -rf "${TEST_DIR}"
mkdir -p "${ORIGINAL_UPLOADS}" "${ORIGINAL_DB_DIR}" "${BACKUP_DIR}" "${RESTORED_UPLOADS}" "${RESTORED_DB_DIR}"

# 1. Prepare initial dataset at time of backup (File uploads + Database records)
echo "Preparing original dataset for backup..."
DOC1_NAME="protocol_2026_01.pdf"
DOC1_CONTENT="Epidemiological Outbreak Investigation Protocol 2026 - Confidential"
DOC2_NAME="report_surveillance.docx"
DOC2_CONTENT="Surveillance Report Data 2026 - Institute of Epidemiology"

echo "${DOC1_CONTENT}" > "${ORIGINAL_UPLOADS}/${DOC1_NAME}"
echo "${DOC2_CONTENT}" > "${ORIGINAL_UPLOADS}/${DOC2_NAME}"

# Populate original database with schema and sample records
sqlite3 "${ORIGINAL_DB}" <<'EOF'
CREATE TABLE patients (
    id INTEGER PRIMARY KEY,
    patient_code TEXT NOT NULL,
    diagnosis TEXT NOT NULL,
    status TEXT NOT NULL
);
INSERT INTO patients (id, patient_code, diagnosis, status) VALUES (1, 'PAT-001', 'Epidemic Typhus', 'CONFIRMED');
INSERT INTO patients (id, patient_code, diagnosis, status) VALUES (2, 'PAT-002', 'Seasonal Influenza', 'RECOVERED');

CREATE TABLE outbreak_cases (
    id INTEGER PRIMARY KEY,
    disease TEXT NOT NULL,
    case_count INTEGER NOT NULL
);
INSERT INTO outbreak_cases (id, disease, case_count) VALUES (101, 'Influenza A', 42);
EOF

echo "Original database populated successfully."

# 2. Run Backup to create recent backup archive
echo "Executing backup script..."
SQLITE_DB_PATH="${ORIGINAL_DB}" \
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

# 3. Provision fresh target environment
echo "Provisioning fresh target environment..."
rm -rf "${RESTORED_UPLOADS}" "${RESTORED_DB_DIR}"
mkdir -p "${RESTORED_UPLOADS}" "${RESTORED_DB_DIR}"

# Verify fresh environment is clean
if [ "$(ls -A "${RESTORED_UPLOADS}")" ]; then
    echo "ERROR: Target uploads environment is not empty!" >&2
    exit 1
fi

if [ -f "${RESTORED_DB}" ]; then
    echo "ERROR: Target database file already exists in fresh environment!" >&2
    exit 1
fi

# 4. Execute Restore process into fresh environment (without bypassing database)
echo "Executing restore script..."
SQLITE_DB_PATH="${RESTORED_DB}" \
UPLOADS_DIR="${RESTORED_UPLOADS}" \
BACKUP_DIR="${BACKUP_DIR}" \
DB_BACKUP_FILE="${DB_BACKUP_FILE}" \
UPLOADS_BACKUP_FILE="${UPLOADS_BACKUP_FILE}" \
bash scripts/restore.sh

# 5. Assert data state matches state at time of backup via real database queries
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

echo "Verified: Restored file uploads match original backup state."

# Verify restored database via SQL queries against the restored environment
if [ ! -f "${RESTORED_DB}" ]; then
    echo "ERROR: Restored database file missing in fresh environment!" >&2
    exit 1
fi

PATIENT_COUNT="$(sqlite3 "${RESTORED_DB}" "SELECT COUNT(*) FROM patients;")"
PATIENT_1_DIAG="$(sqlite3 "${RESTORED_DB}" "SELECT diagnosis FROM patients WHERE id=1;")"
PATIENT_2_STATUS="$(sqlite3 "${RESTORED_DB}" "SELECT status FROM patients WHERE id=2;")"
CASE_COUNT="$(sqlite3 "${RESTORED_DB}" "SELECT case_count FROM outbreak_cases WHERE id=101;")"

if [ "${PATIENT_COUNT}" -ne 2 ]; then
    echo "ERROR: Restored patient count (${PATIENT_COUNT}) does not match original state (2)!" >&2
    exit 1
fi

if [ "${PATIENT_1_DIAG}" != "Epidemic Typhus" ]; then
    echo "ERROR: Restored patient 1 diagnosis ('${PATIENT_1_DIAG}') does not match original state ('Epidemic Typhus')!" >&2
    exit 1
fi

if [ "${PATIENT_2_STATUS}" != "RECOVERED" ]; then
    echo "ERROR: Restored patient 2 status ('${PATIENT_2_STATUS}') does not match original state ('RECOVERED')!" >&2
    exit 1
fi

if [ "${CASE_COUNT}" -ne 42 ]; then
    echo "ERROR: Restored outbreak case count (${CASE_COUNT}) does not match original state (42)!" >&2
    exit 1
fi

echo "Verified: Restored database queried successfully, data genuinely matches pre-backup state."

# Clean up temporary test artifacts
rm -rf "${TEST_DIR}"

echo "SUCCESS: Verified backup restore test passed completely!"
