import { test, expect } from '@playwright/test';

// Target real backend directly rather than relying on frontend mock server relative URL resolution
const BACKEND_URL = process.env.BACKEND_URL || process.env.PLAYWRIGHT_BACKEND_URL || 'http://127.0.0.1:18080';

test.describe('Semantic Boundary Real Backend Integration QA', () => {

  test('Given the real backend API, When health endpoint is queried, Then real Spring Boot instance returns status UP', async ({ request }) => {
    const response = await request.get(`${BACKEND_URL}/health`);
    expect(response.status()).toBe(200);
    const body = await response.json();
    expect(body.status).toBe('UP');
  });

  test('Given the real backend API, When an unauthorized request is made, Then the real backend returns 401 or 403', async ({ request }) => {
    const response = await request.get(`${BACKEND_URL}/api/v1/documents/1/download`, {
      headers: {
        'Authorization': 'Bearer invalid_token'
      }
    });
    expect([401, 403]).toContain(response.status());
  });

  test('Given the real backend API, When an unauthenticated request attempts to access document download endpoint, Then the real backend returns authorization status 401 or 403 and rejects 404', async ({ request }) => {
    const response = await request.get(`${BACKEND_URL}/api/v1/documents/999999/download`);
    expect([401, 403]).toContain(response.status());
  });

  test('Given the seeded test database, When authenticating via Moodle SSO and querying dossier endpoint, Then the real backend returns seeded state', async ({ request }) => {
    // 1. Authenticate via Moodle SSO integration endpoint
    const username = `qa_moodle_user_${Date.now()}`;
    const ssoResponse = await request.post(`${BACKEND_URL}/api/v1/auth/sso/moodle`, {
      data: {
        username,
        moodle_token: 'mock_valid_moodle_token',
        fallback_password: 'Password123!'
      }
    });
    expect([200, 201]).toContain(ssoResponse.status());
    const ssoBody = await ssoResponse.json();
    const token = ssoBody.access_token;
    expect(token).toBeTruthy();

    // 2. Query seeded dossier documents for employee 'Иванов'
    const dossierResponse = await request.get(`${BACKEND_URL}/api/v1/dossier/documents?employee_surname=${encodeURIComponent('Иванов')}`, {
      headers: {
        'Authorization': `Bearer ${token}`
      }
    });
    expect(dossierResponse.status()).toBe(200);
    const dossierDocs = await dossierResponse.json();
    expect(Array.isArray(dossierDocs)).toBe(true);
    expect(dossierDocs.length).toBeGreaterThan(0);

    // Assert against seeded state from Flyway migration (e.g. STRAIN_ISOLATION for Ivanov)
    const ivanovDoc = dossierDocs.find(d => d.employee_surname === 'Иванов');
    expect(ivanovDoc).toBeDefined();
    expect(ivanovDoc.employee_id).toBe('EMP-007');
  });

});
