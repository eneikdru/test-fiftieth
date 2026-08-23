# Follow-up Blocker Artifact: Human Reconciliation for Orchestrator

**Stuck Subjects Scope:**
- Orchestrator Subject ID: `fd6672c6-02c4-455e-a4d9-91e4ae9d308c`
- Orchestrator Subject ID: `765d2ab0-1b55-4701-babd-af5247442de5`

**Nature of the Blocker:**
This explicitly documents that an atomically-guarded database `UPDATE` executed within this Spring Boot repository against the `privacy_export_requests` and `privacy_erasure_requests` tables **is a NO-OP** against the external orchestrator state.
Because the orchestrator maintains its state outside this application database, the stuck subjects `fd6672c6` and `765d2ab0` CANNOT be unblocked by our Flyway migrations.

**Required Action:**
A human operator or the orchestrator administrator must directly perform the state reconciliation externally in the orchestrator's own database to move these subjects out of the stuck or orphaned state.
