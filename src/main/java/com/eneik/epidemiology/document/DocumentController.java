package com.eneik.epidemiology.document;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/documents")
public class DocumentController {

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteDocument(@PathVariable("id") Long id) {
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Документ " + id + " успешно удален."
        ));
    }
}
