# Project Brief

## Customer Job

The client's own entries, quoted verbatim and in the client's own language. This file is
maintained by the factory from the wishlist and must not be edited by hand or translated -
it is the referent every later artifact is checked against.

1. The delivered API rejects authenticated requests with HTTP 401 on endpoints that must answer 200/201, and CI on main has been red for every merge since 2026-08-26 because of it. Measured on the latest main build: 12 tests expecting 200 receive 401, 10 tests expecting 201 receive 401, 15 failures in total across EmployeeDossierControllerTest, EmployeeDossierApiSliceVerificationTest, EmployeeDossierE2ETest, DossierTelemetryVerificationTest and FrontendBackendIntegrationE2ETest. The employee dossier search, document listing and report generation are unreachable as a result, so the product cannot be used at all. What I need: requests that carry valid credentials reach their endpoint and return the documented status, invalid ones still get 401, and the Backend Verification workflow on main goes green and stays green.

2. Интеграция с системой Moodle Института Эпидемиологии: 1. Единая сквозная аутентификация (SSO) через Moodle (OAuth2 / OIDC / LTI). 2. Синхронизация и отображение иерархии ролей Moodle в ролевую модель архива (Администратор, Старший научный сотрудник / Эпидемиолог с правом подписи досье, Исследователь / Аспирант с правом доступа к протоколам). 3. Автоматическое разграничение прав доступа к закрытым штаммам и аналитическим отчетам на основе кафедр и курсов Moodle. 4. Устойчивость к сбоям внешней LMS (автономный fallback).

---

Entries: 2. Anything this product claims - a page
heading, a filter, a capability - must trace to one of them or to a declared route.
