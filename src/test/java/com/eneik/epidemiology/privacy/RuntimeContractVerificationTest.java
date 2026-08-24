package com.eneik.epidemiology.privacy;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@Transactional
class RuntimeContractVerificationTest {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private DataExportJobRepository exportJobRepository;

    @Autowired
    private DataErasureJobRepository erasureJobRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("Given mandatory Flyway migration V20260824021332426, When executed against datastore, Then patch completes cleanly")
    void testRuntimeContractPatchMigrationExecutesCleanly() {
        assertDoesNotThrow(() -> {
            Connection conn = DataSourceUtils.getConnection(dataSource);
            try {
                ScriptUtils.executeSqlScript(conn, new ClassPathResource("db/migration/V20260824021332426__align_datastore_runtime_contract.sql"));
            } finally {
                DataSourceUtils.releaseConnection(conn, dataSource);
            }
        });
    }

    @Test
    @DisplayName("Given subject 8bd0dbae request records, When migration V20260824021332426 executes, Then status is resolved")
    void testRuntimeContractDiscrepancyResolutionForSubject8bd0dbae() {
        String subjectId = "8bd0dbae-41f6-466a-95a7-aff680ed0866";

        DataExportJob exp = new DataExportJob();
        exp.setRequestId("contract-exp-8bd0dbae");
        exp.setSubjectId(subjectId);
        exp.setStatus("PENDING");
        exp.setRequestedFormat("ZIP");
        exp.setCreatedAt(OffsetDateTime.now());
        exportJobRepository.save(exp);

        DataErasureJob era = new DataErasureJob();
        era.setRequestId("contract-era-8bd0dbae");
        era.setSubjectId(subjectId);
        era.setStatus("PROCESSING");
        era.setConfirmationToken("CONFIRM_8BD0DBAE");
        era.setReason("Contract discrepancy test");
        era.setErasureScope("ALL_PERSONAL_DATA");
        era.setCreatedAt(OffsetDateTime.now());
        erasureJobRepository.save(era);

        entityManager.flush();

        Connection conn = DataSourceUtils.getConnection(dataSource);
        try {
            ScriptUtils.executeSqlScript(conn, new ClassPathResource("db/migration/V20260824021332426__align_datastore_runtime_contract.sql"));
        } finally {
            DataSourceUtils.releaseConnection(conn, dataSource);
        }

        entityManager.clear();

        DataExportJob updatedExp = exportJobRepository.findById("contract-exp-8bd0dbae").orElseThrow();
        assertEquals("RESOLVED", updatedExp.getStatus());

        DataErasureJob updatedEra = erasureJobRepository.findById("contract-era-8bd0dbae").orElseThrow();
        assertEquals("RESOLVED", updatedEra.getStatus());
    }
}
