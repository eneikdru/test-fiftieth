package com.eneik.epidemiology.privacy;

import com.eneik.epidemiology.user.User;
import com.eneik.epidemiology.user.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class DataSubjectRightsDeepVerificationTest {

    @Autowired
    private DataExportJobRepository exportJobRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PrivacyService privacyService;

    @Test
    @DisplayName("Given an active user, When erasure is requested with invalid token, Then an exception is thrown and data remains")
    void testErasureWithInvalidToken() {
        User user = new User("rights_erasure_fail", "hashed", "RESEARCHER");
        user = userRepository.save(user);

        assertThrows(PrivacyService.PrivacyBadRequestException.class, () -> {
            privacyService.initiateDataErasure("rights_erasure_fail", "INVALID_TOKEN", "Reason", "ALL");
        });

        assertTrue(userRepository.findByUsername("rights_erasure_fail").isPresent());
    }

    @Test
    @DisplayName("Given an export request, When checked before completion, Then the download data is not available")
    void testExportDownloadNotAvailableBeforeCompletion() {
        User user = new User("rights_export_fail", "hashed", "ADMIN");
        user = userRepository.save(user);

        DataExportJob job = new DataExportJob();
        job.setRequestId("test-export-fail");
        job.setSubjectId("rights_export_fail");
        job.setStatus("PENDING");
        exportJobRepository.save(job);

        assertThrows(PrivacyService.PrivacyException.class, () -> {
            privacyService.getExportDownloadData("test-export-fail");
        });
    }
}
