package com.eneik.epidemiology.service;

import com.eneik.epidemiology.domain.DataErasureRequestEntity;
import com.eneik.epidemiology.domain.DataExportRequestEntity;
import com.eneik.epidemiology.domain.JobStatus;
import com.eneik.epidemiology.domain.UserProfile;
import com.eneik.epidemiology.dto.DataErasureJobResponse;
import com.eneik.epidemiology.dto.DataErasureRequest;
import com.eneik.epidemiology.dto.DataExportJobResponse;
import com.eneik.epidemiology.dto.DataExportRequest;
import com.eneik.epidemiology.dto.ErrorDetails;
import com.eneik.epidemiology.exception.PrivacyException;
import com.eneik.epidemiology.repository.DataErasureRequestRepository;
import com.eneik.epidemiology.repository.DataExportRequestRepository;
import com.eneik.epidemiology.repository.UserProfileRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class PrivacyService {

    private final UserProfileRepository userProfileRepository;
    private final DataExportRequestRepository exportRequestRepository;
    private final DataErasureRequestRepository erasureRequestRepository;
    private final ObjectMapper objectMapper;
    private Clock clock;
    private Supplier<UUID> uuidGenerator;

    public PrivacyService(UserProfileRepository userProfileRepository,
                          DataExportRequestRepository exportRequestRepository,
                          DataErasureRequestRepository erasureRequestRepository,
                          ObjectMapper objectMapper) {
        this.userProfileRepository = userProfileRepository;
        this.exportRequestRepository = exportRequestRepository;
        this.erasureRequestRepository = erasureRequestRepository;
        this.objectMapper = objectMapper;
        this.clock = Clock.systemUTC();
        this.uuidGenerator = UUID::randomUUID;
    }

    public void setClock(Clock clock) {
        this.clock = clock;
    }

    public void setUuidGenerator(Supplier<UUID> uuidGenerator) {
        this.uuidGenerator = uuidGenerator;
    }

    @Transactional
    public DataExportJobResponse createExportRequest(DataExportRequest request) {
        // Check for active export request in progress
        List<JobStatus> activeStatuses = Arrays.asList(JobStatus.PENDING, JobStatus.PROCESSING);
        Optional<DataExportRequestEntity> activeRequest = exportRequestRepository
                .findBySubjectIdAndStatusIn(request.getSubjectId(), activeStatuses);
        if (activeRequest.isPresent()) {
            throw new PrivacyException(
                    "EXPORT_IN_PROGRESS",
                    "Запрос на экспорт данных уже обрабатывается для данного субъекта.",
                    409
            );
        }

        UUID requestId = uuidGenerator.get();
        OffsetDateTime now = OffsetDateTime.now(clock);
        String downloadUrl = "/api/v1/privacy/export-requests/" + requestId + "/download";
        OffsetDateTime expiresAt = now.plusDays(7);

        DataExportRequestEntity entity = new DataExportRequestEntity();
        entity.setRequestId(requestId);
        entity.setSubjectId(request.getSubjectId());
        entity.setStatus(JobStatus.COMPLETED);
        entity.setRequestedFormat(request.getRequestedFormat() != null ? request.getRequestedFormat() : "ZIP");
        entity.setNotes(request.getNotes());
        entity.setDownloadUrl(downloadUrl);
        entity.setCreatedAt(now);
        entity.setCompletedAt(now);
        entity.setExpiresAt(expiresAt);

        DataExportRequestEntity saved = exportRequestRepository.save(entity);

        return toExportJobResponse(saved);
    }

    public DataExportJobResponse getExportJobStatus(UUID requestId) {
        DataExportRequestEntity entity = exportRequestRepository.findById(requestId)
                .orElseThrow(() -> new PrivacyException("EXPORT_NOT_FOUND", "Запрос на экспорт данных не найден.", 404));
        return toExportJobResponse(entity);
    }

    public byte[] downloadExportPackage(UUID requestId) {
        DataExportRequestEntity entity = exportRequestRepository.findById(requestId)
                .orElseThrow(() -> new PrivacyException("EXPORT_NOT_FOUND", "Запрос на экспорт данных не найден.", 404));

        if (entity.getStatus() != JobStatus.COMPLETED) {
            throw new PrivacyException("EXPORT_NOT_READY", "Экспорт данных еще не завершен.", 400);
        }

        OffsetDateTime now = OffsetDateTime.now(clock);
        if (entity.getExpiresAt() != null && entity.getExpiresAt().isBefore(now)) {
            throw new PrivacyException("LINK_EXPIRED", "Срок действия ссылки на скачивание истек.", 410);
        }

        Optional<UserProfile> profileOpt = userProfileRepository.findById(entity.getSubjectId());

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ZipOutputStream zos = new ZipOutputStream(baos)) {

            ZipEntry entry = new ZipEntry("personal_data.json");
            zos.putNextEntry(entry);

            byte[] jsonBytes;
            if (profileOpt.isPresent()) {
                UserProfile profile = profileOpt.get();
                jsonBytes = objectMapper.writeValueAsBytes(profile);
            } else {
                Map<String, String> payload = new HashMap<>();
                payload.put("subject_id", entity.getSubjectId());
                payload.put("status", "NO_PROFILE_DATA_FOUND");
                jsonBytes = objectMapper.writeValueAsBytes(payload);
            }

            zos.write(jsonBytes);
            zos.closeEntry();
            zos.finish();

            return baos.toByteArray();
        } catch (IOException e) {
            throw new PrivacyException("EXPORT_FAILED", "Ошибка при формировании архива персональных данных.", 500);
        }
    }

    @Transactional
    public DataErasureJobResponse createErasureRequest(DataErasureRequest request) {
        String expectedToken = "CONFIRM_ERASURE_" + request.getSubjectId().toUpperCase();
        if (!expectedToken.equals(request.getConfirmationToken())) {
            throw new PrivacyException(
                    "INVALID_CONFIRMATION_TOKEN",
                    "Неверный токен подтверждения удаления данных.",
                    400
            );
        }

        List<JobStatus> activeStatuses = Arrays.asList(JobStatus.PENDING, JobStatus.PROCESSING);
        Optional<DataErasureRequestEntity> activeRequest = erasureRequestRepository
                .findBySubjectIdAndStatusIn(request.getSubjectId(), activeStatuses);
        if (activeRequest.isPresent()) {
            throw new PrivacyException(
                    "ERASURE_IN_PROGRESS",
                    "Запрос на удаление данных уже обрабатывается для данного субъекта.",
                    409
            );
        }

        UUID requestId = uuidGenerator.get();
        OffsetDateTime now = OffsetDateTime.now(clock);

        int recordsErased = 0;
        if (userProfileRepository.existsById(request.getSubjectId())) {
            userProfileRepository.deleteById(request.getSubjectId());
            recordsErased = 1;
        }

        DataErasureRequestEntity entity = new DataErasureRequestEntity();
        entity.setRequestId(requestId);
        entity.setSubjectId(request.getSubjectId());
        entity.setStatus(JobStatus.COMPLETED);
        entity.setConfirmationToken(request.getConfirmationToken());
        entity.setReason(request.getReason());
        entity.setErasureScope(request.getErasureScope() != null ? request.getErasureScope() : "ALL_PERSONAL_DATA");
        entity.setCreatedAt(now);
        entity.setCompletedAt(now);
        entity.setRecordsErasedCount(recordsErased);

        DataErasureRequestEntity saved = erasureRequestRepository.save(entity);

        return toErasureJobResponse(saved);
    }

    public DataErasureJobResponse getErasureJobStatus(UUID requestId) {
        DataErasureRequestEntity entity = erasureRequestRepository.findById(requestId)
                .orElseThrow(() -> new PrivacyException("ERASURE_NOT_FOUND", "Запрос на удаление данных не найден.", 404));
        return toErasureJobResponse(entity);
    }

    private DataExportJobResponse toExportJobResponse(DataExportRequestEntity entity) {
        DataExportJobResponse dto = new DataExportJobResponse();
        dto.setRequestId(entity.getRequestId());
        dto.setSubjectId(entity.getSubjectId());
        dto.setStatus(entity.getStatus());
        dto.setDownloadUrl(entity.getDownloadUrl());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setCompletedAt(entity.getCompletedAt());
        dto.setExpiresAt(entity.getExpiresAt());

        if (entity.getErrorCode() != null) {
            dto.setErrorDetails(new ErrorDetails(entity.getErrorCode(), entity.getErrorMessage()));
        }
        return dto;
    }

    private DataErasureJobResponse toErasureJobResponse(DataErasureRequestEntity entity) {
        DataErasureJobResponse dto = new DataErasureJobResponse();
        dto.setRequestId(entity.getRequestId());
        dto.setSubjectId(entity.getSubjectId());
        dto.setStatus(entity.getStatus());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setCompletedAt(entity.getCompletedAt());
        dto.setRecordsErasedCount(entity.getRecordsErasedCount());

        if (entity.getErrorCode() != null) {
            dto.setErrorDetails(new ErrorDetails(entity.getErrorCode(), entity.getErrorMessage()));
        }
        return dto;
    }
}
