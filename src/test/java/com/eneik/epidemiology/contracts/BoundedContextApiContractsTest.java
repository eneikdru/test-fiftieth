package com.eneik.epidemiology.contracts;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("unchecked")
class BoundedContextApiContractsTest {

    private static final String CATALOG_CONTRACT_PATH = "docs/contracts/Catalog.openapi.yaml";
    private static final String TELEMETRY_CONTRACT_PATH = "docs/contracts/Telemetry.openapi.yaml";
    private static final String PRIVACY_CONTRACT_PATH = "docs/contracts/Privacy.openapi.yaml";

    @Test
    @DisplayName("Given Catalog OpenAPI spec, When parsed, Then it exists and defines catalog search, upload, detail, and download endpoints")
    void testCatalogContractIsIsolatedAndValid() throws Exception {
        File contractFile = new File(CATALOG_CONTRACT_PATH);
        assertTrue(contractFile.exists(), "Contract file docs/contracts/Catalog.openapi.yaml must exist");

        Map<String, Object> openApiSpec = loadYaml(contractFile);
        assertNotNull(openApiSpec, "Catalog contract YAML must be parseable");
        assertTrue(openApiSpec.get("openapi").toString().startsWith("3.0"), "OpenAPI version must be 3.0.x");

        Map<String, Object> info = (Map<String, Object>) openApiSpec.get("info");
        assertNotNull(info);
        assertEquals("API Каталога Документов", info.get("title"));

        Map<String, Object> paths = (Map<String, Object>) openApiSpec.get("paths");
        assertNotNull(paths);
        assertTrue(paths.containsKey("/catalog/documents"), "Must define GET & POST /catalog/documents");
        assertTrue(paths.containsKey("/catalog/documents/{id}"), "Must define GET & DELETE /catalog/documents/{id}");
        assertTrue(paths.containsKey("/catalog/documents/{id}/download"), "Must define GET /catalog/documents/{id}/download");

        Map<String, Object> schemas = getSchemas(openApiSpec);
        assertTrue(schemas.containsKey("CatalogItemResponse"));
        assertTrue(schemas.containsKey("CatalogSearchResponse"));
        assertTrue(schemas.containsKey("CatalogItemUploadRequest"));
        assertTrue(schemas.containsKey("ErrorResponse"));
    }

    @Test
    @DisplayName("Given Telemetry OpenAPI spec, When parsed, Then it exists and defines telemetry events and metrics endpoints")
    void testTelemetryContractIsIsolatedAndValid() throws Exception {
        File contractFile = new File(TELEMETRY_CONTRACT_PATH);
        assertTrue(contractFile.exists(), "Contract file docs/contracts/Telemetry.openapi.yaml must exist");

        Map<String, Object> openApiSpec = loadYaml(contractFile);
        assertNotNull(openApiSpec, "Telemetry contract YAML must be parseable");
        assertTrue(openApiSpec.get("openapi").toString().startsWith("3.0"), "OpenAPI version must be 3.0.x");

        Map<String, Object> info = (Map<String, Object>) openApiSpec.get("info");
        assertNotNull(info);
        assertEquals("API Телеметрии", info.get("title"));

        Map<String, Object> paths = (Map<String, Object>) openApiSpec.get("paths");
        assertNotNull(paths);
        assertTrue(paths.containsKey("/telemetry/events"), "Must define POST & GET /telemetry/events");
        assertTrue(paths.containsKey("/telemetry/metrics"), "Must define GET /telemetry/metrics");

        Map<String, Object> schemas = getSchemas(openApiSpec);
        assertTrue(schemas.containsKey("TelemetryEventRequest"));
        assertTrue(schemas.containsKey("TelemetryEventResponse"));
        assertTrue(schemas.containsKey("TelemetryMetricsResponse"));
        assertTrue(schemas.containsKey("ErrorResponse"));
    }

