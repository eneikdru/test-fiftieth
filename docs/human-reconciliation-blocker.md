# Human Reconciliation Blocker Record

## Blocker Context
Wishlist items / orchestrator subjects `fd6672c6-02c4-455e-a4d9-91e4ae9d308c` (stuck in retired state from iteration-admission poka-yoke failure) and `765d2ab0-1b55-4701-babd-af5247442de5` (stuck with orphaned dependency `168a6edf-53fa-48b4-8226-9fcd0528efd2`) represent orchestrator wishlist item identifiers and audit-subject identifiers, rather than product end-user data subject identifiers stored in the product application database (`privacy_export_requests` and `privacy_erasure_requests`).

## Root Cause Analysis
1. **Target Schema Misalignment**: The product application schema (`privacy_export_requests` and `privacy_erasure_requests`) stores application-level user privacy export/erasure job records. The subjects `fd6672c6-02c4-455e-a4d9-91e4ae9d308c` and `765d2ab0-1b55-4701-babd-af5247442de5` do not exist as rows in these application privacy tables.
2. **Orchestrator Control Plane Boundary**: The orchestrator's state store (wishlist graph, task queue, iteration admission controller) maintains its own separate database/tables outside of this Spring Boot application repository.
3. **Execution Safety**: Executing an UPDATE query against `privacy_export_requests` or `privacy_erasure_requests` for those IDs is a no-op against application data and cannot unblock the orchestrator's internal workflow state.

## Required Human Reconciliation Action
A human operator / orchestrator administrator must directly perform reconciliation in the external orchestrator system:
1. Manually update or clear the status of wishlist subject `fd6672c6-02c4-455e-a4d9-91e4ae9d308c` in the orchestrator task/wishlist store to clear the retired/poka-yoke state.
2. Manually resolve or unlink orphaned dependency `168a6edf-53fa-48b4-8226-9fcd0528efd2` for subject `765d2ab0-1b55-4701-babd-af5247442de5` in the orchestrator task/wishlist store.
3. Resume automated task compilation once the orchestrator state store is reconciled.
