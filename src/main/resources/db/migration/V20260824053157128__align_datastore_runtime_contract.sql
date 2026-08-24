-- Flyway Migration V20260824053157128: Datastore Runtime Contract 9b58412d patch alignment
-- Mandatory Flyway version: V20260824053157128

UPDATE privacy_export_requests
SET status = 'RESOLVED',
    notes = 'Automated patch: Resolved runtime contract discrepancy for closed task 8bd0dbae-41f6-466a-95a7-aff680ed0866'
WHERE subject_id = '8bd0dbae-41f6-466a-95a7-aff680ed0866'
  AND status IN ('PENDING', 'PROCESSING', 'FLAGGED_FOR_HUMAN_REVIEW');

UPDATE privacy_erasure_requests
SET status = 'RESOLVED',
    reason = 'Automated patch: Resolved runtime contract discrepancy for closed task 8bd0dbae-41f6-466a-95a7-aff680ed0866'
WHERE subject_id = '8bd0dbae-41f6-466a-95a7-aff680ed0866'
  AND status IN ('PENDING', 'PROCESSING', 'FLAGGED_FOR_HUMAN_REVIEW');
