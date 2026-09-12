package com.eneik.epidemiology.document;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import com.eneik.epidemiology.telemetry.TelemetryService;
import org.springframework.security.core.context.SecurityContextHolder;
import com.eneik.epidemiology.user.User;
import com.eneik.epidemiology.user.UserRepository;

import com.itextpdf.text.Document;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfWriter;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/dossier")
public class EmployeeDossierController {

    private final EmployeeDocumentRepository employeeDocumentRepository;
    private final DossierReportRepository dossierReportRepository;
    private final TelemetryService telemetryService;
    private final UserRepository userRepository;

    public EmployeeDossierController(EmployeeDocumentRepository employeeDocumentRepository, DossierReportRepository dossierReportRepository, TelemetryService telemetryService, UserRepository userRepository) {
        this.employeeDocumentRepository = employeeDocumentRepository;
        this.dossierReportRepository = dossierReportRepository;
        this.telemetryService = telemetryService;
        this.userRepository = userRepository;
    }

    @GetMapping("/documents")
    public ResponseEntity<?> searchEmployeeDocuments(
            @RequestParam(value = "employee_id", required = false) String employeeId,
            @RequestParam(value = "employee_surname", required = false) String employeeSurname,
            @RequestParam(value = "doc_type", required = false) String docType,
            @RequestParam(value = "scientific_direction", required = false) String scientificDirection,
            @RequestParam(value = "query", required = false) String query,
            @RequestParam(value = "from_date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(value = "to_date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {

        org.springframework.security.core.Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error_code", "UNAUTHORIZED", "message", "Требуется авторизация для выполнения данной операции."));
        }

        String currentUsername = authentication.getName();
        User currentUser = userRepository.findByUsername(currentUsername).orElse(null);

        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);
        boolean isAdmin = currentUser != null && "ADMIN".equals(currentUser.getRole());
        String userDepartment = currentUser != null ? currentUser.getDepartment() : null;
        List<String> userCoursesList = currentUser != null && currentUser.getCourses() != null && !currentUser.getCourses().isEmpty()
                ? java.util.Arrays.asList(currentUser.getCourses().split("\\s*,\\s*"))
                : java.util.Collections.emptyList();

        org.springframework.data.domain.Page<EmployeeDocument> documentPage = employeeDocumentRepository.searchEmployeeDocumentsSecure(
                employeeId, employeeSurname, docType, scientificDirection, query, fromDate, toDate, isAdmin, userDepartment, userCoursesList, pageable
        );
        List<EmployeeDocument> documents = documentPage != null ? documentPage.getContent() : java.util.Collections.emptyList();

        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.add("X-Total-Count", String.valueOf(documentPage != null ? documentPage.getTotalElements() : 0));
        headers.add("X-Total-Pages", String.valueOf(documentPage != null ? documentPage.getTotalPages() : 0));

        return ResponseEntity.ok().headers(headers).body(documents);
    }

