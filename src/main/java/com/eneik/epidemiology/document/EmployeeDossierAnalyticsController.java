package com.eneik.epidemiology.document;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import com.eneik.epidemiology.telemetry.TelemetryService;
import org.springframework.security.core.context.SecurityContextHolder;
import com.eneik.epidemiology.user.User;
import com.eneik.epidemiology.user.UserRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/dossier/analytics")
public class EmployeeDossierAnalyticsController {

    private final EmployeeDocumentRepository employeeDocumentRepository;
    private final DossierReportRepository dossierReportRepository;
    private final TelemetryService telemetryService;
    private final UserRepository userRepository;

    public EmployeeDossierAnalyticsController(EmployeeDocumentRepository employeeDocumentRepository, DossierReportRepository dossierReportRepository, TelemetryService telemetryService, UserRepository userRepository) {
        this.employeeDocumentRepository = employeeDocumentRepository;
        this.dossierReportRepository = dossierReportRepository;
        this.telemetryService = telemetryService;
        this.userRepository = userRepository;
    }

    @GetMapping("/documents")
    public ResponseEntity<?> filterAnalyticsDocuments(
            @RequestParam("employee_id") String employeeId,
            @RequestParam(value = "doc_type", required = false) String docType,
            @RequestParam(value = "scientific_direction", required = false) String scientificDirection,
            @RequestParam(value = "from_date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(value = "to_date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {

        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByUsername(currentUsername).orElse(null);

        List<EmployeeDocument> documents = employeeDocumentRepository.searchEmployeeDocuments(
                employeeId, null, docType, scientificDirection, null, fromDate, toDate
        );

        if (currentUser != null && !"ADMIN".equals(currentUser.getRole())) {
            documents = documents.stream().filter(d -> {
                if (!"STRAIN_ISOLATION".equals(d.getDocType()) && !"REPORT".equals(d.getDocType())) return true;
                if (d.getAccessDepartment() == null && d.getAccessCourse() == null) return true;
                boolean depMatch = d.getAccessDepartment() != null && d.getAccessDepartment().equals(currentUser.getDepartment());
                boolean courseMatch = d.getAccessCourse() != null && currentUser.getCourses() != null && currentUser.getCourses().contains(d.getAccessCourse());
                return depMatch || courseMatch;
            }).toList();
        }

        List<Map<String, Object>> response = documents.stream().map(d -> (Map<String, Object>) Map.<String, Object>of(
                "id", d.getId(),
                "employee_id", d.getEmployeeId(),
                "doc_type", d.getDocType(),
                "title", d.getTitle(),
                "doc_date", d.getDocDate().toString(),
                "employee_surname", d.getEmployeeSurname() != null ? d.getEmployeeSurname() : "",
                "scientific_direction", d.getScientificDirection() != null ? d.getScientificDirection() : ""
        )).toList();

        return ResponseEntity.ok(response);
    }

    @PostMapping("/export")
    @Transactional
    public ResponseEntity<?> exportAnalyticsPdf(@RequestBody Map<String, Object> requestBody) {
        if (!requestBody.containsKey("employee_id")) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error_code", "INVALID_PARAM",
                    "message", "Не указан обязательный параметр employee_id."
            ));
        }

        String employeeId = (String) requestBody.get("employee_id");
        String scientificDirection = (String) requestBody.get("scientific_direction");

        LocalDate fromDate = null;
        if (requestBody.containsKey("from_date") && requestBody.get("from_date") != null) {
            fromDate = LocalDate.parse((String) requestBody.get("from_date"));
        }
        LocalDate toDate = null;
        if (requestBody.containsKey("to_date") && requestBody.get("to_date") != null) {
            toDate = LocalDate.parse((String) requestBody.get("to_date"));
        }

        List<String> docTypes = null;
        if (requestBody.containsKey("doc_types") && requestBody.get("doc_types") != null) {
            docTypes = (List<String>) (Object) requestBody.get("doc_types");
        }

        long startTime = System.currentTimeMillis();
        boolean success = false;
        DossierReport report = null;

        try {
            // we use first doc_type if list provided for simpler query (the repository only supports a single docType)
            String docType = null;

            List<EmployeeDocument> documents = employeeDocumentRepository.searchEmployeeDocuments(
                employeeId, null, docType, scientificDirection, null, fromDate, toDate
            );

            // manual filtering for remaining doc types
            if (docTypes != null && !docTypes.isEmpty()) {
                 List<String> finalDocTypes = docTypes;
                 documents = documents.stream().filter(d -> finalDocTypes.contains(d.getDocType())).toList();
            }

            String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
            User currentUser = userRepository.findByUsername(currentUsername).orElse(null);

            if (currentUser != null && !"ADMIN".equals(currentUser.getRole())) {
                documents = documents.stream().filter(d -> {
                    if (!"STRAIN_ISOLATION".equals(d.getDocType()) && !"REPORT".equals(d.getDocType())) return true;
                    boolean depMatch = d.getAccessDepartment() != null && d.getAccessDepartment().equals(currentUser.getDepartment());
                    boolean courseMatch = d.getAccessCourse() != null && currentUser.getCourses() != null && currentUser.getCourses().contains(d.getAccessCourse());
                    return depMatch || courseMatch;
                }).toList();
            }

            String summaryText = "Сводная аналитическая справка по сотруднику " + employeeId + ": " + documents.size() + " документов.";

            report = new DossierReport(
                    employeeId,
                    "ANALYTICS_EXPORT",
                    "PENDING",
                    null,
                    documents.size(),
                    null
            );
            report = dossierReportRepository.save(report);

            int updatedCount = dossierReportRepository.updateStatus(report.getId(), "PENDING", "COMPLETED");
            if (updatedCount > 0) {
                report.setStatus("COMPLETED");
                report.setSummaryText(summaryText);
                report.setDownloadUrl("/api/v1/dossier/reports/" + report.getId() + "/download");
                report = dossierReportRepository.save(report);
                success = true;
            }
        } finally {
            long processingTime = System.currentTimeMillis() - startTime;
            telemetryService.recordDossierGenerationTelemetry(processingTime, success);
        }

