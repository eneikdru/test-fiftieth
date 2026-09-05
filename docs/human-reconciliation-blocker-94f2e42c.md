# Schema Migration Blocker: Workflow Duration Telemetry

The required schema modifications for tracking workflow duration (the addition of `workflow_duration_ms`, `start_time`, and `end_time` columns to the `telemetry_events` table) are already present in the primary data repository and assigned to the strictly mandated schema version (`V20260828040731769`).

Attempting to recreate this schema version or apply these columns again will result in a database initialization failure due to version collisions and duplicate column definitions. Furthermore, the accompanying data model (`TelemetryEvent.java`) has already been fully updated to support these fields.

Because the data and schema layer changes for this requirement are already fulfilled, and implementing the application-layer logic to populate these fields during dossier generation strictly falls under the application backend scope rather than data infrastructure, no further safe modifications can be made within the current data scope. The system requires a follow-up task routed to the application services team to integrate with the existing schema.
