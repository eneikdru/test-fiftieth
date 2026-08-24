package com.eneik.epidemiology.privacy;

import com.eneik.epidemiology.user.User;
import com.eneik.epidemiology.user.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.OptimisticLockingFailureException;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@SpringBootTest
@Transactional
class PrivacyServiceUpdateAtomicPatchTest {

    @SpyBean
    private DataExportJobRepository exportJobRepository;

    @SpyBean
    private DataErasureJobRepository erasureJobRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private PrivacyService privacyService;
    private final Clock fixedClock = Clock.fixed(Instant.parse("2026-08-22T15:00:00Z"), ZoneId.of("UTC"));

    @BeforeEach
    void setUp() {
        privacyService = new PrivacyService(
            exportJobRepository,
            erasureJobRepository,
            userRepository,
            objectMapper,
            fixedClock
        );
    }

    @Test
    @DisplayName("Given valid export, When completed, Then atomic status update method is used")
    void testDataExportUsesAtomicUpdate() {
        User user = new User("atomic_export_user", "hash", "RESEARCHER");
        user = userRepository.save(user);

        DataExportJob job = privacyService.initiateDataExport(user.getUsername(), "ZIP", "Note");

        verify(exportJobRepository).updateStatusToCompleted(
            eq(job.getRequestId()), eq("PENDING"), eq("COMPLETED"), any(), any(), any(), any()
        );
        assertEquals("COMPLETED", job.getStatus());
    }

    @Test
    @DisplayName("Given concurrent modification, When completing export, Then OptimisticLockingFailureException is thrown")
    void testDataExportOptimisticLockingFailure() {
        User user = new User("concurrent_export_user", "hash", "RESEARCHER");
        user = userRepository.save(user);
        final String username = user.getUsername();

        doReturn(0).when(exportJobRepository).updateStatusToCompleted(any(), any(), any(), any(), any(), any(), any());
        assertThrows(OptimisticLockingFailureException.class, () -> {
            privacyService.initiateDataExport(username, "ZIP", "Note");
        });
    }

    @Test
    @DisplayName("Given valid erasure, When completed, Then atomic status update method is used")
    void testDataErasureUsesAtomicUpdate() {
        User user = new User("atomic_erasure_user", "hash", "RESEARCHER");
        user = userRepository.save(user);

        String token = "CONFIRM_ERASURE_atomic_erasure_user";
        DataErasureJob job = privacyService.initiateDataErasure("atomic_erasure_user", token, "Reason", "ALL");

        verify(erasureJobRepository).updateStatusToCompleted(
            eq(job.getRequestId()), eq("PENDING"), eq("COMPLETED"), eq(1), any()
        );
        assertEquals("COMPLETED", job.getStatus());
    }

    @Test
    @DisplayName("Given concurrent modification, When completing erasure, Then OptimisticLockingFailureException is thrown")
    void testDataErasureOptimisticLockingFailure() {
        User user = new User("concurrent_erasure_user", "hash", "RESEARCHER");
        user = userRepository.save(user);
        final String username = user.getUsername();

        doReturn(0).when(erasureJobRepository).updateStatusToCompleted(any(), any(), any(), eq(1), any());
        assertThrows(OptimisticLockingFailureException.class, () -> {
            privacyService.initiateDataErasure(username, "CONFIRM_ERASURE_" + username, "Reason", "ALL");
        });
    }
}
