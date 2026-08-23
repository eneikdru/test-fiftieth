#!/usr/bin/env bash
set -uo pipefail

BLOCKER_FILE="docs/human-reconciliation-blocker.md"

if [ ! -f "$BLOCKER_FILE" ]; then
    echo "[ERROR] Blocker documentation file not found at $BLOCKER_FILE"
    exit 1
fi

if ! grep -q "fd6672c6-02c4-455e-a4d9-91e4ae9d308c" "$BLOCKER_FILE"; then
    echo "[ERROR] Blocker documentation is missing subject ID fd6672c6-02c4-455e-a4d9-91e4ae9d308c"
    exit 1
fi

if ! grep -q "765d2ab0-1b55-4701-babd-af5247442de5" "$BLOCKER_FILE"; then
    echo "[ERROR] Blocker documentation is missing subject ID 765d2ab0-1b55-4701-babd-af5247442de5"
    exit 1
fi

echo "Blocker documentation verified."
