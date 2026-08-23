#!/usr/bin/env bash
set -euo pipefail

echo "=== Running Task Plan Validation & Linkage Test Suite ==="

TEST_DIR="$(pwd)/tmp_test_task_plan"
TEST_ENEIK_DIR="${TEST_DIR}/.eneik"
TEST_DOCS_DIR="${TEST_DIR}/docs"

# Cleanup any previous temporary test directory
rm -rf "${TEST_DIR}"
mkdir -p "${TEST_ENEIK_DIR}" "${TEST_DOCS_DIR}"

PAYLOAD_FILE="${TEST_DIR}/payload.json"
cat << 'EOF' > "${PAYLOAD_FILE}"
{
  "wishlistIds": [
    "ffedab63-db2c-43f1-8323-e025b511e0cc",
    "f8760c2b-a6b6-40c5-80df-3fcd60733bc4"
  ]
}
EOF

VALID_ENEIK_PLAN="${TEST_ENEIK_DIR}/task-plan.json"
cat << 'EOF' > "${VALID_ENEIK_PLAN}"
{
  "taskId": "e0bf2baf",
  "role": "BARCAN-TAG-05",
  "sourceIndex": {
    "wishlistIds": [
      "ffedab63-db2c-43f1-8323-e025b511e0cc",
      "f8760c2b-a6b6-40c5-80df-3fcd60733bc4"
    ]
  },
  "taskPlan": {
    "slices": [
      {
        "role": "BARCAN-TAG-05",
        "description": "Fix missing delivery plan validation criteria"
      }
    ]
  }
}
EOF

DOCS_PLAN="${TEST_DOCS_DIR}/task-plan.json"
cp "${VALID_ENEIK_PLAN}" "${DOCS_PLAN}"

MISMATCHED_PLAN="${TEST_ENEIK_DIR}/mismatched-plan.json"
cat << 'EOF' > "${MISMATCHED_PLAN}"
{
  "taskId": "e0bf2baf",
  "role": "BARCAN-TAG-05",
  "sourceIndex": {
    "wishlistIds": [
      "invalid-wishlist-id-99999"
    ]
  }
}
EOF

echo "1. Testing valid task plan saved in .eneik directory with payload linkage..."
if python3 scripts/validate-task-plan.py "${VALID_ENEIK_PLAN}" --payload "${PAYLOAD_FILE}"; then
    echo "PASS: Valid task plan in .eneik/ directory passed validation."
else
    echo "FAIL: Valid task plan in .eneik/ directory failed validation!" >&2
    rm -rf "${TEST_DIR}"
    exit 1
fi

echo "2. Testing invalid task plan saved in docs directory..."
if python3 scripts/validate-task-plan.py "${DOCS_PLAN}" --payload "${PAYLOAD_FILE}" 2>/dev/null; then
    echo "FAIL: Task plan saved in docs/ directory should have been rejected!" >&2
    rm -rf "${TEST_DIR}"
    exit 1
else
    echo "PASS: Task plan saved in docs/ directory was correctly rejected."
fi

echo "3. Testing task plan with mismatched sourceIndex wishlist ID..."
if python3 scripts/validate-task-plan.py "${MISMATCHED_PLAN}" --payload "${PAYLOAD_FILE}" 2>/dev/null; then
    echo "FAIL: Mismatched sourceIndex wishlist ID should have been rejected!" >&2
    rm -rf "${TEST_DIR}"
    exit 1
else
    echo "PASS: Task plan with mismatched sourceIndex wishlist ID was correctly rejected."
fi

# Cleanup test directory
rm -rf "${TEST_DIR}"

echo "SUCCESS: All task plan validation and linkage tests passed!"