        if (report == null) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "export_id", report.getId(),
                "status", report.getStatus(),
                "download_url", report.getDownloadUrl()
        ));
    }

    @GetMapping("/metrics")
    public ResponseEntity<?> getAnalyticsMetrics(
            @RequestParam("employee_id") String employeeId,
            @RequestParam(value = "scientific_direction", required = false) String scientificDirection) {

        List<EmployeeDocument> documents = employeeDocumentRepository.searchEmployeeDocuments(
                employeeId, null, null, scientificDirection, null, null, null
        );

        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByUsername(currentUsername).orElse(null);

        if (currentUser != null && !"ADMIN".equals(currentUser.getRole())) {
            documents = documents.stream().filter(d -> {
                if (!"STRAIN_ISOLATION".equals(d.getDocType()) && !"REPORT".equals(d.getDocType())) return true;
                if (d.getAccessDepartment() == null && d.getAccessCourse() == null) return true;
                boolean depMatch = d.getAccessDepartment() != null && d.getAccessDepartment().equals(currentUser.getDepartment());
                boolean courseMatch = d.getAccessCourse() != null && currentUser.getCourses() != null && currentUser.getCourses().contains(d.getAccessCourse());
                return depMatch || courseMatch;
            }).toList();
        }

        int denominator = documents.size();
        if (denominator == 0) {
            return ResponseEntity.ok(Map.of(
                    "employee_id", employeeId,
                    "metric_name", "Доля научных отчетов в общем объеме документов",
                    "value", 0.0,
                    "denominator", 0,
                    "lower_bound", 0.0,
                    "upper_bound", 0.0
            ));
        }

        long reportCount = documents.stream().filter(d -> "REPORT".equals(d.getDocType())).count();
        double value = (double) reportCount / denominator;

        // Basic confidence interval mock logic
        double marginOfError = 1.96 * Math.sqrt((value * (1 - value)) / denominator);
        if(Double.isNaN(marginOfError)) {
             marginOfError = 0.0;
        }

        double lowerBound = Math.max(0.0, value - marginOfError);
        double upperBound = Math.min(1.0, value + marginOfError);

        return ResponseEntity.ok(Map.of(
                "employee_id", employeeId,
                "metric_name", "Доля научных отчетов в общем объеме документов",
                "value", value,
                "denominator", denominator,
                "lower_bound", lowerBound,
                "upper_bound", upperBound
        ));
    }

    @GetMapping("/reports/{id}/download")
    public ResponseEntity<?> downloadAnalyticsReport(@PathVariable("id") Long id) {
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByUsername(currentUsername).orElse(null);

        return dossierReportRepository.findById(id)
                .map(report -> {
                    if (currentUser != null && !"ADMIN".equals(currentUser.getRole())) {
                        boolean depMatch = report.getAccessDepartment() != null && report.getAccessDepartment().equals(currentUser.getDepartment());
                        boolean courseMatch = report.getAccessCourse() != null && currentUser.getCourses() != null && currentUser.getCourses().contains(report.getAccessCourse());
                        if (report.getAccessDepartment() != null || report.getAccessCourse() != null) {
                             if (!depMatch && !courseMatch) {
                                  return ResponseEntity.status(HttpStatus.FORBIDDEN).body((Object) Map.of("error_code", "FORBIDDEN", "message", "Access denied"));
                             }
                        }
                    }

                    if (!"COMPLETED".equals(report.getStatus())) {
                        return ResponseEntity.status(HttpStatus.NOT_FOUND).body((Object) Map.of(
                                "error_code", "NOT_FOUND",
                                "message", "Файл справки не найден или еще не сгенерирован"
                        ));
                    }

                    byte[] content;
                    try (java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream()) {
                        com.itextpdf.text.Document pdfDoc = new com.itextpdf.text.Document();
                        com.itextpdf.text.pdf.PdfWriter.getInstance(pdfDoc, out);
                        pdfDoc.open();
                        pdfDoc.add(new com.itextpdf.text.Paragraph(report.getSummaryText() != null ? report.getSummaryText() : "Отчет пуст"));
                        pdfDoc.close();
                        content = out.toByteArray();
                    } catch (Exception e) {
                        content = new byte[0];
                    }
                    return ResponseEntity.ok()
                            .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"dossier_report_" + id + ".pdf\"")
                            .contentType(org.springframework.http.MediaType.APPLICATION_PDF)
                            .body((Object) content);
                })
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body((Object) Map.of(
                        "error_code", "NOT_FOUND",
                        "message", "Справка не найдена"
                )));
    }
}
