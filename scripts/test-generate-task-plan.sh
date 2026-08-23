#!/usr/bin/env bash
set -euo pipefail

echo "=== Running Generate Task Plan Write Error Test ==="

TEST_DIR="$(pwd)/tmp_test_generate"
rm -rf "${TEST_DIR}"
mkdir -p "${TEST_DIR}"

VALID_OUTPUT="${TEST_DIR}/output.json"
INVALID_OUTPUT="/invalid_read_only_dir_999/out.json"

INPUT_FILE_VALID="${TEST_DIR}/input_valid.json"
cat << JSON_EOF > "${INPUT_FILE_VALID}"
{
  "test": "data",
  "outputPath": "'${VALID_OUTPUT}'"
}
JSON_EOF
sed -i "s|'${VALID_OUTPUT}'|${VALID_OUTPUT}|g" "${INPUT_FILE_VALID}"

INPUT_FILE_INVALID="${TEST_DIR}/input_invalid.json"
cat << JSON_EOF > "${INPUT_FILE_INVALID}"
{
  "test": "data",
  "outputPath": "'${INVALID_OUTPUT}'"
}
JSON_EOF
sed -i "s|'${INVALID_OUTPUT}'|${INVALID_OUTPUT}|g" "${INPUT_FILE_INVALID}"

MALFORMED_OUTPUT="${TEST_DIR}/malformed_output.json"
INPUT_FILE_MALFORMED="${TEST_DIR}/input_malformed.json"
cat << 'JSON_EOF' > "${INPUT_FILE_MALFORMED}"
```json
{
  "test": "malformed_data",
  "outputPath": "MALFORMED_OUTPUT_PLACEHOLDER"
}
```
JSON_EOF
sed -i "s|MALFORMED_OUTPUT_PLACEHOLDER|${MALFORMED_OUTPUT}|g" "${INPUT_FILE_MALFORMED}"
sed -i "s|'${MALFORMED_OUTPUT}'|${MALFORMED_OUTPUT}|g" "${INPUT_FILE_MALFORMED}"

echo "1. Testing successful write..."
python3 scripts/generate-task-plan.py "${INPUT_FILE_VALID}"
if [ ! -f "${VALID_OUTPUT}" ]; then
    echo "FAIL: Expected file ${VALID_OUTPUT} was not written!" >&2
    exit 1
fi
echo "PASS: Successfully wrote to valid output path."

echo "2. Testing successful write from malformed payload..."
python3 scripts/generate-task-plan.py "${INPUT_FILE_MALFORMED}"
if [ ! -f "${MALFORMED_OUTPUT}" ]; then
    echo "FAIL: Expected file ${MALFORMED_OUTPUT} was not written from malformed input!" >&2
    exit 1
fi
echo "PASS: Successfully repaired and wrote to valid output path."

echo "3. Testing write to invalid directory (simulating write error)..."
set +e
OUTPUT=$(python3 scripts/generate-task-plan.py "${INPUT_FILE_INVALID}" 2>&1)
EXIT_CODE=$?
set -e

if [ $EXIT_CODE -ne 0 ]; then
    echo "FAIL: Script did not exit cleanly (exit code $EXIT_CODE)." >&2
    exit 1
fi

if echo "$OUTPUT" | grep -qi "write error"; then
    echo "PASS: Exited cleanly and logged write error."
else
    echo "FAIL: Did not log write error. Output was:" >&2
    echo "$OUTPUT" >&2
    exit 1
fi

rm -rf "${TEST_DIR}"
echo "SUCCESS: All generation script tests passed!"
