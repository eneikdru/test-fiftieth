# Delivery Decision and Service Boundary Definition: Slice 2f79461d

## Context & Problem Statement
- **Task**: Service Boundary 2f79461d
- **Wishlist ID**: `4fd0cf38-74b3-4d7c-9adc-7f9bd3627083`
- **Role**: BARCAN-TAG-01 (Solution / System Architect)
- **Slice Target**: OIDC Token Result Type Definition

### Background & Critique
In the previous implementation of OIDC verification, failure modes during OIDC ID token claim verification (such as missing `iss` claim, missing `aud` claim, invalid signature, or token expiration) collapsed into returning `null`. This generic `null` return value compressed distinct causal failure evidence into a single opaque state, destroying explicit state observability and preventing callers or operators from determining why token verification failed.

## Architecture Decision & Service Boundary
To restore epistemic clarity and explicitly represent validation failures as constructive evidence without generic `null` collapse:

1. **Constructive Proof Object (`OidcTokenValidationResult`)**:
   - Verification methods must return a sealed result type `OidcTokenValidationResult`.
   - `OidcTokenValidationResult.Success`: Contains the parsed `OidcProfile` data.
   - `OidcTokenValidationResult.Failure`: Contains a typed `OidcValidationException`.

2. **Domain Exception Hierarchy (`OidcValidationException`)**:
   - `OidcMissingClaimException`: Explicitly specifies missing claim name (e.g. `iss`, `aud`).
   - `OidcInvalidSignatureException`: Captures cryptographic signature or JWK resolution failures.
   - `OidcInvalidIssuerException`: Captures issuer mismatch between token `iss` and expected Moodle URL.
   - `OidcInvalidAudienceException`: Captures audience mismatch between token `aud` and Moodle client ID.
   - `OidcTokenExpiredException`: Captures token expiration.
   - `OidcMalformedTokenException`: Captures syntax or JSON deserialization errors.

3. **Isolated Domain Service (`OidcTokenValidator`)**:
   - Encapsulates token parsing and claim validation logic inside `com.eneik.epidemiology.auth.OidcTokenValidator`.
   - Leaves controller and authentication wiring intact while decoupling failure classification.

## Target Next Owner
- **Next Implementation Owner**: BARCAN-TAG-09 (Integration / Delivery Engineering)
- **Next Slice Task**: Controller integration wiring to consume `OidcTokenValidationResult` and map explicit error states into telemetry and REST response representations.

## Verification
- Unit test suite: `com.eneik.epidemiology.auth.OidcTokenValidationResultTest`
- Execution command: `mvn test`
