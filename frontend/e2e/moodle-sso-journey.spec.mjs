import { test, expect } from '@playwright/test';

test.describe('Moodle SSO Authentication to Catalog Search Journey', () => {

  test('Given the deployed test environment, When the full user journey from Moodle login to catalog search is executed, Then the system seamlessly authenticates and returns actual data without mocking', async ({ page }) => {
    const testUsername = process.env.TEST_MOODLE_USERNAME || 'moodle_authenticated_user';
    const testPassword = process.env.TEST_MOODLE_PASSWORD || process.env.TEST_USER_PASSWORD || 'dummy_test_pass';

    // Navigate to the authentication page / harness in login mode
    await page.goto('/test-harness.html?mode=login');

    // Perform Moodle user authentication login using test configuration credentials
    await page.fill('#username-input', testUsername);
    await page.fill('#password-input', testPassword);
    await page.click('button[type="submit"]');

    // Verify successful authentication and redirection to the catalog UI
    await expect(page.locator('.catalog-container')).toBeVisible();

    // Fill in catalog search query for sample protocol material
    await page.fill('#search-query-input', 'сальмонеллеза');
    await page.click('#search-submit-btn');

    // Confirm catalog returns matching protocol document without mock routes
    const docTitle = page.locator('.doc-title').first();
    await expect(docTitle).toBeVisible();
    await expect(docTitle).toContainText('сальмонеллеза');
  });

});
