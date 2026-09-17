import { test, expect } from '@playwright/test';

test.describe('Semantic Boundary Mock QA', () => {

  test('Given the updated mock server, When a simulated unauthorized request is made, Then the test must assert the mock returns 403', async ({ request }) => {
    const response = await request.get('/api/v1/documents/unauthorized/download');
    expect(response.status()).toBe(403);
    const text = await response.text();
    expect(text).toBe('Forbidden');
  });

  test('Given the updated mock server, When a missing document is requested, Then the test must assert the mock returns 404', async ({ request }) => {
    const response = await request.get('/api/v1/documents/non-existent/download');
    expect(response.status()).toBe(404);
    const text = await response.text();
    expect(text).toBe('Not Found');
  });

});
