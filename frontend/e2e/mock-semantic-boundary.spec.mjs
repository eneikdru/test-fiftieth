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

  test('Given the real backend API, When an unauthorized request is made with invalid token, Then the real backend returns 401 or 403 and rejects 404', async ({ request }) => {
    const response = await request.get(`${BACKEND_URL}/api/v1/documents/1/download`, {
      headers: {
        'Authorization': 'Bearer invalid_token'
      }
    });
    expect([401, 403]).toContain(response.status());
    expect(response.status()).not.toBe(404);
  });

  test('Given the real backend API, When a missing document is requested by an authenticated user, Then the real backend returns 404 Not Found', async ({ request }) => {
    // 1. Register user
    const username = `qa_e2e_user_${Date.now()}`;
    await request.post(`${BACKEND_URL}/api/v1/auth/register`, {
      data: {
        username,
        password: 'Password123!',
        email: `${username}@test.com`,
        full_name: 'QA E2E User'
      }
    });

    // 2. Obtain token via Moodle SSO endpoint
    const ssoResponse = await request.post(`${BACKEND_URL}/api/v1/auth/sso/moodle`, {
      data: {
        username,
        moodle_token: `moodle_token_${Date.now()}`,
        fallback_password: 'Password123!'
      }
    });
    const token = (await ssoResponse.json()).access_token;

    // 3. Request non-existent document ID with valid auth token -> expect 404 Not Found
    const response = await request.get(`${BACKEND_URL}/api/v1/documents/999999/download`, {
      headers: {
        'Authorization': `Bearer ${token}`
      }
    });
    expect(response.status()).toBe(404);
  });

  test('Given the seeded test database, When authenticating via Moodle SSO and querying dossier endpoint, Then Moodle SSO issues token and backend returns seeded state', async ({ request }) => {
    // 1. Register a test user for Moodle SSO fallback mapping
    const username = `qa_e2e_user_${Date.now()}`;
    const registerResponse = await request.post(`${BACKEND_URL}/api/v1/auth/register`, {
      data: {
        username,
        password: 'Password123!',
        email: `${username}@test.com`,
        full_name: 'QA E2E User'
      }
    });
    expect(registerResponse.status()).toBe(201);

    // 2. Authenticate via Moodle SSO endpoint (POST /api/v1/auth/sso/moodle)
    const ssoResponse = await request.post(`${BACKEND_URL}/api/v1/auth/sso/moodle`, {
      data: {
        username,
        moodle_token: `moodle_token_${Date.now()}`,
        fallback_password: 'Password123!'
      }
    });
    expect(ssoResponse.status()).toBe(200);
    const loginBody = await ssoResponse.json();
    const token = loginBody.access_token;
    expect(token).toBeTruthy();

    // 3. Query seeded dossier documents for employee 'Иванов'
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
