import { test, expect } from '@playwright/test';
import fs from 'node:fs';
import path from 'node:path';

test.describe('SPA Navigation Telemetry E2E Tests', () => {

  test('Given a user navigating the main application, Then the Telemetry tab is entirely absent and inaccessible', async ({ page }) => {
    await page.goto('/');

    // Check catalog tab is active by default
    await expect(page.locator('#tab-catalog')).toBeVisible();

    // Verify telemetry tab button does NOT exist in navigation
    await expect(page.locator('#tab-telemetry')).not.toBeAttached();
    await expect(page.locator('#panel-telemetry')).not.toBeAttached();

    // Click Dossier tab (#tab-dossier)
    await page.click('#tab-dossier');
    await expect(page.locator('#panel-dossier')).toBeVisible();

    // Click Foci tab (#tab-foci)
    await page.click('#tab-foci');
    await expect(page.locator('#panel-foci')).toBeVisible();
  });

  test('Design verification screenshots and layout geometry for Frontend Telemetry Removal', async ({ page }) => {
    const rootRepoDir = path.resolve(process.cwd(), '..');
    const screenshotDir = path.join(rootRepoDir, '.eneik/records/design-check-fd9f7daf-5e84-4960-a805-dd15648c5979');
    fs.mkdirSync(screenshotDir, { recursive: true });

    // Desktop viewport (1440px)
    await page.setViewportSize({ width: 1440, height: 900 });
    await page.goto('/');
    await expect(page.locator('#tab-catalog')).toBeVisible();
    await page.screenshot({ path: path.join(screenshotDir, 'desktop-1440.png'), fullPage: true });

    // Mobile viewport (375px)
    await page.setViewportSize({ width: 375, height: 812 });
    await page.goto('/');
    await expect(page.locator('#tab-catalog')).toBeVisible();
    await page.screenshot({ path: path.join(screenshotDir, 'mobile-375.png'), fullPage: true });

    // Layout check bounding boxes
    const layoutElements = [
      { id: 'header', selector: 'header' },
      { id: 'nav', selector: 'nav[aria-label="Навигация по модулям"]' },
      { id: 'panel-catalog', selector: '#panel-catalog' }
    ];

    const layoutCheck = [];
    for (const item of layoutElements) {
      const el = page.locator(item.selector);
      if (await el.isVisible()) {
        const box = await el.boundingBox();
        if (box) {
          layoutCheck.push({
            id: item.id,
            left: box.x,
            top: box.y,
            width: box.width,
            height: box.height
          });
        }
      }
    }

    fs.writeFileSync(path.join(screenshotDir, 'layout-check.json'), JSON.stringify(layoutCheck, null, 2));

    expect(fs.existsSync(path.join(screenshotDir, 'desktop-1440.png'))).toBe(true);
    expect(fs.existsSync(path.join(screenshotDir, 'mobile-375.png'))).toBe(true);
    expect(fs.existsSync(path.join(screenshotDir, 'layout-check.json'))).toBe(true);
  });

});
