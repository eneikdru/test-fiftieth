package com.eneik.epidemiology.privacy;

import com.eneik.epidemiology.document.DossierReport;
import com.eneik.epidemiology.document.DossierReportRepository;
import com.eneik.epidemiology.document.EmployeeDocument;
import com.eneik.epidemiology.document.EmployeeDocumentRepository;
import com.eneik.epidemiology.user.User;
import com.eneik.epidemiology.user.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class PrivacyService {

    private final DataExportJobRepository exportJobRepository;
    private final DataErasureJobRepository erasureJobRepository;
    private final UserRepository userRepository;
    private final EmployeeDocumentRepository employeeDocumentRepository;
    private final DossierReportRepository dossierReportRepository;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Autowired
    public PrivacyService(
        DataExportJobRepository exportJobRepository,
        DataErasureJobRepository erasureJobRepository,
        UserRepository userRepository,
        EmployeeDocumentRepository employeeDocumentRepository,
        DossierReportRepository dossierReportRepository,
        ObjectMapper objectMapper
    ) {
        this(exportJobRepository, erasureJobRepository, userRepository, employeeDocumentRepository, dossierReportRepository, objectMapper, Clock.systemUTC());
    }

    public PrivacyService(
        DataExportJobRepository exportJobRepository,
        DataErasureJobRepository erasureJobRepository,
        UserRepository userRepository,
        ObjectMapper objectMapper,
        Clock clock
    ) {
        this(exportJobRepository, erasureJobRepository, userRepository, null, null, objectMapper, clock);
    }

    public PrivacyService(
        DataExportJobRepository exportJobRepository,
        DataErasureJobRepository erasureJobRepository,
        UserRepository userRepository,
        EmployeeDocumentRepository employeeDocumentRepository,
        DossierReportRepository dossierReportRepository,
        ObjectMapper objectMapper,
        Clock clock
    ) {
        this.exportJobRepository = exportJobRepository;
        this.erasureJobRepository = erasureJobRepository;
        this.userRepository = userRepository;
        this.employeeDocumentRepository = employeeDocumentRepository;
        this.dossierReportRepository = dossierReportRepository;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Transactional
    public DataExportJob initiateDataExport(String subjectId, String format, String notes) {
        if (subjectId == null || subjectId.isBlank()) {
            throw new PrivacyException("INVALID_SUBJECT_ID", "Идентификатор субъекта данных не может быть пустым.");
        }

        User user = findSubjectUser(subjectId)
            .orElseThrow(() -> new PrivacyException("SUBJECT_NOT_FOUND", "Пользователь с указанным идентификатором не найден."));

        List<DataExportJob> activeJobs = exportJobRepository.findBySubjectIdAndStatusIn(
            user.getUsername(),
            List.of("PENDING", "PROCESSING")
        );
        if (!activeJobs.isEmpty()) {
            throw new PrivacyConflictException("ACTIVE_EXPORT_EXISTS", "Для данного субъекта уже выполняется запрос на выгрузку данных.");
        }

        String requestId = UUID.randomUUID().toString();
        String requestedFormat = (format != null && !format.isBlank()) ? format.toUpperCase() : "ZIP";

        DataExportJob job = new DataExportJob();
        job.setRequestId(requestId);
        job.setSubjectId(user.getUsername());
        job.setStatus("PENDING");
        job.setRequestedFormat(requestedFormat);
        job.setNotes(notes);
        job.setCreatedAt(OffsetDateTime.now(clock));

        exportJobRepository.saveAndFlush(job);

        return processExportJob(job, user, requestedFormat);
    }

    private DataExportJob processExportJob(DataExportJob job, User user, String format) {
        try {
            Map<String, Object> userDataMap = new LinkedHashMap<>();
            userDataMap.put("id", user.getId());
            userDataMap.put("username", user.getUsername());
            userDataMap.put("role", user.getRole());
            userDataMap.put("email", user.getEmail());
            userDataMap.put("full_name", user.getFullName());
            userDataMap.put("moodle_id", user.getMoodleId());
            userDataMap.put("department", user.getDepartment());
            userDataMap.put("courses", user.getCourses());
            userDataMap.put("created_at", user.getCreatedAt() != null ? user.getCreatedAt().toString() : null);

            if (employeeDocumentRepository != null) {
                List<EmployeeDocument> docs = employeeDocumentRepository.findByEmployeeIdOrderByDocDateDesc(user.getUsername());
                List<Map<String, Object>> docList = new ArrayList<>();
                for (EmployeeDocument doc : docs) {
                    Map<String, Object> docMap = new LinkedHashMap<>();
                    docMap.put("id", doc.getId());
                    docMap.put("employee_id", doc.getEmployeeId());
                    docMap.put("employee_surname", doc.getEmployeeSurname());
                    docMap.put("doc_type", doc.getDocType());
                    docMap.put("title", doc.getTitle());
                    docMap.put("doc_date", doc.getDocDate() != null ? doc.getDocDate().toString() : null);
                    docMap.put("details", doc.getDetails());
                    docMap.put("scientific_direction", doc.getScientificDirection());
                    docMap.put("created_at", doc.getCreatedAt() != null ? doc.getCreatedAt().toString() : null);
                    docList.add(docMap);
                }
                userDataMap.put("dossier_documents", docList);
            }

            if (dossierReportRepository != null) {
                List<DossierReport> reports = dossierReportRepository.findByEmployeeId(user.getUsername());
                List<Map<String, Object>> reportList = new ArrayList<>();
                for (DossierReport r : reports) {
                    Map<String, Object> rMap = new LinkedHashMap<>();
                    rMap.put("id", r.getId());
                    rMap.put("employee_id", r.getEmployeeId());
                    rMap.put("template_type", r.getTemplateType());
                    rMap.put("status", r.getStatus());
                    rMap.put("summary_text", r.getSummaryText());
                    rMap.put("document_count", r.getDocumentCount());
                    rMap.put("download_url", r.getDownloadUrl());
                    rMap.put("created_at", r.getCreatedAt() != null ? r.getCreatedAt().toString() : null);
                    reportList.add(rMap);
                }
                userDataMap.put("dossier_reports", reportList);
            }

            String payloadJson = objectMapper.writeValueAsString(userDataMap);
            String downloadUrl = "/api/v1/privacy/export-requests/" + job.getRequestId() + "/download";
            OffsetDateTime now = OffsetDateTime.now(clock);
            OffsetDateTime expiresAt = now.plusDays(7);

            job.setStatus("COMPLETED");
            job.setDownloadUrl(downloadUrl);
            job.setExportPayload(payloadJson);
            job.setCompletedAt(now);
            job.setExpiresAt(expiresAt);

            int updated = exportJobRepository.updateStatusToCompleted(
                job.getRequestId(),
                "PENDING",
                "COMPLETED",
                downloadUrl,
                payloadJson,
                now,
                expiresAt
            );
            if (updated == 0) {
                exportJobRepository.save(job);
            }
            return job;
        } catch (Exception e) {
            e.printStackTrace();
            job.setStatus("FAILED");
            job.setErrorCode("EXPORT_PROCESSING_ERROR");
            job.setErrorMessage("Ошибка при формировании экспортного пакета данных: " + e.getMessage());
            return exportJobRepository.save(job);
        }
    }

    @Transactional(readOnly = true)
    public DataExportJob getExportJobStatus(String requestId) {
        return exportJobRepository.findById(requestId)
            .orElseThrow(() -> new PrivacyNotFoundException("EXPORT_NOT_FOUND", "Запрос на выгрузку данных не найден."));
    }

    @Transactional(readOnly = true)
    public DownloadData getExportDownloadData(String requestId) {
        DataExportJob job = getExportJobStatus(requestId);

        if (!"COMPLETED".equals(job.getStatus())) {
            throw new PrivacyException("EXPORT_NOT_READY", "Выгрузка данных еще не завершена или завершилась с ошибкой.");
        }

        if (job.getExpiresAt() != null && OffsetDateTime.now(clock).isAfter(job.getExpiresAt())) {
            throw new PrivacyGoneException("DOWNLOAD_EXPIRED", "Ссылка на скачивание выгрузки данных истекла.");
        }

        try {
            byte[] contentBytes;
            String mediaType;

            if ("JSON".equalsIgnoreCase(job.getRequestedFormat())) {
                contentBytes = job.getExportPayload().getBytes(StandardCharsets.UTF_8);
                mediaType = "application/json";
            } else {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                try (ZipOutputStream zos = new ZipOutputStream(baos)) {
                    ZipEntry zipEntry = new ZipEntry("personal_data.json");
                    zos.putNextEntry(zipEntry);
                    zos.write(job.getExportPayload().getBytes(StandardCharsets.UTF_8));
                    zos.closeEntry();
                }
                contentBytes = baos.toByteArray();
                mediaType = "application/zip";
            }

            return new DownloadData(contentBytes, mediaType);
        } catch (Exception e) {
            throw new PrivacyException("DOWNLOAD_ERROR", "Ошибка при подготовке файла для скачивания.");
        }
    }

    @Transactional
    public DataErasureJob initiateDataErasure(String subjectId, String confirmationToken, String reason, String scope) {
        if (subjectId == null || subjectId.isBlank()) {
            throw new PrivacyException("INVALID_SUBJECT_ID", "Идентификатор субъекта данных не может быть пустым.");
        }

        User user = findSubjectUser(subjectId)
            .orElseThrow(() -> new PrivacyNotFoundException("SUBJECT_NOT_FOUND", "Пользователь с указанным идентификатором не найден."));

        String expectedToken = "CONFIRM_ERASURE_" + subjectId;
        String expectedTokenByUsername = "CONFIRM_ERASURE_" + user.getUsername();
        if (confirmationToken == null || (!confirmationToken.equals(expectedToken) && !confirmationToken.equals(expectedTokenByUsername))) {
            throw new PrivacyBadRequestException("INVALID_CONFIRMATION_TOKEN", "Неверный токен подтверждения удаления данных.");
        }

        List<DataErasureJob> activeJobs = erasureJobRepository.findBySubjectIdAndStatusIn(
            user.getUsername(),
            List.of("PENDING", "PROCESSING")
        );
        if (!activeJobs.isEmpty()) {
            throw new PrivacyConflictException("ACTIVE_ERASURE_EXISTS", "Для данного субъекта уже выполняется запрос на удаление данных.");
        }

        String requestId = UUID.randomUUID().toString();
        String erasureScope = (scope != null && !scope.isBlank()) ? scope : "ALL_PERSONAL_DATA";

        DataErasureJob job = new DataErasureJob();
        job.setRequestId(requestId);
        job.setSubjectId(user.getUsername());
        job.setStatus("PENDING");
        job.setConfirmationToken(confirmationToken);
        job.setReason(reason != null ? reason : "Запрос на удаление персональных данных (152-ФЗ)");
        job.setErasureScope(erasureScope);
        job.setCreatedAt(OffsetDateTime.now(clock));

        erasureJobRepository.saveAndFlush(job);

        int totalErased = 1; // user account

        if (employeeDocumentRepository != null) {
            List<EmployeeDocument> docs = employeeDocumentRepository.findByEmployeeIdOrderByDocDateDesc(user.getUsername());
            if (!docs.isEmpty()) {
                totalErased += docs.size();
                employeeDocumentRepository.deleteAll(docs);
                employeeDocumentRepository.flush();
            }
        }

        if (dossierReportRepository != null) {
            List<DossierReport> reports = dossierReportRepository.findByEmployeeId(user.getUsername());
            if (!reports.isEmpty()) {
                totalErased += reports.size();
                dossierReportRepository.deleteAll(reports);
                dossierReportRepository.flush();
            }
        }

        // Execute permanent removal of identifiable user data from database
        userRepository.delete(user);
        userRepository.flush();

        OffsetDateTime completedAt = OffsetDateTime.now(clock);
        job.setStatus("COMPLETED");
        job.setRecordsErasedCount(totalErased);
        job.setCompletedAt(completedAt);

        int updated = erasureJobRepository.updateStatusToCompleted(
            job.getRequestId(),
            "PENDING",
            "COMPLETED",
            totalErased,
            completedAt
        );
        if (updated == 0) {
            erasureJobRepository.save(job);
        }

        return job;
    }

    @Transactional(readOnly = true)
    public DataErasureJob getErasureJobStatus(String requestId) {
        return erasureJobRepository.findById(requestId)
            .orElseThrow(() -> new PrivacyNotFoundException("ERASURE_NOT_FOUND", "Запрос на удаление данных не найден."));
    }

    private Optional<User> findSubjectUser(String subjectId) {
        try {
            Long id = Long.parseLong(subjectId);
            Optional<User> byId = userRepository.findById(id);
            if (byId.isPresent()) {
                return byId;
            }
        } catch (NumberFormatException ignored) {
        }
        return userRepository.findByUsername(subjectId);
    }

    public record DownloadData(byte[] bytes, String mediaType) {}

    // Exception hierarchy for privacy handling
    public static class PrivacyException extends RuntimeException {
        private final String errorCode;

        public PrivacyException(String errorCode, String message) {
            super(message);
            this.errorCode = errorCode;
        }

        public String getErrorCode() {
            return errorCode;
        }
    }

    public static class PrivacyNotFoundException extends PrivacyException {
        public PrivacyNotFoundException(String errorCode, String message) {
            super(errorCode, message);
        }
    }

    public static class PrivacyBadRequestException extends PrivacyException {
        public PrivacyBadRequestException(String errorCode, String message) {
            super(errorCode, message);
        }
    }

    public static class PrivacyConflictException extends PrivacyException {
        public PrivacyConflictException(String errorCode, String message) {
            super(errorCode, message);
        }
    }

    public static class PrivacyGoneException extends PrivacyException {
        public PrivacyGoneException(String errorCode, String message) {
            super(errorCode, message);
        }
    }
}
