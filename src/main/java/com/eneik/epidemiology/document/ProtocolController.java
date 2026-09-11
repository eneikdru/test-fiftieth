package com.eneik.epidemiology.document;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/protocols")
public class ProtocolController {

    private final DocumentRepository documentRepository;

    public ProtocolController(DocumentRepository documentRepository) {
        this.documentRepository = documentRepository;
    }

    @GetMapping
    public ResponseEntity<?> getProtocols(
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<Document> protocolPage = documentRepository.fullTextSearch(
                (q != null && !q.trim().isEmpty()) ? q.trim() : null,
                "PROTOCOL",
                null,
                null,
                pageable
        );

        return ResponseEntity.ok(Map.of(
                "protocols", protocolPage.getContent(),
                "total_elements", protocolPage.getTotalElements(),
                "page", protocolPage.getNumber(),
                "size", protocolPage.getSize()
        ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getProtocolById(@PathVariable("id") Long id) {
        return documentRepository.findById(id)
                .filter(doc -> doc.getDocType() != null && "PROTOCOL".equalsIgnoreCase(doc.getDocType()))
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
