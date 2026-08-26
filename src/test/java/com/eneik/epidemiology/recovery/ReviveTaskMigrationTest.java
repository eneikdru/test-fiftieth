package com.eneik.epidemiology.recovery;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class ReviveTaskMigrationTest {

    @Autowired
    private PlannedWorkRecoveryService plannedWorkRecoveryService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("Given stuck task 8bd0dbae-41f6-466a-95a7-aff680ed0866 in FAILED or CLOSED state, When resumeTask is invoked with REVIVE_FAILED_TASK, Then task status is updated to IN_PROGRESS via atomic CAS update")
    void testResumeStuckTask() {
        UUID taskId = UUID.fromString("8bd0dbae-41f6-466a-95a7-aff680ed0866");

        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS tasks (id VARCHAR(255) PRIMARY KEY, status VARCHAR(255))");
        jdbcTemplate.update("DELETE FROM tasks WHERE id = ?", taskId.toString());
        jdbcTemplate.update("INSERT INTO tasks (id, status) VALUES (?, ?)", taskId.toString(), "FAILED");

        boolean revived = plannedWorkRecoveryService.resumeTask(taskId, OperationalAction.REVIVE_FAILED_TASK);

        assertTrue(revived, "Task 8bd0dbae-41f6-466a-95a7-aff680ed0866 should be revived");

        String status = jdbcTemplate.queryForObject("SELECT status FROM tasks WHERE id = ?", String.class, taskId.toString());
        assertEquals("IN_PROGRESS", status);
    }
}
