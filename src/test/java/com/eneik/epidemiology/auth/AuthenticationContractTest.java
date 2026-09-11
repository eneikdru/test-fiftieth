package com.eneik.epidemiology.auth;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AuthenticationContractTest {

    private static final String CONTRACT_PATH = "docs/contracts/Auth.openapi.yaml";

    @Test
    @DisplayName("Given the Authentication OpenAPI contract file, When parsed, Then it exists and is a valid OpenAPI 3.0 specification")
    void testContractFileExistsAndIsValidOpenApi() throws Exception {
        File contractFile = new File(CONTRACT_PATH);
        assertTrue(contractFile.exists(), "Contract file docs/contracts/Auth.openapi.yaml must exist");

        Yaml yaml = new Yaml();
        Map<String, Object> openApiSpec;
        try (InputStream is = new FileInputStream(contractFile)) {
            openApiSpec = yaml.load(is);
        }

        assertNotNull(openApiSpec, "Contract YAML must be parseable");
        assertTrue(openApiSpec.containsKey("openapi"), "Spec must contain 'openapi' version declaration");
        assertTrue(openApiSpec.get("openapi").toString().startsWith("3.0"), "OpenAPI version must be 3.0.x");

        @SuppressWarnings("unchecked")
        Map<String, Object> info = (Map<String, Object>) openApiSpec.get("info");
        assertNotNull(info, "Spec must contain 'info' section");
        assertEquals("Authentication & Account Recovery API Contract", info.get("title"));
    }

    @Test
    @DisplayName("Given the OpenAPI spec, When auth endpoints are inspected, Then login and token management endpoints are explicitly defined")
    void testAuthEndpointsAreDefined() throws Exception {
        File contractFile = new File(CONTRACT_PATH);
        Yaml yaml = new Yaml();
        Map<String, Object> openApiSpec;
        try (InputStream is = new FileInputStream(contractFile)) {
            openApiSpec = yaml.load(is);
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> paths = (Map<String, Object>) openApiSpec.get("paths");
        assertNotNull(paths, "Spec must contain 'paths' definitions");

        assertTrue(paths.containsKey("/auth/login"), "Must define POST /auth/login endpoint");
        assertTrue(paths.containsKey("/auth/register"), "Must define POST /auth/register endpoint");
        assertTrue(paths.containsKey("/auth/refresh"), "Must define POST /auth/refresh endpoint");
        assertTrue(paths.containsKey("/auth/logout"), "Must define POST /auth/logout endpoint");
    }

    @Test
    @DisplayName("Given the OpenAPI spec, When token response schema is inspected, Then the JWT structure is explicitly specified")
    void testJwtStructureIsExplicitlySpecified() throws Exception {
        File contractFile = new File(CONTRACT_PATH);
        Yaml yaml = new Yaml();
        Map<String, Object> openApiSpec;
        try (InputStream is = new FileInputStream(contractFile)) {
            openApiSpec = yaml.load(is);
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> components = (Map<String, Object>) openApiSpec.get("components");
        assertNotNull(components, "Spec must contain 'components'");

        @SuppressWarnings("unchecked")
        Map<String, Object> schemas = (Map<String, Object>) components.get("schemas");
        assertNotNull(schemas, "Components must contain 'schemas'");

        // Verify JwtTokenStructure schema
        assertTrue(schemas.containsKey("JwtTokenStructure"), "Must define JwtTokenStructure schema");
        @SuppressWarnings("unchecked")
        Map<String, Object> jwtTokenStruct = (Map<String, Object>) schemas.get("JwtTokenStructure");
        @SuppressWarnings("unchecked")
        List<String> structRequired = (List<String>) jwtTokenStruct.get("required");
        assertTrue(structRequired.contains("header"), "JwtTokenStructure must require 'header'");
        assertTrue(structRequired.contains("payload"), "JwtTokenStructure must require 'payload'");
        assertTrue(structRequired.contains("signature"), "JwtTokenStructure must require 'signature'");

        // Verify JwtHeader schema
        assertTrue(schemas.containsKey("JwtHeader"), "Must define JwtHeader schema");
        @SuppressWarnings("unchecked")
        Map<String, Object> jwtHeader = (Map<String, Object>) schemas.get("JwtHeader");
        @SuppressWarnings("unchecked")
        List<String> headerRequired = (List<String>) jwtHeader.get("required");
        assertTrue(headerRequired.contains("alg"), "JwtHeader must require 'alg'");
        assertTrue(headerRequired.contains("typ"), "JwtHeader must require 'typ'");

        // Verify JwtPayload schema
        assertTrue(schemas.containsKey("JwtPayload"), "Must define JwtPayload schema");
        @SuppressWarnings("unchecked")
        Map<String, Object> jwtPayload = (Map<String, Object>) schemas.get("JwtPayload");
        @SuppressWarnings("unchecked")
        List<String> payloadRequired = (List<String>) jwtPayload.get("required");
        assertTrue(payloadRequired.contains("sub"), "JwtPayload must require 'sub'");
        assertTrue(payloadRequired.contains("role"), "JwtPayload must require 'role'");
        assertTrue(payloadRequired.contains("iat"), "JwtPayload must require 'iat'");
        assertTrue(payloadRequired.contains("exp"), "JwtPayload must require 'exp'");
    }

    @Test
    @DisplayName("Given the OpenAPI spec, When login request and response schemas are inspected, Then fields and DTOs match domain specifications")
    void testLoginRequestAndResponseSchemas() throws Exception {
        File contractFile = new File(CONTRACT_PATH);
        Yaml yaml = new Yaml();
        Map<String, Object> openApiSpec;
        try (InputStream is = new FileInputStream(contractFile)) {
            openApiSpec = yaml.load(is);
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> components = (Map<String, Object>) openApiSpec.get("components");
        @SuppressWarnings("unchecked")
        Map<String, Object> schemas = (Map<String, Object>) components.get("schemas");

        // Verify LoginRequest
        assertTrue(schemas.containsKey("LoginRequest"), "Must define LoginRequest schema");
        @SuppressWarnings("unchecked")
        Map<String, Object> loginReq = (Map<String, Object>) schemas.get("LoginRequest");
        @SuppressWarnings("unchecked")
        List<String> loginReqRequired = (List<String>) loginReq.get("required");
        assertTrue(loginReqRequired.contains("username"), "LoginRequest must require 'username'");
        assertTrue(loginReqRequired.contains("password"), "LoginRequest must require 'password'");

        // Verify AuthTokenResponse
        assertTrue(schemas.containsKey("AuthTokenResponse"), "Must define AuthTokenResponse schema");
        @SuppressWarnings("unchecked")
        Map<String, Object> tokenResp = (Map<String, Object>) schemas.get("AuthTokenResponse");
        @SuppressWarnings("unchecked")
        List<String> tokenRespRequired = (List<String>) tokenResp.get("required");
        assertTrue(tokenRespRequired.contains("access_token"), "AuthTokenResponse must require 'access_token'");
        assertTrue(tokenRespRequired.contains("refresh_token"), "AuthTokenResponse must require 'refresh_token'");
        assertTrue(tokenRespRequired.contains("token_type"), "AuthTokenResponse must require 'token_type'");
        assertTrue(tokenRespRequired.contains("expires_in"), "AuthTokenResponse must require 'expires_in'");
        assertTrue(tokenRespRequired.contains("user"), "AuthTokenResponse must require 'user'");
        assertTrue(tokenRespRequired.contains("token_structure"), "AuthTokenResponse must require 'token_structure'");

        // Verify ErrorResponse
        assertTrue(schemas.containsKey("ErrorResponse"), "Must define ErrorResponse schema");
        @SuppressWarnings("unchecked")
        Map<String, Object> errorResp = (Map<String, Object>) schemas.get("ErrorResponse");
        @SuppressWarnings("unchecked")
        List<String> errorRespRequired = (List<String>) errorResp.get("required");
        assertTrue(errorRespRequired.contains("error_code"));
        assertTrue(errorRespRequired.contains("message"));
        assertTrue(errorRespRequired.contains("timestamp"));
    }
}