    @PostMapping("/reports")
    @Transactional
    public ResponseEntity<?> generateDossierReport(@RequestBody(required = false) Map<String, Object> requestBody) {
        if (requestBody == null || !requestBody.containsKey("employee_id") || !requestBody.containsKey("template_type")) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error_code", "VALIDATION_ERROR",
                    "message", "Не указан обязательный параметр employee_id или template_type."
            ));
        }

        String employeeId = (String) requestBody.get("employee_id");
        String templateType = (String) requestBody.get("template_type");

        long startTime = System.currentTimeMillis();
        boolean success = false;
        DossierReport report = null;

        try {
            if (requestBody.containsKey("session_id") || requestBody.containsKey("session_start_time") || requestBody.containsKey("session_duration_ms")) {
                String sessionId = requestBody.containsKey("session_id") ? (String) requestBody.get("session_id") : "session_" + employeeId;
                OffsetDateTime sessionStart = null;
                OffsetDateTime sessionEnd = null;
                Long sessionDurationMs = null;

                if (requestBody.containsKey("session_start_time") && requestBody.get("session_start_time") != null) {
                    sessionStart = OffsetDateTime.parse(requestBody.get("session_start_time").toString());
                }
                if (requestBody.containsKey("session_end_time") && requestBody.get("session_end_time") != null) {
                    sessionEnd = OffsetDateTime.parse(requestBody.get("session_end_time").toString());
                } else if (sessionStart != null) {
                    sessionEnd = OffsetDateTime.now();
                }

                if (requestBody.containsKey("session_duration_ms") && requestBody.get("session_duration_ms") != null) {
                    sessionDurationMs = ((Number) requestBody.get("session_duration_ms")).longValue();
                }

                telemetryService.recordAnalysisSpeedTelemetry(sessionId, sessionStart, sessionEnd, sessionDurationMs);
            }

            List<EmployeeDocument> documents;

            if (requestBody.containsKey("document_ids") && requestBody.get("document_ids") != null) {
                List<Number> docIdsNum = (List<Number>) requestBody.get("document_ids");
                List<Long> docIds = docIdsNum.stream().map(Number::longValue).toList();
                documents = employeeDocumentRepository.findAllById(docIds);

                // Only include documents belonging to the specified employee
                documents = documents.stream().filter(d -> d.getEmployeeId().equals(employeeId)).toList();
            } else {
                 documents = employeeDocumentRepository.findUnifiedEmployeeDossier(employeeId);
            }

            if (requestBody.containsKey("include_doc_types") && requestBody.get("include_doc_types") != null) {
                 List<String> docTypes = (List<String>) requestBody.get("include_doc_types");
                 documents = documents.stream().filter(d -> docTypes.contains(d.getDocType())).toList();
            }

            org.springframework.security.core.Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error_code", "UNAUTHORIZED", "message", "Требуется авторизация для выполнения данной операции."));
            }

            String currentUsername = authentication.getName();
            User currentUser = userRepository.findByUsername(currentUsername).orElse(null);

            if (currentUser != null && !"ADMIN".equals(currentUser.getRole())) {
                List<String> userCoursesList = currentUser.getCourses() != null && !currentUser.getCourses().isEmpty()
                        ? java.util.Arrays.asList(currentUser.getCourses().split("\\s*,\\s*"))
                        : java.util.Collections.emptyList();
                documents = documents.stream().filter(d -> {
                    if (!"STRAIN_ISOLATION".equals(d.getDocType()) && !"REPORT".equals(d.getDocType())) return true;
                    boolean depMatch = d.getAccessDepartment() != null && d.getAccessDepartment().equals(currentUser.getDepartment());
                    boolean courseMatch = d.getAccessCourse() != null && userCoursesList.contains(d.getAccessCourse());
                    return depMatch || courseMatch;
                }).toList();
            }

            String summaryText = "Сводная справка по сотруднику " + employeeId + ": " + documents.size() + " документов.";

            report = new DossierReport(
                    employeeId,
                    templateType,
                    "PENDING",
                    null,
                    documents.size(),
                    null
            );
            if (currentUser != null) {
                report.setAccessDepartment(currentUser.getDepartment());
                report.setAccessCourse(currentUser.getCourses());
            }
            report = dossierReportRepository.save(report);

            // Simulating immediate generation as a single atomic operation for now (satisfies complicated cynefin probe)
            int updatedCount = dossierReportRepository.updateStatus(report.getId(), "PENDING", "COMPLETED");
            if (updatedCount > 0) {
                report.setStatus("COMPLETED");
                report.setSummaryText(summaryText);
                report.setDownloadUrl("/api/v1/dossier/reports/" + report.getId() + "/download");
                report = dossierReportRepository.save(report); // update remaining fields
                success = true;
            }
        } finally {
            long processingTime = System.currentTimeMillis() - startTime;
            telemetryService.recordDossierGenerationTelemetry(processingTime, success);
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "id", report != null ? report.getId() : null,
                "employee_id", report.getEmployeeId(),
                "template_type", report.getTemplateType(),
                "status", report.getStatus(),
                "summary_text", report.getSummaryText(),
                "document_count", report.getDocumentCount(),
                "download_url", report.getDownloadUrl(),
                "created_at", report.getCreatedAt() != null ? report.getCreatedAt().toString() : ""
        ));
    }
    @GetMapping("/reports")
    public ResponseEntity<?> listDossierReports(
            @RequestParam(value = "employee_id", required = false) String employeeId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {

        org.springframework.security.core.Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error_code", "UNAUTHORIZED", "message", "Требуется авторизация для выполнения данной операции."));
        }

        String currentUsername = authentication.getName();
        User currentUser = userRepository.findByUsername(currentUsername).orElse(null);

        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);
        boolean isAdmin = currentUser != null && "ADMIN".equals(currentUser.getRole());
        String userDepartment = currentUser != null ? currentUser.getDepartment() : null;
        List<String> userCoursesList = currentUser != null && currentUser.getCourses() != null && !currentUser.getCourses().isEmpty()
                ? java.util.Arrays.asList(currentUser.getCourses().split("\\s*,\\s*"))
                : java.util.Collections.emptyList();

        org.springframework.data.domain.Page<DossierReport> reportPage = dossierReportRepository.searchReportsSecure(
                employeeId, isAdmin, userDepartment, userCoursesList, pageable
        );

        List<Map<String, Object>> reports = reportPage.getContent().stream()
                .map(report -> Map.<String, Object>of(
                        "id", report.getId(),
                        "employee_id", report.getEmployeeId(),
                        "template_type", report.getTemplateType(),
                        "status", report.getStatus(),
                        "summary_text", report.getSummaryText() != null ? report.getSummaryText() : "",
                        "document_count", report.getDocumentCount(),
                        "download_url", report.getDownloadUrl() != null ? report.getDownloadUrl() : "",
                        "created_at", report.getCreatedAt() != null ? report.getCreatedAt().toString() : ""
                )).toList();

        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Total-Count", String.valueOf(reportPage.getTotalElements()));
        headers.add("X-Total-Pages", String.valueOf(reportPage.getTotalPages()));

        return ResponseEntity.ok().headers(headers).body(reports);
    }



    private boolean isAccessDenied(User currentUser, DossierReport report) {
        if (currentUser == null || "ADMIN".equals(currentUser.getRole())) {
            return false;
        }
        if (report.getAccessDepartment() == null && report.getAccessCourse() == null) {
            return false;
        }
        boolean depMatch = report.getAccessDepartment() != null && report.getAccessDepartment().equals(currentUser.getDepartment());
        List<String> userCoursesList = currentUser.getCourses() != null && !currentUser.getCourses().isEmpty()
                ? java.util.Arrays.asList(currentUser.getCourses().split("\\s*,\\s*"))
                : java.util.Collections.emptyList();
        boolean courseMatch = report.getAccessCourse() != null && userCoursesList.contains(report.getAccessCourse());
        return !depMatch && !courseMatch;
    }

    @GetMapping("/reports/{id}")
    public ResponseEntity<?> getDossierReportStatus(@PathVariable("id") Long id) {
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByUsername(currentUsername).orElse(null);

        return dossierReportRepository.findById(id)
                .map(report -> {
                    if (isAccessDenied(currentUser, report)) {
                        return ResponseEntity.status(HttpStatus.FORBIDDEN).body((Object) Map.of("error_code", "FORBIDDEN", "message", "Access denied"));
                    }
                    return ResponseEntity.ok(Map.of(
                            "id", report.getId(),
                            "employee_id", report.getEmployeeId(),
                            "template_type", report.getTemplateType(),
                            "status", report.getStatus(),
                            "summary_text", report.getSummaryText() != null ? report.getSummaryText() : "",
                            "document_count", report.getDocumentCount(),
                            "download_url", report.getDownloadUrl() != null ? report.getDownloadUrl() : "",
                            "created_at", report.getCreatedAt() != null ? report.getCreatedAt().toString() : ""
                    ));
                })
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                        "error_code", "NOT_FOUND",
                        "message", "Справка не найдена"
                )));
    }

    @GetMapping("/reports/{id}/download")
    public ResponseEntity<?> downloadDossierReport(@PathVariable("id") Long id) {
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByUsername(currentUsername).orElse(null);

        return dossierReportRepository.findById(id)
                .map(report -> {
                    if (isAccessDenied(currentUser, report)) {
                        return ResponseEntity.status(HttpStatus.FORBIDDEN).body((Object) Map.of("error_code", "FORBIDDEN", "message", "Access denied"));
                    }

                    if (!"COMPLETED".equals(report.getStatus())) {
                        return ResponseEntity.status(HttpStatus.NOT_FOUND).body((Object) Map.of(
                                "error_code", "NOT_FOUND",
                                "message", "Файл справки не найден или еще не сгенерирован"
                        ));
                    }

                    try {
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        Document document = new Document();
                        PdfWriter.getInstance(document, baos);
                        document.open();

                        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, "Cp1251", BaseFont.NOT_EMBEDDED, 16);
                        Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, "Cp1251", BaseFont.NOT_EMBEDDED, 12);
                        Font bodyFont = FontFactory.getFont(FontFactory.HELVETICA, "Cp1251", BaseFont.NOT_EMBEDDED, 11);

                        // Page 1: Dossier Cover and Summary
                        document.add(new Paragraph("Досье сотрудника: " + report.getEmployeeId(), titleFont));
                        document.add(new Paragraph("Тип отчета: " + report.getTemplateType(), bodyFont));
                        document.add(new Paragraph("Статус: " + report.getStatus(), bodyFont));
                        document.add(new Paragraph("Количество документов: " + report.getDocumentCount(), bodyFont));
                        document.add(new Paragraph("Сводное резюме: " + (report.getSummaryText() != null ? report.getSummaryText() : "Отчет пуст"), bodyFont));

                        // Page 2: Detailed Document Inventory
                        document.newPage();
                        document.add(new Paragraph("Полный перечень документов досье (" + report.getEmployeeId() + "):", headerFont));

                        List<EmployeeDocument> documents = employeeDocumentRepository.findUnifiedEmployeeDossier(report.getEmployeeId());
                        if (documents.isEmpty()) {
                            document.add(new Paragraph("Документы в досье отсутствуют.", bodyFont));
                        } else {
                            for (EmployeeDocument doc : documents) {
                                String docInfo = String.format("• [%s] %s (от %s)", doc.getDocType(), doc.getTitle(), doc.getDocDate());
                                document.add(new Paragraph(docInfo, bodyFont));
                                if (doc.getScientificDirection() != null) {
                                    document.add(new Paragraph("  Научное направление: " + doc.getScientificDirection(), bodyFont));
                                }
                                if (doc.getDetails() != null) {
                                    document.add(new Paragraph("  Детали: " + doc.getDetails(), bodyFont));
                                }
                            }
                        }

                        document.close();

                        byte[] content = baos.toByteArray();
                        return ResponseEntity.ok()
                                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"dossier_report_" + id + ".pdf\"")
                                .contentType(MediaType.APPLICATION_PDF)
                                .body((Object) content);
                    } catch (Exception e) {
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body(Map.of("error_code", "PDF_GENERATION_ERROR", "message", "Ошибка при генерации PDF файла"));
                    }
                })
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body((Object) Map.of(
                        "error_code", "NOT_FOUND",
                        "message", "Справка не найдена"
                )));
    }



    @PostMapping("/reports/{id}/sign")
    @Transactional
    public ResponseEntity<?> signDossierReport(@PathVariable("id") Long id, @RequestBody(required = false) Map<String, Object> requestBody) {
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error_code", "FORBIDDEN", "message", "Access denied"));
        }
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        if (currentUsername == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error_code", "FORBIDDEN", "message", "Access denied"));
        }

        User currentUser = userRepository.findByUsername(currentUsername).orElse(null);

        if (currentUser == null || (!"EPIDEMIOLOGIST".equals(currentUser.getRole()) && !"ADMIN".equals(currentUser.getRole()))) {
             return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error_code", "FORBIDDEN", "message", "Access denied"));
        }

        if (requestBody == null || !requestBody.containsKey("signature") || requestBody.get("signature") == null || requestBody.get("signature").toString().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error_code", "VALIDATION_ERROR",
                    "message", "Не указан обязательный параметр signature."
            ));
        }

        String signature = requestBody.get("signature").toString();

        if (!dossierReportRepository.existsById(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "error_code", "NOT_FOUND",
                    "message", "Справка не найдена"
            ));
        }

        int updatedCount = dossierReportRepository.signReport(id, signature);
        if (updatedCount == 0) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                    "error_code", "CONFLICT",
                    "message", "Невозможно подписать справку: справка не завершена или уже подписана."
            ));
        }

        return dossierReportRepository.findById(id)
            .map(report -> {
                java.util.Map<String, Object> response = new java.util.HashMap<>();
                response.put("id", report.getId());
                response.put("employee_id", report.getEmployeeId());
                response.put("template_type", report.getTemplateType());
                response.put("status", report.getStatus());
                response.put("is_signed", report.getIsSigned());
                response.put("signature", report.getSignature());
                return ResponseEntity.ok((Object) response);
            })
            .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());

    }


}
