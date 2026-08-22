package com.eneik.epidemiology;

import com.eneik.epidemiology.domain.JobStatus;
import com.eneik.epidemiology.domain.UserProfile;
import com.eneik.epidemiology.dto.DataErasureRequest;
import com.eneik.epidemiology.dto.DataExportRequest;
import com.eneik.epidemiology.repository.DataErasureRequestRepository;
import com.eneik.epidemiology.repository.DataExportRequestRepository;
import com.eneik.epidemiology.repository.UserProfileRepository;
import com.eneik.epidemiology.service.PrivacyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.io.ByteArrayInputStream;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.UUID;
import java.util.zip.ZipInputStream;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class PrivacyIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private DataExportRequestRepository exportRequestRepository;

    @Autowired
    private DataErasureRequestRepository erasureRequestRepository;

    @Autowired
    private PrivacyService privacyService;

    private final Clock fixedClock = Clock.fixed(Instant.parse("2026-08-22T23:00:00Z"), ZoneId.of("UTC"));
    private final UUID fixedUuid = UUID.fromString("a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11");

    @BeforeEach
    void setUp() {
        exportRequestRepository.deleteAll();
        erasureRequestRepository.deleteAll();
        userProfileRepository.deleteAll();

        privacyService.setClock(fixedClock);
        privacyService.setUuidGenerator(() -> fixedUuid);
    }

    @Test
    void testDataExportFlow() throws Exception {
        // Seed user profile
        UserProfile user = new UserProfile(
                "usr_100",
                "Иван Иванов",
                "ivan@epidemiology.ru",
                "НИИ Эпидемиологии",
                OffsetDateTime.now(fixedClock)
        );
        userProfileRepository.save(user);

        // 1. Submit export request
        String requestJson = """
                {
                    "subject_id": "usr_100",
                    "requested_format": "ZIP",
                    "notes": "Экспорт личных данных по запросу"
                }
                """;

        mockMvc.perform(post("/api/v1/privacy/export-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.request_id", equalTo("a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11")))
                .andExpect(jsonPath("$.subject_id", equalTo("usr_100")))
                .andExpect(jsonPath("$.status", equalTo("COMPLETED")))
                .andExpect(jsonPath("$.download_url", equalTo("/api/v1/privacy/export-requests/a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11/download")));

        // 2. Check export job status
        mockMvc.perform(get("/api/v1/privacy/export-requests/{requestId}", fixedUuid))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.request_id", equalTo("a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11")))
                .andExpect(jsonPath("$.status", equalTo("COMPLETED")));

        // 3. Download export package
        byte[] downloadBytes = mockMvc.perform(get("/api/v1/privacy/export-requests/{requestId}/download", fixedUuid))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/octet-stream"))
                .andReturn()
                .getResponse()
                .getContentAsByteArray();

        assertNotNull(downloadBytes);

        // Verify ZIP content
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(downloadBytes))) {
            var entry = zis.getNextEntry();
            assertNotNull(entry);
            assertEquals("personal_data.json", entry.getName());
            String jsonContent = new String(zis.readAllBytes());
            assert(jsonContent.contains("usr_100"));
            assert(jsonContent.contains("Иван Иванов"));
        }
    }

    @Test
    void testDataErasureFlow() throws Exception {
        // Seed user profile
        UserProfile user = new UserProfile(
                "usr_200",
                "Петр Петров",
                "petr@epidemiology.ru",
                "НИИ Эпидемиологии",
                OffsetDateTime.now(fixedClock)
        );
        userProfileRepository.save(user);

        // 1. Submit erasure request with valid confirmation token
        String erasureJson = """
                {
                    "subject_id": "usr_200",
                    "confirmation_token": "CONFIRM_ERASURE_USR_200",
                    "reason": "Отозвано согласие на обработку персональных данных (152-ФЗ)",
                    "erasure_scope": "ALL_PERSONAL_DATA"
                }
                """;

        mockMvc.perform(post("/api/v1/privacy/erasure-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(erasureJson))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.request_id", equalTo("a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11")))
                .andExpect(jsonPath("$.subject_id", equalTo("usr_200")))
                .andExpect(jsonPath("$.status", equalTo("COMPLETED")))
                .andExpect(jsonPath("$.records_erased_count", equalTo(1)));

        // Verify profile permanently deleted from DB
        assertFalse(userProfileRepository.existsById("usr_200"));

        // 2. Query erasure status endpoint
        mockMvc.perform(get("/api/v1/privacy/erasure-requests/{requestId}", fixedUuid))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", equalTo("COMPLETED")))
                .andExpect(jsonPath("$.records_erased_count", equalTo(1)));
    }

    @Test
    void testInvalidConfirmationTokenReturns400() throws Exception {
        String erasureJson = """
                {
                    "subject_id": "usr_300",
                    "confirmation_token": "WRONG_TOKEN",
                    "reason": "Тест"
                }
                """;

        mockMvc.perform(post("/api/v1/privacy/erasure-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(erasureJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error_code", equalTo("INVALID_CONFIRMATION_TOKEN")))
                .andExpect(jsonPath("$.message", equalTo("Неверный токен подтверждения удаления данных.")));
    }
}
