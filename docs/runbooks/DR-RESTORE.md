# Disaster Recovery - Restore Procedure

This runbook documents the VERIFIED procedure for restoring the `epidemiology_db` database and associated document uploads from the automated backup archives.

## 1. Locate the latest backup archives

The backups are securely synchronized off-site. For the local recovery process, locate the corresponding `.sql.gz` and `.tar.gz` files from the same backup run timestamp in the designated storage (`/offsite_backups`).

## 2. Execute the restore process

Use the `restore.sh` script to perform the recovery against a fresh environment. The script will automatically retrieve missing files from the off-site storage.

### Example Restore Command

```bash
docker compose run --rm -e OFFSITE_BACKUP_DIR=/offsite_backups backup /usr/local/bin/restore.sh
```

## 3. Verify Restoration

Once the script completes, verify that the application has recovered:
1. Ensure the backend container is healthy and responding to requests.
2. Spot-check the database to ensure expected records exist.
3. Verify that uploaded documents are accessible.
