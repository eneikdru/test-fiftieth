package com.eneik.epidemiology.process;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

@Service
public class ProcessRecoveryService {

    private final BackgroundProcessRepository recoveryProcessRepository;
    private final Clock clock;

    @Autowired
    public ProcessRecoveryService(BackgroundProcessRepository recoveryProcessRepository) {
        this(recoveryProcessRepository, Clock.systemUTC());
    }

    public ProcessRecoveryService(BackgroundProcessRepository recoveryProcessRepository, Clock clock) {
        this.recoveryProcessRepository = recoveryProcessRepository;
        this.clock = clock;
    }

    public boolean isEligibleRetiredPlanProcess(BackgroundProcess process) {
        if (process == null) {
            return false;
        }
        String failureReason = process.getFailureReason();
        return failureReason != null && (
            failureReason.contains("data_processing_error") ||
            failureReason.contains("data-integrity") ||
            failureReason.contains("iteration-admission data-integrity")
        );
    }

    @Transactional
    public BackgroundProcess resumeProcess(UUID processId) {
        return resumeProcess(processId, "REVIVE_FAILED_TASK");
    }

    @Transactional
    public BackgroundProcess resumeProcess(UUID processId, String action) {
        if (processId == null) {
            throw new ProcessBadRequestException("INVALID_TASK_ID", "Process ID must be provided");
        }
        if (action != null && !Objects.equals(action, "REVIVE_FAILED_TASK")) {
            throw new ProcessBadRequestException("INVALID_ACTION", "Invalid operational action for revival: " + action);
        }

        BackgroundProcess process = recoveryProcessRepository.findById(processId)
                .orElseThrow(() -> new ProcessNotFoundException("PROCESS_NOT_FOUND", "Process not found with ID: " + processId));

        if (!isEligibleRetiredPlanProcess(process)) {
            throw new ProcessConflictException("STATE_CONFLICT", "Process is not eligible for recovery");
        }

        String currentStatus = process.getStatus();
        if ("IN_PROGRESS".equals(currentStatus) || "RESOLVED".equals(currentStatus)) {
            throw new ProcessConflictException("STATE_CONFLICT", "Process is already in status: " + currentStatus);
        }

        OffsetDateTime now = OffsetDateTime.now(clock);
        int updated = recoveryProcessRepository.updateStatusAtomically(processId, currentStatus, "IN_PROGRESS", now);
        if (updated == 0) {
            throw new ProcessConflictException("STATE_CONFLICT", "Process status conflict during concurrent update");
        }

        process.setStatus("IN_PROGRESS");
        process.setUpdatedAt(now);
        return process;
    }

    public static class ProcessRecoveryException extends RuntimeException {
        private final String errorCode;

        public ProcessRecoveryException(String errorCode, String message) {
            super(message);
            this.errorCode = errorCode;
        }

        public String getErrorCode() {
            return errorCode;
        }
    }

    public static class ProcessNotFoundException extends ProcessRecoveryException {
        public ProcessNotFoundException(String errorCode, String message) {
            super(errorCode, message);
        }
    }

    public static class ProcessBadRequestException extends ProcessRecoveryException {
        public ProcessBadRequestException(String errorCode, String message) {
            super(errorCode, message);
        }
    }

    public static class ProcessConflictException extends ProcessRecoveryException {
        public ProcessConflictException(String errorCode, String message) {
            super(errorCode, message);
        }
    }
}
