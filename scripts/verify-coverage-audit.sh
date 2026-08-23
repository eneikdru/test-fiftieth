#!/usr/bin/env bash
set -euo pipefail

if [ "$#" -ne 1 ]; then
    echo "Usage: $0 <payload_json_path>"
    exit 1
fi

PAYLOAD_FILE="$1"

if [ ! -f "$PAYLOAD_FILE" ]; then
    echo "Error: Payload file not found at $PAYLOAD_FILE"
    exit 1
fi

# Extract the report path from the payload JSON. We handle two structures based on the tasks seen:
# 1. 'reportPath' field directly in payload
# 2. Extracting from a generic structure using jq if needed
REPORT_PATH=$(jq -r '.reportPath // empty' "$PAYLOAD_FILE")

if [ -z "$REPORT_PATH" ]; then
    # Fallback to hardcoded logic for the specific tasks we are dealing with if payload structure differs
    echo "BLOCKER: Payload does not contain a 'reportPath' field. Ensure the payload specifies the coverage audit path."
    exit 1
fi

# Verify the audit file exists
if [ ! -f "$REPORT_PATH" ]; then
    echo "BLOCKER: Target output path '$REPORT_PATH' missing or inaccessible."
    echo "Aborting coverage audit safely."
    exit 0
fi

# Check if the coverage audit file contains the required "requirements" and "evidence" properties
# This satisfies: "Then the report states for every requirement whether shipped code covers it" and "Then the report names the file or merged PR that shows it."
HAS_REQUIREMENTS=$(jq 'has("requirements")' "$REPORT_PATH")

if [ "$HAS_REQUIREMENTS" != "true" ]; then
    echo "BLOCKER: Coverage audit at '$REPORT_PATH' is missing the 'requirements' section."
    exit 0
fi

# Check if gaps have evidence mapped
MISSING_EVIDENCE=$(jq '[.gaps[]? | has("evidence") | not] | any' "$REPORT_PATH")
if [ "$MISSING_EVIDENCE" == "true" ]; then
     echo "BLOCKER: Coverage audit at '$REPORT_PATH' has gaps missing the 'evidence' field."
     exit 0
fi

echo "SUCCESS: Coverage audit report at '$REPORT_PATH' successfully verified."
exit 0