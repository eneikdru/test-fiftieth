package com.eneik.epidemiology.privacy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

@Service
public class TaskRecoveryService {

    private final RecoveryTaskRepository recoveryTaskRepository;
    private final Clock clock;

    @Autowired
    public TaskRecoveryService(RecoveryTaskRepository recoveryTaskRepository) {
        this(recoveryTaskRepository, Clock.systemUTC());
    }

    public TaskRecoveryService(RecoveryTaskRepository recoveryTaskRepository, Clock clock) {
        this.recoveryTaskRepository = recoveryTaskRepository;
        this.clock = clock;
    }

    public boolean isEligibleRetiredPlanTask(RecoveryTask task) {
        if (task == null) {
            return false;
        }
        String failureReason = task.getFailureReason();
        return failureReason != null && failureReason.contains("reconcileClosedUnmergedPullRequest");
    }

    @Transactional
    public RecoveryTask resumeTask(UUID taskId) {
        return resumeTask(taskId, "REVIVE_FAILED_TASK");
    }

    @Transactional
    public RecoveryTask resumeTask(UUID taskId, String action) {
        if (taskId == null) {
            throw new TaskBadRequestException("INVALID_TASK_ID", "Task ID must be provided");
        }
        if (action != null && !Objects.equals(action, "REVIVE_FAILED_TASK")) {
            throw new TaskBadRequestException("INVALID_ACTION", "Invalid operational action for revival: " + action);
        }

        RecoveryTask task = recoveryTaskRepository.findById(taskId)
                .orElseThrow(() -> new TaskNotFoundException("TASK_NOT_FOUND", "Task not found with ID: " + taskId));

        if (!isEligibleRetiredPlanTask(task)) {
            throw new TaskConflictException("STATE_CONFLICT", "Task is not eligible for recovery");
        }

        String currentStatus = task.getStatus();
        if ("IN_PROGRESS".equals(currentStatus) || "RESOLVED".equals(currentStatus)) {
            throw new TaskConflictException("STATE_CONFLICT", "Task is already in status: " + currentStatus);
        }

        OffsetDateTime now = OffsetDateTime.now(clock);
        int updated = recoveryTaskRepository.updateStatusAtomically(taskId, currentStatus, "IN_PROGRESS", now);
        if (updated == 0) {
            throw new TaskConflictException("STATE_CONFLICT", "Task status conflict during concurrent update");
        }

        task.setStatus("IN_PROGRESS");
        task.setUpdatedAt(now);
        return task;
    }

    public static class TaskRecoveryException extends RuntimeException {
        private final String errorCode;

        public TaskRecoveryException(String errorCode, String message) {
            super(message);
            this.errorCode = errorCode;
        }

        public String getErrorCode() {
            return errorCode;
        }
    }

    public static class TaskNotFoundException extends TaskRecoveryException {
        public TaskNotFoundException(String errorCode, String message) {
            super(errorCode, message);
        }
    }

    public static class TaskBadRequestException extends TaskRecoveryException {
        public TaskBadRequestException(String errorCode, String message) {
            super(errorCode, message);
        }
    }

    public static class TaskConflictException extends TaskRecoveryException {
        public TaskConflictException(String errorCode, String message) {
            super(errorCode, message);
        }
    }
}
