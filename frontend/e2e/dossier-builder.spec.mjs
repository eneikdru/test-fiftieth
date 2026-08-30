import { test, expect } from '@playwright/test';

test.describe('Dossier Builder E2E Tests', () => {
  test('Given the Dossier Builder is loaded, When a user clicks export, Then it generates the dossier and shows a success notice', async ({ page }) => {
    // Navigate to the harness
    await page.goto('/dossier-builder-harness.html');

    // Ensure component is rendered
    await expect(page.locator('h1')).toHaveText('Dossier Builder', { timeout: 10000 });

    // Ensure we have some documents
    const exportButton = page.locator('button', { hasText: 'Export Dossier' });
    await expect(exportButton).toBeVisible();
    await expect(exportButton).toBeEnabled();

    // Click the export button
    await exportButton.click();

    // Assert the loading state
    await expect(page.locator('button', { hasText: 'Generating PDF...' })).toBeVisible();

    // The download notice should appear after 1500ms
    const notice = page.locator('div[role="status"]');
    await expect(notice).toHaveText('✓ Dossier successfully generated as PDF and downloaded.', { timeout: 3000 });
  });
});
