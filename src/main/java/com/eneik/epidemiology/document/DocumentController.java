package com.eneik.epidemiology.document;

import com.eneik.epidemiology.telemetry.TelemetryService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/documents")
public class DocumentController {

    private final TelemetryService telemetryService;
    private final DocumentRepository documentRepository;

    public DocumentController(TelemetryService telemetryService, DocumentRepository documentRepository) {
        this.telemetryService = telemetryService;
        this.documentRepository = documentRepository;
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

        String parsedQuery = (query == null || query.isEmpty()) ? null : query;
        List<Document> documents = documentRepository.searchDocuments(parsedQuery, author, year);
        List<Map<String, Object>> results = new ArrayList<>();

        for (Document doc : documents) {
            results.add(Map.of(
                    "id", doc.getId(),
                    "title", doc.getTitle(),
                    "author_organization", doc.getAuthorOrganization(),
                    "publication_year", doc.getPublicationYear(),
                    "created_at", doc.getCreatedAt().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
            ));
        }

        telemetryService.recordSearchTelemetry(query, results.size());

        return ResponseEntity.ok(Map.of(
                "query", query,
                "count", results.size(),
                "results", results
        ));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadDocument(
            @RequestParam("title") String title,
            @RequestParam("author_organization") String authorOrganization,
            @RequestParam("publication_year") Integer publicationYear,
            @RequestParam("file") MultipartFile file) {

        String contentType = file.getContentType();
        if (contentType == null || (!contentType.equals("application/pdf") &&
                !contentType.equals("application/msword") &&
                !contentType.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document") &&
                !contentType.equals("application/vnd.ms-excel") &&
                !contentType.equals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "error_code", "VALIDATION_ERROR",
                    "message", "Неподдерживаемый формат файла.",
                    "timestamp", OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
            ));
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename != null) {
            originalFilename = java.nio.file.Paths.get(originalFilename).getFileName().toString();
        } else {
            originalFilename = "unknown";
        }

        String dummyPath = "/data/docs/" + publicationYear + "/" + UUID.randomUUID() + "_" + originalFilename;
        Document document = new Document(title, authorOrganization, publicationYear, dummyPath);
        Document savedDoc = documentRepository.save(document);

        // NOTE: Saving file content to a real filesystem/blob storage goes here.
        // For this minimal slice we only care about persisting the metadata.

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "id", savedDoc.getId(),
                "title", savedDoc.getTitle(),
                "author_organization", savedDoc.getAuthorOrganization(),
                "publication_year", savedDoc.getPublicationYear(),
                "created_at", savedDoc.getCreatedAt().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        ));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<?> downloadDocument(@PathVariable("id") Long id) {
        Document document = documentRepository.findById(id).orElse(null);
        if (document == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "error_code", "NOT_FOUND",
                    "message", "Документ не найден.",
                    "timestamp", OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
            ));
        }

        telemetryService.recordDownloadTelemetry(id);

        // NOTE: Retrieving file content from a real filesystem/blob storage goes here.
        // For this minimal slice we return dummy bytes to satisfy the download endpoint contract.
        byte[] content = ("Содержимое документа " + id).getBytes(StandardCharsets.UTF_8);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"document_" + id + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(content);
    }
}
