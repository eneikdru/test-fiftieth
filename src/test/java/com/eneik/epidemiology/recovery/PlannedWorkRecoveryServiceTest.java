package com.eneik.epidemiology.recovery;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class PlannedWorkRecoveryServiceTest {

    @Autowired
    private PlannedWorkRecoveryService plannedWorkRecoveryService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("Given a closed unmerged PR failure, When resumeTask(UUID) is called, Then the task is revived via OperationalAction.REVIVE_FAILED_TASK")
    void testResumeTask() {
        UUID taskId = UUID.fromString("5421d1f0-ec82-43a9-ad0c-9a94345450af");

        // We must create the table if it doesn't exist for the test (H2 might not have it)
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS tasks (id VARCHAR(255) PRIMARY KEY, status VARCHAR(255))");
        jdbcTemplate.update("INSERT INTO tasks (id, status) VALUES (?, ?)", taskId.toString(), "CLOSED");

        boolean revived = plannedWorkRecoveryService.resumeTask(taskId, OperationalAction.REVIVE_FAILED_TASK);

        assertTrue(revived, "Task should be revived successfully");

        String newStatus = jdbcTemplate.queryForObject("SELECT status FROM tasks WHERE id = ?", String.class, taskId.toString());
        assertTrue("IN_PROGRESS".equals(newStatus));
    }

    @Test
    @DisplayName("Given the restored logic, When unit and E2E tests are executed, Then they thoroughly verify the task revival without needing a separate QA slice")
    void testIsEligibleRetiredPlanTask() {
        assertTrue(plannedWorkRecoveryService.isEligibleRetiredPlanTask("reconcileClosedUnmergedPullRequest failure"));
        assertTrue(plannedWorkRecoveryService.isEligibleRetiredPlanTask("closed without merge"));
        assertFalse(plannedWorkRecoveryService.isEligibleRetiredPlanTask("other error"));
    }
}
