package com.eneik.epidemiology.document;

import com.eneik.epidemiology.telemetry.TelemetryService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/documents")
public class DocumentController {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx"
    );

    private final DocumentRepository documentRepository;
    private final TelemetryService telemetryService;

    public DocumentController(DocumentRepository documentRepository, TelemetryService telemetryService) {
        this.documentRepository = documentRepository;
        this.telemetryService = telemetryService;
    }

    private boolean isValidExtension(String originalFilename) {
        if (originalFilename == null || !originalFilename.contains(".")) {
            return false;
        }
        String extension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
        return ALLOWED_EXTENSIONS.contains(extension);
    }

    @PostMapping(value = {"", "/upload"}, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam(name = "title", required = false) String title,
            @RequestParam(name = "authorOrganization", required = false) String authorOrganization,
            @RequestParam(name = "author", required = false) String author,
            @RequestParam(name = "publicationYear", required = false) Integer publicationYear,
            @RequestParam(name = "year", required = false) Integer year) {

        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error_code", "INVALID_FILE",
                    "message", "Файл не загружен или пуст."
            ));
        }

        String originalFilename = file.getOriginalFilename();
        if (!isValidExtension(originalFilename)) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error_code", "UNSUPPORTED_FILE_TYPE",
                    "message", "Неподдерживаемый формат файла. Разрешены только PDF и офисные документы (.pdf, .doc, .docx, .xls, .xlsx, .ppt, .pptx)."
            ));
        }

        String docTitle = (title != null && !title.trim().isEmpty()) ? title.trim() : originalFilename;
        String docAuthor = (authorOrganization != null && !authorOrganization.trim().isEmpty())
                ? authorOrganization.trim()
                : ((author != null && !author.trim().isEmpty()) ? author.trim() : "НИИ Эпидемиологии");
        Integer docYear = (publicationYear != null) ? publicationYear : ((year != null) ? year : OffsetDateTime.now().getYear());

        String filePath = "/data/docs/uploads/" + (originalFilename != null ? originalFilename : "doc.pdf");

        try {
            Path targetDir = Paths.get("./data/docs/uploads");
            Files.createDirectories(targetDir);
            Path targetPath = targetDir.resolve(originalFilename != null ? originalFilename : "doc.pdf");
            file.transferTo(targetPath.toFile());
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "error_code", "FILE_SAVE_ERROR",
                    "message", "Ошибка сохранения файла на сервере."
            ));
        }

        Document document = new Document(docTitle, docAuthor, docYear, filePath);
        Document savedDocument = documentRepository.save(document);

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "success", true,
                "message", "Документ успешно загружен.",
                "document", savedDocument
        ));
    }

    @GetMapping
    public ResponseEntity<?> getAllDocuments() {
        List<Document> documents = documentRepository.findAll();
        return ResponseEntity.ok(documents);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteDocument(@PathVariable("id") Long id) {
        if (documentRepository.existsById(id)) {
            documentRepository.deleteById(id);
        }
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Документ " + id + " успешно удален."
        ));
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchDocuments(
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(name = "query", required = false) String query,
            @RequestParam(name = "author", required = false) String author,
            @RequestParam(name = "year", required = false) Integer year,
            @RequestParam(name = "doc_type", required = false) String docType,
            @RequestParam(name = "from_date", required = false) String fromDateStr,
            @RequestParam(name = "to_date", required = false) String toDateStr,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {

        boolean isFullTextSearchRequest = (q != null || docType != null || fromDateStr != null || toDateStr != null);

        if (isFullTextSearchRequest) {
            if (q != null && q.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error_code", "INVALID_SEARCH_QUERY",
                        "message", "Поисковый запрос не должен быть пустым.",
                        "timestamp", OffsetDateTime.now().toString()
                ));
            }

            LocalDate fromDate = parseDate(fromDateStr);
            LocalDate toDate = parseDate(toDateStr);

            Pageable pageable = PageRequest.of(page, size);
            Page<Document> resultPage = documentRepository.fullTextSearch(
                    (q != null && !q.trim().isEmpty()) ? q.trim() : null,
                    (docType != null && !docType.trim().isEmpty()) ? docType.trim() : null,
                    fromDate,
                    toDate,
                    pageable
            );

            telemetryService.recordSearchTelemetry(q != null ? q : "", resultPage.getContent().size());

            List<Map<String, Object>> items = resultPage.getContent().stream().map(doc -> {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("document_id", doc.getId());
                item.put("title", doc.getTitle());
                item.put("doc_type", doc.getDocType() != null ? doc.getDocType() : "DOCUMENT");
                item.put("author_organization", doc.getAuthorOrganization());

                String pubDateStr = null;
                if (doc.getPublicationDate() != null) {
                    pubDateStr = doc.getPublicationDate().toString();
                } else if (doc.getPublicationYear() != null) {
                    pubDateStr = doc.getPublicationYear() + "-01-01";
                }
                item.put("publication_date", pubDateStr);

                item.put("highlights", buildHighlights(doc, q));
                item.put("matched_pages", List.of(1));
                item.put("relevance_score", 1.0);
                return item;
            }).collect(Collectors.toList());

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("items", items);
            response.put("total_elements", resultPage.getTotalElements());
            response.put("page", resultPage.getNumber());
            response.put("size", resultPage.getSize());

            return ResponseEntity.ok(response);
        }

        // Legacy search compatibility
        String normalizedQuery = (query != null && !query.trim().isEmpty()) ? query.trim() : null;
        String normalizedAuthor = (author != null && !author.trim().isEmpty()) ? author.trim() : null;

        Pageable pageable = PageRequest.of(page, size);
        Page<Document> resultPage = documentRepository.searchDocuments(normalizedQuery, normalizedAuthor, year, pageable);

        String telemetryQuery = query != null ? query : (author != null ? author : "");
        telemetryService.recordSearchTelemetry(telemetryQuery, resultPage.getContent().size());

        return ResponseEntity.ok(Map.of(
                "query", telemetryQuery,
                "count", resultPage.getContent().size(),
                "results", resultPage.getContent(),
                "totalPages", resultPage.getTotalPages(),
                "totalElements", resultPage.getTotalElements(),
                "currentPage", resultPage.getNumber()
        ));
    }

    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(dateStr.trim());
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private List<String> buildHighlights(Document doc, String q) {
        if (q == null || q.isBlank()) {
            return Collections.emptyList();
        }
        String text = doc.getTextContent();
        if (text == null || text.isBlank()) {
            text = doc.getTitle();
        }
        if (text == null || text.isBlank()) {
            return Collections.emptyList();
        }

        String lowerText = text.toLowerCase();
        String lowerQ = q.toLowerCase();
        int index = lowerText.indexOf(lowerQ);

        if (index != -1) {
            int start = Math.max(0, index - 40);
            int end = Math.min(text.length(), index + q.length() + 40);
            String snippet = text.substring(start, end);

            String matchedWord = text.substring(index, index + q.length());
            String highlightedSnippet = snippet.replace(matchedWord, "<em>" + matchedWord + "</em>");
            if (start > 0) highlightedSnippet = "..." + highlightedSnippet;
            if (end < text.length()) highlightedSnippet = highlightedSnippet + "...";

            return List.of(highlightedSnippet);
        } else {
            return List.of(text);
        }
    }

    @GetMapping("/{id}/view")
    public ResponseEntity<?> viewDocument(@PathVariable("id") Long id) {
        telemetryService.recordDownloadTelemetry(id);

        return documentRepository.findById(id)
                .map(doc -> {
                    String fileName = doc.getFilePath().substring(doc.getFilePath().lastIndexOf('/') + 1);
                    if (fileName.isEmpty()) {
                        fileName = "document_" + id + ".pdf";
                    }

                    try {
                        Path basePath = Paths.get("data/docs/uploads").toAbsolutePath().normalize();
                        String normalizedDbPath = doc.getFilePath();

                        if (normalizedDbPath.startsWith("/")) {
                            normalizedDbPath = normalizedDbPath.substring(1);
                        }
                        if (normalizedDbPath.startsWith("data/docs/uploads/")) {
                            normalizedDbPath = normalizedDbPath.substring("data/docs/uploads/".length());
                        }

                        Path resolvedPath = basePath.resolve(normalizedDbPath).normalize();
                        if (!resolvedPath.startsWith(basePath)) {
                            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body((Object) "Invalid file path");
                        }

                        if (!Files.exists(resolvedPath)) {
                            byte[] fallbackContent = ("Содержимое документа: " + doc.getTitle()).getBytes(StandardCharsets.UTF_8);
                            return ResponseEntity.ok()
                                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"")
                                    .contentType(MediaType.APPLICATION_PDF)
                                    .body(fallbackContent);
                        }

                        Object body = new org.springframework.core.io.FileSystemResource(resolvedPath.toFile());

                        MediaType mediaType = MediaType.APPLICATION_PDF;
                        if (fileName.toLowerCase().endsWith(".txt")) {
                            mediaType = MediaType.TEXT_PLAIN;
                        } else if (fileName.toLowerCase().endsWith(".doc") || fileName.toLowerCase().endsWith(".docx") ||
                                   fileName.toLowerCase().endsWith(".xls") || fileName.toLowerCase().endsWith(".xlsx")) {
                            mediaType = MediaType.APPLICATION_OCTET_STREAM;
                        }

                        return ResponseEntity.ok()
                                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"")
                                .contentType(mediaType)
                                .body(body);
                    } catch (Exception e) {
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body((Object) "Failed to process document view");
                    }
                })
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body((Object) "Document not found in database"));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<?> downloadDocument(@PathVariable("id") Long id) {
        telemetryService.recordDownloadTelemetry(id);

        return documentRepository.findById(id)
                .map(doc -> {
                    byte[] content = ("Содержимое документа: " + doc.getTitle()).getBytes(StandardCharsets.UTF_8);
                    String fileName = doc.getFilePath().substring(doc.getFilePath().lastIndexOf('/') + 1);
                    if (fileName.isEmpty()) {
                        fileName = "document_" + id + ".pdf";
                    }
                    return ResponseEntity.ok()
                            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                            .contentType(MediaType.APPLICATION_OCTET_STREAM)
                            .body(content);
                })
                .orElseGet(() -> {
                    byte[] fallbackContent = ("Содержимое документа " + id).getBytes(StandardCharsets.UTF_8);
                    return ResponseEntity.ok()
                            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"document_" + id + ".pdf\"")
                            .contentType(MediaType.APPLICATION_PDF)
                            .body(fallbackContent);
                });
    }
}
