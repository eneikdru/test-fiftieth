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

  test('Given the real backend API, When a missing document is requested by an authenticated user, Then the real backend returns 404 NOT FOUND status', async ({ request }) => {
    // 1. Register a test user
    const username = `qa_missing_doc_${Date.now()}`;
    const registerResponse = await request.post(`${BACKEND_URL}/api/v1/auth/register`, {
      data: {
        username,
        password: 'Password123!',
        email: `${username}@test.com`,
        full_name: 'QA Missing Doc User'
      }
    });
    expect(registerResponse.status()).toBe(201);

    // 2. Login to receive JWT token
    const loginResponse = await request.post(`${BACKEND_URL}/api/v1/auth/login`, {
      data: {
        username,
        password: 'Password123!'
      }
    });
    expect(loginResponse.status()).toBe(200);
    const loginBody = await loginResponse.json();
    const token = loginBody.access_token;
    expect(token).toBeTruthy();

    // 3. Request non-existent document with valid authentication token
    const response = await request.get(`${BACKEND_URL}/api/v1/documents/999999/download`, {
      headers: {
        'Authorization': `Bearer ${token}`
      }
    });
    expect(response.status()).toBe(404);
  });

  test('Given the seeded test database, When querying authenticated dossier endpoint, Then the real backend returns seeded state', async ({ request }) => {
    // 1. Register a test user
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

    // 2. Login to receive JWT token
    const loginResponse = await request.post(`${BACKEND_URL}/api/v1/auth/login`, {
      data: {
        username,
        password: 'Password123!'
      }
    });
    expect(loginResponse.status()).toBe(200);
    const loginBody = await loginResponse.json();
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
