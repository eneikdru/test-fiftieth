import { test, expect } from '@playwright/test';
import fs from 'node:fs';
import path from 'node:path';

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

    // Click Telemetry tab (#tab-telemetry)
    await page.click('#tab-telemetry');
    await expect(page.locator('#panel-telemetry')).toBeVisible();

    // Verify click distance and load success rate elements are recorded
    await expect(page.locator('#metric-click-distance-value')).toBeVisible();
    await expect(page.locator('#metric-tab-load-success-value')).toBeVisible();

    const loadSuccessText = await page.locator('#metric-tab-load-success-value').innerText();
    expect(loadSuccessText).toContain('100');
  });

  test('Design verification screenshots for SPA Navigation Telemetry', async ({ page }) => {
    const rootRepoDir = path.resolve(process.cwd(), '..');
    const screenshotDir = path.join(rootRepoDir, '.eneik/records/design-check-1605bd3b-cb55-4e4a-9c91-49658d3ccd38');
    fs.mkdirSync(screenshotDir, { recursive: true });

    // Desktop viewport (1440px)
    await page.setViewportSize({ width: 1440, height: 900 });
    await page.goto('/?tab=telemetry');
    await expect(page.locator('#panel-telemetry')).toBeVisible();
    await page.screenshot({ path: path.join(screenshotDir, 'desktop-1440.png'), fullPage: true });

    // Mobile viewport (375px)
    await page.setViewportSize({ width: 375, height: 812 });
    await page.goto('/?tab=telemetry');
    await expect(page.locator('#panel-telemetry')).toBeVisible();
    await page.screenshot({ path: path.join(screenshotDir, 'mobile-375.png'), fullPage: true });

    expect(fs.existsSync(path.join(screenshotDir, 'desktop-1440.png'))).toBe(true);
    expect(fs.existsSync(path.join(screenshotDir, 'mobile-375.png'))).toBe(true);
  });

});
