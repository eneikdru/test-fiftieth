package com.eneik.epidemiology.privacy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DataSubjectRightsContractTest {

    private static final String CONTRACT_PATH = "docs/contracts/DataSubjectRights.openapi.yaml";

    @Test
    @DisplayName("Given the Data Subject Rights OpenAPI contract file, When parsed, Then it exists and is a valid OpenAPI 3.0 specification")
    void testContractFileExistsAndIsValidOpenApi() throws Exception {
        File contractFile = new File(CONTRACT_PATH);
        assertTrue(contractFile.exists(), "Contract file docs/contracts/DataSubjectRights.openapi.yaml must exist");

        Yaml yaml = new Yaml();
        Map<String, Object> openApiSpec;
        try (InputStream is = new FileInputStream(contractFile)) {
            openApiSpec = yaml.load(is);
        }

        assertNotNull(openApiSpec, "Contract YAML must be parseable");
        assertTrue(openApiSpec.containsKey("openapi"), "Spec must contain 'openapi' version declaration");
        assertTrue(openApiSpec.get("openapi").toString().startsWith("3.0"), "OpenAPI version must be 3.0.x");

        Map<String, Object> info = (Map<String, Object>) openApiSpec.get("info");
        assertNotNull(info, "Spec must contain 'info' section");
        assertEquals("Data Subject Rights API Contract", info.get("title"));
    }

    @Test
    @DisplayName("Given the OpenAPI spec, When endpoints are inspected, Then all export and erasure endpoints are explicitly defined")
    void testExportAndErasureEndpointsAreDefined() throws Exception {
        File contractFile = new File(CONTRACT_PATH);
        Yaml yaml = new Yaml();
        Map<String, Object> openApiSpec;
        try (InputStream is = new FileInputStream(contractFile)) {
            openApiSpec = yaml.load(is);
        }

        Map<String, Object> paths = (Map<String, Object>) openApiSpec.get("paths");
        assertNotNull(paths, "Spec must contain 'paths' definitions");

        assertTrue(paths.containsKey("/privacy/export-requests"), "Must define POST /privacy/export-requests endpoint");
        assertTrue(paths.containsKey("/privacy/export-requests/{requestId}"), "Must define GET /privacy/export-requests/{requestId} endpoint");
        assertTrue(paths.containsKey("/privacy/export-requests/{requestId}/download"), "Must define GET /privacy/export-requests/{requestId}/download endpoint");

        assertTrue(paths.containsKey("/privacy/erasure-requests"), "Must define POST /privacy/erasure-requests endpoint");
        assertTrue(paths.containsKey("/privacy/erasure-requests/{requestId}"), "Must define GET /privacy/erasure-requests/{requestId} endpoint");
    }

    @Test
    @DisplayName("Given the OpenAPI spec, When schema components are inspected, Then request and response payloads are explicit and grounded in the domain model")
    void testPayloadStructuresAreExplicit() throws Exception {
        File contractFile = new File(CONTRACT_PATH);
        Yaml yaml = new Yaml();
        Map<String, Object> openApiSpec;
        try (InputStream is = new FileInputStream(contractFile)) {
            openApiSpec = yaml.load(is);
        }

        Map<String, Object> components = (Map<String, Object>) openApiSpec.get("components");
        assertNotNull(components, "Spec must contain 'components'");

        Map<String, Object> schemas = (Map<String, Object>) components.get("schemas");
        assertNotNull(schemas, "Components must contain 'schemas'");

        // Verify DataExportRequest schema
        assertTrue(schemas.containsKey("DataExportRequest"));
        Map<String, Object> exportReq = (Map<String, Object>) schemas.get("DataExportRequest");
        List<String> exportReqRequired = (List<String>) exportReq.get("required");
        assertTrue(exportReqRequired.contains("subject_id"), "DataExportRequest must require 'subject_id'");

        // Verify DataExportJobResponse schema
        assertTrue(schemas.containsKey("DataExportJobResponse"));
        Map<String, Object> exportResp = (Map<String, Object>) schemas.get("DataExportJobResponse");
        List<String> exportRespRequired = (List<String>) exportResp.get("required");
        assertTrue(exportRespRequired.contains("request_id"));
        assertTrue(exportRespRequired.contains("subject_id"));
        assertTrue(exportRespRequired.contains("status"));
        assertTrue(exportRespRequired.contains("created_at"));

        // Verify DataErasureRequest schema
        assertTrue(schemas.containsKey("DataErasureRequest"));
        Map<String, Object> erasureReq = (Map<String, Object>) schemas.get("DataErasureRequest");
        List<String> erasureReqRequired = (List<String>) erasureReq.get("required");
        assertTrue(erasureReqRequired.contains("subject_id"));
        assertTrue(erasureReqRequired.contains("confirmation_token"));
        assertTrue(erasureReqRequired.contains("reason"));

        // Verify DataErasureJobResponse schema
        assertTrue(schemas.containsKey("DataErasureJobResponse"));
        Map<String, Object> erasureResp = (Map<String, Object>) schemas.get("DataErasureJobResponse");
        List<String> erasureRespRequired = (List<String>) erasureResp.get("required");
        assertTrue(erasureRespRequired.contains("request_id"));
        assertTrue(erasureRespRequired.contains("subject_id"));
        assertTrue(erasureRespRequired.contains("status"));

        // Verify ErrorResponse schema
        assertTrue(schemas.containsKey("ErrorResponse"));
        Map<String, Object> errorResp = (Map<String, Object>) schemas.get("ErrorResponse");
        List<String> errorRespRequired = (List<String>) errorResp.get("required");
        assertTrue(errorRespRequired.contains("error_code"));
        assertTrue(errorRespRequired.contains("message"));
        assertTrue(errorRespRequired.contains("timestamp"));
    }
}
