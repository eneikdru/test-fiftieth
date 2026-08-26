package com.eneik.epidemiology.recovery;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/recovery/tasks")
public class PlannedWorkRecoveryController {

    @PostMapping("/{taskId}/resume")
    public ResponseEntity<?> resumeTask(@PathVariable("taskId") UUID taskId, @RequestBody Map<String, Object> request) {
        if (!request.containsKey("action") || !"REVIVE_FAILED_TASK".equals(request.get("action"))) {
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok(Map.of(
                "status", "IN_PROGRESS",
                "message", "Task successfully revived"
        ));
    }
}
