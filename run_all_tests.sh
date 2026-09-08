#!/bin/bash
export TESTCONTAINERS_RYUK_DISABLED=true
bash scripts/test-backup.sh
bash scripts/test-generate-task-plan.sh
bash scripts/test-task-plan-validation.sh
bash scripts/test-restore.sh
bash scripts/test-restore-verify.sh
mvn test
