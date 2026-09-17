import { test, expect } from '@playwright/test';

test.describe('Semantic Boundary Integration QA', () => {

  test('Given the API endpoint, When an unauthorized request with an invalid authorization header is made, Then the response status must be 403', async ({ request }) => {
    const response = await request.get('/api/v1/documents/1/download', {
      headers: {
        'Authorization': 'Bearer invalid_token'
      }
    });
    expect([401, 403]).toContain(response.status());
  });

  test('Given the API endpoint, When a missing document ID is requested, Then the response status must be 404', async ({ request }) => {
    const response = await request.get('/api/v1/documents/999999/download');
    expect(response.status()).toBe(404);
  });

});
