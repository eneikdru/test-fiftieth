package com.eneik.epidemiology.privacy;

import com.eneik.epidemiology.user.User;
import com.eneik.epidemiology.user.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.OptimisticLockingFailureException;

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
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Autowired
    public PrivacyService(
        DataExportJobRepository exportJobRepository,
        DataErasureJobRepository erasureJobRepository,
        UserRepository userRepository,
        ObjectMapper objectMapper
    ) {
        this(exportJobRepository, erasureJobRepository, userRepository, objectMapper, Clock.systemUTC());
    }

    public PrivacyService(
        DataExportJobRepository exportJobRepository,
        DataErasureJobRepository erasureJobRepository,
        UserRepository userRepository,
        ObjectMapper objectMapper,
        Clock clock
    ) {
        this.exportJobRepository = exportJobRepository;
        this.erasureJobRepository = erasureJobRepository;
        this.userRepository = userRepository;
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

        exportJobRepository.save(job);

        return processExportJob(job, user, requestedFormat);
    }

    private DataExportJob processExportJob(DataExportJob job, User user, String format) {
        try {
            Map<String, Object> userDataMap = new LinkedHashMap<>();
            userDataMap.put("id", user.getId());
            userDataMap.put("username", user.getUsername());
            userDataMap.put("role", user.getRole());
            userDataMap.put("created_at", user.getCreatedAt() != null ? user.getCreatedAt().toString() : null);

            String payloadJson = objectMapper.writeValueAsString(userDataMap);
            String downloadUrl = "/api/v1/privacy/export-requests/" + job.getRequestId() + "/download";
            OffsetDateTime now = OffsetDateTime.now(clock);

            int rowsUpdated = exportJobRepository.updateStatusToCompleted(
                job.getRequestId(), "PENDING", "COMPLETED", downloadUrl, payloadJson, now, now.plusDays(7)
            );
            if (rowsUpdated == 0) {
                throw new OptimisticLockingFailureException("Export job status was modified concurrently");
            }
            return exportJobRepository.findById(job.getRequestId()).orElseThrow();
        } catch (Exception e) {
            int rowsUpdated = exportJobRepository.updateStatusToFailed(
                job.getRequestId(), "PENDING", "FAILED", "EXPORT_PROCESSING_ERROR", "Ошибка при формировании экспортного пакета данных."
            );
            if (rowsUpdated == 0) {
                throw new OptimisticLockingFailureException("Export job status was modified concurrently");
            }
            return exportJobRepository.findById(job.getRequestId()).orElseThrow();
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

        erasureJobRepository.save(job);

        // Execute permanent removal of identifiable user data from database
        userRepository.delete(user);

        int rowsUpdated = erasureJobRepository.updateStatusToCompleted(
            job.getRequestId(), "PENDING", "COMPLETED", 1, OffsetDateTime.now(clock)
        );
        if (rowsUpdated == 0) {
            throw new OptimisticLockingFailureException("Erasure job status was modified concurrently");
        }
        return erasureJobRepository.findById(job.getRequestId()).orElseThrow();
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
