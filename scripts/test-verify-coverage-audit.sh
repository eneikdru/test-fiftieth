#!/usr/bin/env bash
set -euo pipefail

echo "=== Running Coverage Audit Pipeline Verification Test Suite ==="

TEST_DIR="$(pwd)/tmp_test_coverage_audit"
rm -rf "${TEST_DIR}"
mkdir -p "${TEST_DIR}"

PAYLOAD_FILE="${TEST_DIR}/payload.json"
REPORT_FILE="${TEST_DIR}/report.json"

cat << EOF > "${PAYLOAD_FILE}"
{
  "reportPath": "${REPORT_FILE}"
}
EOF

# 1. Valid report
cat << 'EOF' > "${REPORT_FILE}"
{
  "requirements": [
    { "id": "REQ-1", "description": "Department access control", "status": "VERIFIED" }
  ],
  "gaps": [
    { "id": "GAP-1", "evidence": "Course-based access control under review" }
  ]
}
EOF

echo "1. Testing valid coverage audit report..."
if bash scripts/verify-coverage-audit.sh "${PAYLOAD_FILE}"; then
    echo "PASS: Valid coverage audit report passed verification."
else
    echo "FAIL: Valid coverage audit report failed verification!" >&2
    rm -rf "${TEST_DIR}"
    exit 1
fi

# 2. Report missing requirements section -> must fail (exit 1)
cat << 'EOF' > "${REPORT_FILE}"
{
  "gaps": []
}
EOF

echo "2. Testing report missing requirements section..."
if bash scripts/verify-coverage-audit.sh "${PAYLOAD_FILE}" 2>/dev/null; then
    echo "FAIL: Report missing requirements section should have exited with status 1!" >&2
    rm -rf "${TEST_DIR}"
    exit 1
else
    echo "PASS: Report missing requirements section correctly exited with status 1."
fi

# 3. Report with gaps missing evidence -> must fail (exit 1)
cat << 'EOF' > "${REPORT_FILE}"
{
  "requirements": [
    { "id": "REQ-1", "description": "Department access control", "status": "VERIFIED" }
  ],
  "gaps": [
    { "id": "GAP-1" }
  ]
}
EOF

echo "3. Testing report with gap missing evidence field..."
if bash scripts/verify-coverage-audit.sh "${PAYLOAD_FILE}" 2>/dev/null; then
    echo "FAIL: Report with gap missing evidence field should have exited with status 1!" >&2
    rm -rf "${TEST_DIR}"
    exit 1
else
    echo "PASS: Report with gap missing evidence field correctly exited with status 1."
fi

# Cleanup test directory
rm -rf "${TEST_DIR}"

echo "SUCCESS: All coverage audit pipeline verification tests passed!"
