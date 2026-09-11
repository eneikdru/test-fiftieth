package com.eneik.epidemiology.catalog;

import com.eneik.epidemiology.document.Document;
import com.eneik.epidemiology.document.DocumentRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.GrantedAuthority;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/catalog/documents")
public class CatalogController {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx"
    );

    private final DocumentRepository documentRepository;

    public CatalogController(DocumentRepository documentRepository) {
        this.documentRepository = documentRepository;
    }

    private boolean isProtocolAccessAuthorized() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }
        for (GrantedAuthority authority : auth.getAuthorities()) {
            if ("ROLE_EPIDEMIOLOGIST".equals(authority.getAuthority()) ||
                "ROLE_ADMIN".equals(authority.getAuthority()) ||
                "EPIDEMIOLOGIST".equals(authority.getAuthority()) ||
                "ADMIN".equals(authority.getAuthority())) {
                return true;
            }
        }
        return false;
    }

    private boolean isAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }
        for (GrantedAuthority authority : auth.getAuthorities()) {
            if ("ROLE_ADMIN".equals(authority.getAuthority()) ||
                "ADMIN".equals(authority.getAuthority())) {
                return true;
            }
        }
        return false;
    }

    @GetMapping
    public ResponseEntity<?> searchCatalogDocuments(
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(name = "author_organization", required = false) String authorOrganization,
            @RequestParam(name = "doc_type", required = false) String docType,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {

        if (q != null && q.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error_code", "INVALID_SEARCH_QUERY",
                    "message", "Поисковый запрос не должен быть пустым.",
                    "timestamp", OffsetDateTime.now().toString()
            ));
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<Document> resultPage = documentRepository.fullTextSearch(
                (q != null && !q.trim().isEmpty()) ? q.trim() : null,
                (docType != null && !docType.trim().isEmpty()) ? docType.trim() : null,
                null,
                null,
                pageable
        );

        List<Document> filteredContent = resultPage.getContent();
        if (authorOrganization != null && !authorOrganization.trim().isEmpty()) {
            filteredContent = filteredContent.stream()
                .filter(doc -> doc.getAuthorOrganization() != null && doc.getAuthorOrganization().toLowerCase().contains(authorOrganization.toLowerCase()))
                .collect(Collectors.toList());
        }

        if (!isProtocolAccessAuthorized()) {
            filteredContent = filteredContent.stream()
                    .filter(doc -> doc.getDocType() == null || !"PROTOCOL".equalsIgnoreCase(doc.getDocType()))
                    .collect(Collectors.toList());
        }

        List<Map<String, Object>> items = filteredContent.stream().map(this::mapToCatalogItem).collect(Collectors.toList());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("items", items);
        response.put("total_elements", resultPage.getTotalElements());
        response.put("page", resultPage.getNumber());
        response.put("size", resultPage.getSize());

        return ResponseEntity.ok(response);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadCatalogDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam("title") String title,
            @RequestParam("author_organization") String authorOrganization,
            @RequestParam("publication_year") Integer publicationYear,
            @RequestParam(name = "doc_type", required = false) String docType) {

        if (!isAdmin()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                    "error_code", "FORBIDDEN",
                    "message", "Требуются права администратора",
                    "timestamp", OffsetDateTime.now().toString()
            ));
        }

        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error_code", "INVALID_FILE",
                    "message", "Файл не загружен или пуст.",
                    "timestamp", OffsetDateTime.now().toString()
            ));
        }

        String originalFilename = file.getOriginalFilename();
        if (!isValidExtension(originalFilename)) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error_code", "UNSUPPORTED_FILE_TYPE",
                    "message", "Неподдерживаемый формат файла.",
                    "timestamp", OffsetDateTime.now().toString()
            ));
        }

        String docTitle = title.trim();
        String docAuthor = authorOrganization.trim();
        String safeFilename = UUID.randomUUID().toString() + "_" + (originalFilename != null ? originalFilename.replaceAll("[^a-zA-Z0-9.-]", "_") : "doc.pdf");
        String filePath = "/data/docs/uploads/" + safeFilename;

        try {
            Path targetDir = Paths.get("./data/docs/uploads");
            Files.createDirectories(targetDir);
            Path targetPath = targetDir.resolve(safeFilename);
            file.transferTo(targetPath.toFile());
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "error_code", "FILE_SAVE_ERROR",
                    "message", "Ошибка сохранения файла на сервере.",
                    "timestamp", OffsetDateTime.now().toString()
            ));
        }

        Document document = new Document(docTitle, docAuthor, publicationYear, filePath);
        document.setDocType(docType);
        Document savedDocument = documentRepository.save(document);

        return ResponseEntity.status(HttpStatus.CREATED).body(mapToCatalogItem(savedDocument));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getCatalogDocumentById(@PathVariable("id") Long id) {
        return documentRepository.findById(id)
                .map(doc -> {
                    if ("PROTOCOL".equalsIgnoreCase(doc.getDocType()) && !isProtocolAccessAuthorized()) {
                         return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                            "error_code", "CATALOG_ITEM_NOT_FOUND",
                            "message", "Документ не найден",
                            "timestamp", OffsetDateTime.now().toString()
                        ));
                    }
                    return ResponseEntity.ok(mapToCatalogItem(doc));
                })
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                        "error_code", "CATALOG_ITEM_NOT_FOUND",
                        "message", "Документ не найден",
                        "timestamp", OffsetDateTime.now().toString()
                )));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteCatalogDocument(@PathVariable("id") Long id) {
        if (!isAdmin()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                    "error_code", "FORBIDDEN",
                    "message", "Требуются права администратора",
                    "timestamp", OffsetDateTime.now().toString()
            ));
        }

        if (documentRepository.existsById(id)) {
            documentRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                "error_code", "CATALOG_ITEM_NOT_FOUND",
                "message", "Документ не найден",
                "timestamp", OffsetDateTime.now().toString()
        ));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<?> downloadCatalogDocument(@PathVariable("id") Long id) {
        return documentRepository.findById(id)
                .map(doc -> {
                    if ("PROTOCOL".equalsIgnoreCase(doc.getDocType()) && !isProtocolAccessAuthorized()) {
                         return ResponseEntity.status(HttpStatus.NOT_FOUND).body((Object) Map.of(
                            "error_code", "CATALOG_ITEM_NOT_FOUND",
                            "message", "Документ не найден",
                            "timestamp", OffsetDateTime.now().toString()
                        ));
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
                        if (!resolvedPath.startsWith(basePath) || !Files.exists(resolvedPath)) {
                            return ResponseEntity.status(HttpStatus.NOT_FOUND).body((Object) Map.of(
                                "error_code", "CATALOG_ITEM_NOT_FOUND",
                                "message", "Файл документа не найден",
                                "timestamp", OffsetDateTime.now().toString()
                            ));
                        }

                        Object body = new org.springframework.core.io.FileSystemResource(resolvedPath.toFile());
                        String fileName = resolvedPath.getFileName().toString();

                        MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
                        if (fileName.toLowerCase().endsWith(".pdf")) {
                            mediaType = MediaType.APPLICATION_PDF;
                        }

                        return ResponseEntity.ok()
                                .contentType(mediaType)
                                .body(body);
                    } catch (Exception e) {
                        return ResponseEntity.status(HttpStatus.NOT_FOUND).body((Object) Map.of(
                            "error_code", "CATALOG_ITEM_NOT_FOUND",
                            "message", "Ошибка доступа к файлу",
                            "timestamp", OffsetDateTime.now().toString()
                        ));
                    }
                })
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body((Object) Map.of(
                        "error_code", "CATALOG_ITEM_NOT_FOUND",
                        "message", "Документ не найден",
                        "timestamp", OffsetDateTime.now().toString()
                )));
    }

    private boolean isValidExtension(String originalFilename) {
        if (originalFilename == null || !originalFilename.contains(".")) {
            return false;
        }
        String extension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
        return ALLOWED_EXTENSIONS.contains(extension);
    }

    private Map<String, Object> mapToCatalogItem(Document doc) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", doc.getId());
        item.put("title", doc.getTitle());
        item.put("doc_type", doc.getDocType() != null ? doc.getDocType() : "DOCUMENT");
        item.put("author_organization", doc.getAuthorOrganization());
        item.put("publication_year", doc.getPublicationYear());
        item.put("created_at", doc.getCreatedAt() != null ? doc.getCreatedAt().toString() : OffsetDateTime.now().toString());
        if (doc.getTextContent() != null) {
            item.put("text_content", doc.getTextContent());
        }
        return item;
    }
}
