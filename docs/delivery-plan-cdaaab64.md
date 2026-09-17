# Delivery Plan: API Authorization Guard and Security Verification

## Overview
- **Slice**: Resolve security and authentication audit findings for dossier endpoints and single sign-on delivery.
- **Scope**: Enforcement of 401 Unauthorized status responses on protected dossier endpoints, secure HttpOnly session cookie delivery, and audit record updates.

## Audit Findings and Resolutions

### Finding 1: Unauthenticated Dossier Endpoint Access
- **Issue**: Unauthenticated requests to dossier report status (`/api/v1/dossier/reports/{id}`) and signing endpoints resulted in inconsistent error codes or missing authentication checks instead of HTTP 401 Unauthorized.
- **Resolution**: Updated `EmployeeDossierController` product code to validate security context authentication explicitly and return HTTP 401 Unauthorized for unauthenticated callers. Added integration tests in `EmployeeDossierControllerTest` asserting HTTP 200 OK / 201 Created for authenticated requests and HTTP 401 Unauthorized for unauthenticated requests.

### Finding 2: Rejection Decision Format
- **Issue**: Authorization decision records contained non-conforming status values violating binary decision requirements.
- **Resolution**: Normalized security review decision records to use explicit binary verdicts (`REJECT`).

### Finding 3: Secure Session Cookie Verification
- **Issue**: Single sign-on launch tests previously checked display names without asserting HttpOnly `Set-Cookie` headers directly.
- **Resolution**: Updated `AuthControllerTest` `testLtiLaunch_JsonPayload_Success` to explicitly verify session token issuance via secure `Set-Cookie` headers (`access_token` and `refresh_token`) and absence of access token parameters in `Location` redirect URLs.

### Finding 4: Definition of Done for Security Slices
- **Issue**: Planned single sign-on secure token delivery slice lacked explicit Definition of Done for error states.
- **Resolution**: Updated security completion criteria to mandate handling of error states (HTTP 401 Unauthorized for invalid or missing tokens, HTTP 403 Forbidden for insufficient permissions, and standardized JSON error payloads).

## Modified Files
- `src/main/java/com/eneik/epidemiology/document/EmployeeDossierController.java`: Added explicit SecurityContext authentication validation returning 401 Unauthorized for unauthenticated requests.
- `src/test/java/com/eneik/epidemiology/document/EmployeeDossierControllerTest.java`: Added test coverage verifying 200 OK / 201 Created for authenticated requests and 401 Unauthorized for unauthenticated requests.
- `src/test/java/com/eneik/epidemiology/auth/AuthControllerTest.java`: Updated LTI launch test assertions to verify secure HttpOnly Set-Cookie token delivery and absence of tokens in redirect URL headers.
- `docs/delivery-plan-cdaaab64.md`: Documented delivery plan and security audit resolutions.

## Verification Results
- Executed backend test suite: `mvn test -Dtest=EmployeeDossierControllerTest,AuthControllerTest`
- Result: 53 tests executed, 0 failures, 0 errors.
