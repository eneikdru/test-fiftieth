package com.eneik.epidemiology.feature;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class SixSigmaAuditService {

    private static final Logger log = LoggerFactory.getLogger(SixSigmaAuditService.class);

    private String getActiveProjectId() {
        return "project-123";
    }

    public BigDecimal calculateFullSixSigmaAudit() {
        return calculateProjectSixSigmaAudit(getActiveProjectId());
    }

    public BigDecimal calculateProjectSixSigmaAudit(String projectId) {
        if (projectId == null) {
            projectId = getActiveProjectId();
        }
        return calculateSixSigmaAuditInternal(projectId);
    }

    private BigDecimal calculateSixSigmaAuditInternal(String targetProjectId) {
        if (targetProjectId == null) {
            log.info("Calculating full cross-project Six Sigma audit");
            return new BigDecimal("99.99966");
        } else {
            log.info("Calculating Six Sigma audit for project {}", targetProjectId);
            return new BigDecimal("95.00000");
        }
    }
}
