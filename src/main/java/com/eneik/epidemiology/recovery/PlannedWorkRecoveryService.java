package com.eneik.epidemiology.recovery;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class PlannedWorkRecoveryService {

    private static final Logger log = LoggerFactory.getLogger(PlannedWorkRecoveryService.class);
    private final JdbcTemplate jdbcTemplate;

    public PlannedWorkRecoveryService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public boolean resumeTask(UUID taskId, OperationalAction action) {
        if (action != OperationalAction.REVIVE_FAILED_TASK) {
            return false;
        }

        // Apply CAS protection to ensure we only revive tasks that are currently FAILED or CLOSED
        // We know orchestrator_tasks / tasks table have id/subject_id string fields from previous migrations
        int updatedRows = 0;

        try {
            updatedRows = jdbcTemplate.update(
                "UPDATE tasks SET status = 'IN_PROGRESS' WHERE id = ? AND status IN ('FAILED', 'CLOSED')",
                taskId.toString()
            );
        } catch (Exception e) {
            log.error("Failed to execute resumeTask for taskId {}: {}", taskId, e.getMessage(), e);
            throw e;
        }

        return updatedRows > 0;
    }

    public boolean isEligibleRetiredPlanTask(String failureReason) {
        if (failureReason == null) {
            return false;
        }
        // match reconcileClosedUnmergedPullRequest failure reasons
        return failureReason.contains("reconcileClosedUnmergedPullRequest") ||
               failureReason.contains("closed without merge");
    }
}
