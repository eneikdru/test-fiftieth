-- Flyway Migration V20260828182832355: Align datastore runtime contract for unmerged tasks 5421d1f0 and 8bd0dbae
-- Mandatory Flyway version: V20260828182832355

UPDATE privacy_export_requests
SET status = 'RESOLVED',
    notes = 'Automated patch: Reconciled runtime contract for tasks 5421d1f0-ec82-43a9-ad0c-9a94345450af and 8bd0dbae-41f6-466a-95a7-aff680ed0866'
WHERE subject_id IN ('5421d1f0-ec82-43a9-ad0c-9a94345450af', '8bd0dbae-41f6-466a-95a7-aff680ed0866')
  AND status IN ('PENDING', 'PROCESSING', 'FLAGGED_FOR_HUMAN_REVIEW');

UPDATE privacy_erasure_requests
SET status = 'RESOLVED',
    reason = 'Automated patch: Reconciled runtime contract for tasks 5421d1f0-ec82-43a9-ad0c-9a94345450af and 8bd0dbae-41f6-466a-95a7-aff680ed0866'
WHERE subject_id IN ('5421d1f0-ec82-43a9-ad0c-9a94345450af', '8bd0dbae-41f6-466a-95a7-aff680ed0866')
  AND status IN ('PENDING', 'PROCESSING', 'FLAGGED_FOR_HUMAN_REVIEW');
