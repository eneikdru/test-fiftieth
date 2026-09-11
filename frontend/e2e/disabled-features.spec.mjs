import { test, expect } from '@playwright/test';

test.describe('Verification of Disabled Features and Absence of Mock Behaviors', () => {

  test('Given the application UI, When locating file upload controls, Then they must be strictly disabled or absent', async ({ page }) => {
    await page.goto('/');

    const uploadBtn = page.locator('#open-upload-modal-btn');
    const count = await uploadBtn.count();
    if (count > 0) {
      await expect(uploadBtn).toBeDisabled();
    } else {
      await expect(uploadBtn).toHaveCount(0);
    }
  });

  test('Given the application UI, When navigating to Dossier view, Then report generation is strictly disabled', async ({ page }) => {
    await page.goto('/');

    await page.click('#tab-dossier');
    await expect(page.locator('#panel-dossier')).toBeVisible();

    await page.fill('#panel-dossier #search-query-input', 'Иванов');
    await page.click('#panel-dossier #search-button');

    const generateReportBtn = page.locator('#generate-report-button');
    await expect(generateReportBtn).toBeVisible();
    await expect(generateReportBtn).toBeDisabled();
  });

  test('Given the dossier harness UI, When performed search, Then report generation is strictly disabled and no mock notice is generated', async ({ page }) => {
    await page.goto('/dossier-harness.html');

    await page.fill('#search-query-input', 'Иванов');
    await page.click('#search-button');

    const generateReportBtn = page.locator('#generate-report-button');
    await expect(generateReportBtn).toBeVisible();
    await expect(generateReportBtn).toBeDisabled();

    // Verify absence of mock generated report notice
    const mockNotice = page.locator('text=✓ Итоговая справка успешно сформирована.');
    await expect(mockNotice).toHaveCount(0);
  });

});
