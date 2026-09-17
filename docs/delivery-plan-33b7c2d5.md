# Delivery Decision and Handoff Note: Delivery Plan 33b7c2d5

## Task Information
- **Task ID**: 33b7c2d5
- **Task Title**: Delivery Plan 33b7c2d5
- **Role**: BARCAN-TAG-09 (Delivery Management / Technical Product Manager)
- **Target Next Owner Role**: BARCAN-TAG-07 (Security Engineering / Authentication)
- **Wishlist Item ID**: `4fd0cf38-74b3-4d7c-9adc-7f9bd3627083`
- **Slice**: Knowledge-First Password Transmission Definition
- **Kano**: Must-Be
- **Cynefin**: Complicated

## Delivery Decision & Secure Transmission Architecture
This delivery decision records the architectural decision for secure fallback credential transmission in accordance with knowledge-first epistemological principles for wishlist item `4fd0cf38-74b3-4d7c-9adc-7f9bd3627083`.

### 1. Problem Context
When a user logs in via federated identity (e.g., Moodle OIDC) and local account creation is triggered, `AuthController.java` generates a random fallback password via `generateSecureFallbackPassword()`. Currently, this generated credential is never transmitted to the user or recorded in a user-accessible manner. This creates an ungrounded state where the user possesses an account but is structurally deprived of epistemic knowledge of their own fallback credentials.

### 2. Decision: Secure Transmission Mechanism Choice
To satisfy the knowledge-first constraint while upholding security best practices, the system explicitly selects the **Time-Bound Single-Use Onboarding Token Link (One-Time Knowledge Link)** mechanism:

1. **Generation & Storage**: Upon fallback password generation, the system creates a high-entropy, cryptographically secure single-use onboarding token. The token digest is stored with a strict TTL (e.g., 15 minutes) and associated with the newly created account.
2. **Secure Delivery Channel**: The system delivers a time-bound credential activation link (`/auth/claim-credential?token=...`) to the user via out-of-band verified notification (e.g., transactional email / system notification channel) or presents a single-view confirmation screen during the initial OIDC callback flow.
3. **Knowledge Acquisition Flow**: Clicking the one-time link allows the user to explicitly claim, view, or re-set their fallback local password, converting unverified backend state into verified epistemic knowledge.

### 3. Knowledge-First Constraint Satisfaction
- **Epistemic Link Restoration**: Grounding authentication in actual knowledge ensures that the user is explicitly aware of their fallback login credentials.
- **Verification Evidence**: The system logs token issuance and redemption events with timestamped audit trails without storing or transmitting unhashed secrets.
- **No Silent Dead-End Accounts**: Prevents the creation of local accounts that cannot be accessed directly by the account owner.

## Handoff Note & Scope Boundaries
- **Current Owner Role**: `BARCAN-TAG-09` (Delivery Management)
- **Target Next Owner Role**: `BARCAN-TAG-07` (Security Engineering / Auth)
- **Scope Restriction**: No implementation scope expansion in this delivery management slice. The decision record formally defines the specification and handoff requirements for `BARCAN-TAG-07`.

### Actionable Scope for Next Owner (BARCAN-TAG-07)
1. **Token Service Implementation**: Implement single-use credential claim token generation and validation in `AuthController` or a dedicated `CredentialClaimService`.
2. **Endpoint Specification**: Expose `/api/v1/auth/claim-credential` to validate single-use tokens and prompt password confirmation.
3. **Audit & Safety**: Ensure token hashes are stored securely and invalidated immediately upon first use or expiry.
