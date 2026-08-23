#!/bin/bash
# Verify the human reconciliation blocker exists
if [ ! -f "docs/human-reconciliation-blocker.md" ] && [ ! -f "docs/human-reconciliation-blocker-followup.md" ]; then
  echo "[ERROR] Blocker artifact explicitly stating the subject fd6672c6-02c4-455e-a4d9-91e4ae9d308c and 765d2ab0-1b55-4701-babd-af5247442de5 is missing!"
  exit 1
fi
echo "Blocker artifact found."
exit 0
