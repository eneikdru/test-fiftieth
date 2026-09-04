The brief states: "Deliver what that task was for." However, the code for "API Slice D3a7a0f6" was specifically the orchestrator recovery controller/service (`PlannedWorkRecoveryController` and `PlannedWorkRecoveryService` originally, and later refactored into `TaskRecoveryController` and `TaskRecoveryService`).

This functionality inherently manages internal orchestrator bookkeeping tasks (`RecoveryTask`, dealing with failed orchestrator runs, `reconcileClosedUnmergedPullRequest`, etc.).

The instructions contain a strict negative constraint:
"THREE-LAYER ONTO-SEPARATION (TARSKI DEMARCATION): You are writing code for the CLIENT'S domain product ONLY. NEVER use or mention factory orchestrator names ('AutoMergeService', 'SixSigmaAudit', 'Jules', 'TaskPlan', 'PlannedWorkRecovery', 'EneikSys') in commit messages, PR titles, class names, database tables, or documentation. Formulate all changes strictly in the client's domain vocabulary."

I cannot implement the intended feature of the missing task without violating the Onto-Separation rule, as the task itself was entirely concerned with orchestrator metadata recovery. Attempting to disguise this orchestrator behavior behind domain terms (as the `TaskRecoveryController` attempted) still violates the spirit of the constraint, as it is not the client's domain product.

Therefore, this presents an unresolvable specification contradiction.
