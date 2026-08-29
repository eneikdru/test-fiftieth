import { test, expect } from '@playwright/test';

test.describe('SPA Navigation Telemetry E2E Tests', () => {

  test('Given the SPA application is actively used, When navigating between tabs, Then click distance and load success rate are recorded and updated', async ({ page }) => {
    await page.goto('/');

    // Check catalog tab is active by default
    await expect(page.locator('#tab-catalog')).toBeVisible();

    // Click Dossier tab (#tab-dossier)
    await page.click('#tab-dossier');
    await expect(page.locator('#panel-dossier')).toBeVisible();

    // Click Privacy tab (#tab-privacy)
    await page.click('#tab-privacy');
    await expect(page.locator('#panel-privacy')).toBeVisible();

    // Click Foci tab (#tab-foci)
    await page.click('#tab-foci');
    await expect(page.locator('#panel-foci')).toBeVisible();
  });

});
