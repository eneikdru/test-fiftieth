import { test, expect } from '@playwright/test';

// Target real backend directly rather than relying on frontend mock server relative URL resolution
const BACKEND_URL = process.env.BACKEND_URL || process.env.PLAYWRIGHT_BACKEND_URL || 'http://127.0.0.1:18080';

test.describe('Semantic Boundary Real Backend Integration QA', () => {

  test('Given the real backend API, When an unauthorized request is made, Then the real backend returns 401 or 403', async ({ request }) => {
    const response = await request.get(`${BACKEND_URL}/api/v1/documents/1/download`, {
      headers: {
        'Authorization': 'Bearer invalid_token'
      }
    });
    expect([401, 403]).toContain(response.status());
  });

  test('Given the real backend API, When a missing document is requested, Then the real backend returns 404 or authorization boundary status', async ({ request }) => {
    const response = await request.get(`${BACKEND_URL}/api/v1/documents/999999/download`);
    expect([401, 403, 404]).toContain(response.status());
  });

});
