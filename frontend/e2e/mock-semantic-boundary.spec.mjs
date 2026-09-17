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

  test('Given the real backend API, When an unauthorized request is made, Then the real backend returns 401 or 403 and rejects 404', async ({ request }) => {
    const response = await request.get(`${BACKEND_URL}/api/v1/documents/1/download`, {
      headers: {
        'Authorization': 'Bearer invalid_token'
      }
    });
    expect([401, 403]).toContain(response.status());
    expect(response.status()).not.toBe(404);
  });

  test('Given the real backend API, When an unauthenticated request to a missing document is made, Then it returns 401 or 403 and rejects 404', async ({ request }) => {
    const unauthResponse = await request.get(`${BACKEND_URL}/api/v1/documents/999999/download`);
    expect([401, 403]).toContain(unauthResponse.status());
    expect(unauthResponse.status()).not.toBe(404);
  });

  test('Given the real backend API, When a missing document is requested with Moodle SSO authorization, Then the real backend returns 404', async ({ request }) => {
    const username = `qa_moodle_sso_user_${Date.now()}`;
    const password = 'Password123!';

    // 1. Provision user account
    const regResponse = await request.post(`${BACKEND_URL}/api/v1/auth/register`, {
      data: {
        username,
        password,
        email: `${username}@inst.ru`,
        full_name: 'QA Moodle User'
      }
    });
    expect(regResponse.status()).toBe(201);

    // 2. Authenticate via Moodle SSO endpoint (/api/v1/auth/sso/moodle)
    const ssoResponse = await request.post(`${BACKEND_URL}/api/v1/auth/sso/moodle`, {
      data: {
        username,
        moodle_token: 'mock_moodle_sso_token',
        fallback_password: password
      }
    });
    expect(ssoResponse.status()).toBe(200);
    const ssoBody = await ssoResponse.json();
    const token = ssoBody.access_token;
    expect(token).toBeTruthy();

    // 3. Request non-existent document resource with valid Moodle SSO token
    const response = await request.get(`${BACKEND_URL}/api/v1/documents/999999/download`, {
      headers: {
        'Authorization': `Bearer ${token}`
      }
    });
    expect(response.status()).toBe(404);
  });

  test('Given the seeded test database, When querying authenticated dossier endpoint via Moodle SSO, Then the real backend returns seeded state', async ({ request }) => {
    const username = `qa_moodle_dossier_user_${Date.now()}`;
    const password = 'Password123!';

    // 1. Provision user account
    const regResponse = await request.post(`${BACKEND_URL}/api/v1/auth/register`, {
      data: {
        username,
        password,
        email: `${username}@inst.ru`,
        full_name: 'QA Moodle Dossier User'
      }
    });
    expect(regResponse.status()).toBe(201);

    // 2. Authenticate via Moodle SSO endpoint (/api/v1/auth/sso/moodle)
    const ssoResponse = await request.post(`${BACKEND_URL}/api/v1/auth/sso/moodle`, {
      data: {
        username,
        moodle_token: 'mock_moodle_sso_token',
        fallback_password: password
      }
    });
    expect(ssoResponse.status()).toBe(200);
    const ssoBody = await ssoResponse.json();
    const token = ssoBody.access_token;
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
