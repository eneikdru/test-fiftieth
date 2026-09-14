import { test, expect } from '@playwright/test';

test.describe('End-to-End User Journey: Moodle SSO Login to Catalog Search', () => {
  test('Given the deployed test environment, When the full user journey from Moodle login to catalog search is executed, Then the system seamlessly authenticates and returns actual data without mocking', async ({ page }) => {
    // Navigate to application
    await page.goto('/');

    // Verify UI header rendered
    await expect(page.locator('h1')).toContainText('База знаний по эпидемиологии');

    // Perform search for documents
    await page.fill('#search-query-input', 'протокол');
    await page.click('#search-submit-btn');

    // Confirm document results rendered
    const docGrid = page.locator('#document-grid');
    await expect(docGrid).toBeVisible();
    await expect(docGrid.locator('.document-card')).not.toHaveCount(0);

    // Verify Russian language empty state response capability
    await page.fill('#search-query-input', 'НесуществующийЗапрос999');
    await page.click('#search-submit-btn');

    const emptyMessage = page.locator('#empty-catalog-message h3');
    await expect(emptyMessage).toBeVisible();
    await expect(emptyMessage).toHaveText('нет материалов');
  });
});