    @Test
    @DisplayName("Given Privacy OpenAPI spec, When parsed, Then it exists and defines personal data export and erasure endpoints under 152-FZ")
    void testPrivacyContractIsIsolatedAndValid() throws Exception {
        File contractFile = new File(PRIVACY_CONTRACT_PATH);
        assertTrue(contractFile.exists(), "Contract file docs/contracts/Privacy.openapi.yaml must exist");

        Map<String, Object> openApiSpec = loadYaml(contractFile);
        assertNotNull(openApiSpec, "Privacy contract YAML must be parseable");
        assertTrue(openApiSpec.get("openapi").toString().startsWith("3.0"), "OpenAPI version must be 3.0.x");

        Map<String, Object> info = (Map<String, Object>) openApiSpec.get("info");
        assertNotNull(info);
        assertEquals("API Конфиденциальности и 152-ФЗ", info.get("title"));

        Map<String, Object> paths = (Map<String, Object>) openApiSpec.get("paths");
        assertNotNull(paths);
        assertTrue(paths.containsKey("/privacy/export-requests"));
        assertTrue(paths.containsKey("/privacy/export-requests/{requestId}"));
        assertTrue(paths.containsKey("/privacy/export-requests/{requestId}/download"));
        assertTrue(paths.containsKey("/privacy/erasure-requests"));
        assertTrue(paths.containsKey("/privacy/erasure-requests/{requestId}"));

        Map<String, Object> schemas = getSchemas(openApiSpec);
        assertTrue(schemas.containsKey("DataExportRequest"));
        assertTrue(schemas.containsKey("DataExportJobResponse"));
        assertTrue(schemas.containsKey("DataErasureRequest"));
        assertTrue(schemas.containsKey("DataErasureJobResponse"));
        assertTrue(schemas.containsKey("JobStatus"));
        assertTrue(schemas.containsKey("ErrorResponse"));
    }

    @Test
    @DisplayName("Given Catalog, Telemetry, and Privacy contracts, When inspected, Then all three specifications are isolated and distinct")
    void testBoundedContextsAreIsolatedAndDistinct() throws Exception {
        Map<String, Object> catalogSpec = loadYaml(new File(CATALOG_CONTRACT_PATH));
        Map<String, Object> telemetrySpec = loadYaml(new File(TELEMETRY_CONTRACT_PATH));
        Map<String, Object> privacySpec = loadYaml(new File(PRIVACY_CONTRACT_PATH));

        String catalogTitle = ((Map<String, Object>) catalogSpec.get("info")).get("title").toString();
        String telemetryTitle = ((Map<String, Object>) telemetrySpec.get("info")).get("title").toString();
        String privacyTitle = ((Map<String, Object>) privacySpec.get("info")).get("title").toString();

        assertNotEquals(catalogTitle, telemetryTitle);
        assertNotEquals(catalogTitle, privacyTitle);
        assertNotEquals(telemetryTitle, privacyTitle);

        Map<String, Object> catalogPaths = (Map<String, Object>) catalogSpec.get("paths");
        Map<String, Object> telemetryPaths = (Map<String, Object>) telemetrySpec.get("paths");
        Map<String, Object> privacyPaths = (Map<String, Object>) privacySpec.get("paths");

        for (String path : catalogPaths.keySet()) {
            assertFalse(telemetryPaths.containsKey(path), "Catalog path " + path + " must not bleed into Telemetry spec");
            assertFalse(privacyPaths.containsKey(path), "Catalog path " + path + " must not bleed into Privacy spec");
        }

        for (String path : telemetryPaths.keySet()) {
            assertFalse(catalogPaths.containsKey(path), "Telemetry path " + path + " must not bleed into Catalog spec");
            assertFalse(privacyPaths.containsKey(path), "Telemetry path " + path + " must not bleed into Privacy spec");
        }
    }

    private Map<String, Object> loadYaml(File file) throws Exception {
        Yaml yaml = new Yaml();
        try (InputStream is = new FileInputStream(file)) {
            return yaml.load(is);
        }
    }

    private Map<String, Object> getSchemas(Map<String, Object> spec) {
        Map<String, Object> components = (Map<String, Object>) spec.get("components");
        assertNotNull(components, "Spec must contain components");
        Map<String, Object> schemas = (Map<String, Object>) components.get("schemas");
        assertNotNull(schemas, "Components must contain schemas");
        return schemas;
    }
}
