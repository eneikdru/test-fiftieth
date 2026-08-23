package com.eneik.epidemiology.document;

import com.eneik.epidemiology.telemetry.TelemetryService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/documents")
public class DocumentController {

    private final TelemetryService telemetryService;

    public DocumentController(TelemetryService telemetryService) {
        this.telemetryService = telemetryService;
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteDocument(@PathVariable("id") Long id) {
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Документ " + id + " успешно удален."
        ));
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchDocuments(
            @RequestParam(name = "query", required = false, defaultValue = "") String query,
            @RequestParam(name = "author", required = false) String author,
            @RequestParam(name = "year", required = false) Integer year) {

        List<Map<String, Object>> results = Collections.emptyList();
        telemetryService.recordSearchTelemetry(query, results.size());

        return ResponseEntity.ok(Map.of(
                "query", query,
                "count", results.size(),
                "results", results
        ));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<?> downloadDocument(@PathVariable("id") Long id) {
        telemetryService.recordDownloadTelemetry(id);

        byte[] content = ("Содержимое документа " + id).getBytes(StandardCharsets.UTF_8);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"document_" + id + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(content);
    }
}
