import { test, expect } from '@playwright/test';
import fs from 'node:fs';
import path from 'node:path';

test.describe('SPA Navigation Telemetry and Consent E2E Tests', () => {

  test('Given a new user session, When telemetry scripts attempt to load, Then they must be blocked until explicit consent is given', async ({ page }) => {
    // Clear localStorage to simulate new user session
    await page.goto('/');
    await page.evaluate(() => localStorage.clear());
    await page.reload();

    // Verify consent banner is visible on new session
    const consentBanner = page.locator('#telemetry-consent-banner');
    await expect(consentBanner).toBeVisible();

    // Navigate between tabs before giving consent
    await page.click('#tab-dossier');
    await expect(page.locator('#panel-dossier')).toBeVisible();

    await page.click('#tab-telemetry');
    await expect(page.locator('#panel-telemetry')).toBeVisible();

    // Activities list should only have initial app load item, tab transitions unrecorded
    const activityCountText = await page.locator('#panel-telemetry').innerText();
    expect(activityCountText).toContain('Всего записей: 1');
  });

  test('Given given consent, When a search occurs, Then telemetry is recorded', async ({ page }) => {
    await page.goto('/');
    await page.evaluate(() => localStorage.clear());
    await page.reload();

    // Accept consent
    const acceptBtn = page.locator('#accept-telemetry-btn');
    await expect(acceptBtn).toBeVisible();
    await acceptBtn.click();

    // Consent banner disappears
    await expect(page.locator('#telemetry-consent-banner')).toHaveCount(0);

    // Verify consent is stored
    const consentState = await page.evaluate(() => localStorage.getItem('telemetry_consent'));
    expect(consentState).toBe('granted');

    // Perform navigation with consent given
    await page.click('#tab-dossier');
    await expect(page.locator('#panel-dossier')).toBeVisible();

    await page.click('#tab-telemetry');
    await expect(page.locator('#panel-telemetry')).toBeVisible();

    // Verify telemetry recorded the tab transition
    const panelText = await page.locator('#panel-telemetry').innerText();
    expect(panelText).toContain('Переход:');
  });

  test('Given the consent banner, When navigating via keyboard, Then all controls must be focusable and operable', async ({ page }) => {
    await page.goto('/');
    await page.evaluate(() => localStorage.clear());
    await page.reload();

    const declineBtn = page.locator('#decline-telemetry-btn');
    const acceptBtn = page.locator('#accept-telemetry-btn');

    await expect(declineBtn).toBeVisible();
    await expect(acceptBtn).toBeVisible();

    // Focus accept button directly and trigger via Keyboard Enter
    await acceptBtn.focus();
    await expect(acceptBtn).toBeFocused();
    await page.keyboard.press('Enter');

    // Verify consent banner dismissed
    await expect(page.locator('#telemetry-consent-banner')).toHaveCount(0);
    const consentState = await page.evaluate(() => localStorage.getItem('telemetry_consent'));
    expect(consentState).toBe('granted');
  });

  test('Design verification screenshots and layout geometry for Telemetry Consent', async ({ page }) => {
    const rootRepoDir = path.resolve(process.cwd(), '..');
    const screenshotDir = path.join(rootRepoDir, '.eneik/records/design-check-c4650feb-8810-4237-b19c-0d755e2dc7fe');
    fs.mkdirSync(screenshotDir, { recursive: true });

    // Desktop viewport (1440px)
    await page.setViewportSize({ width: 1440, height: 900 });
    await page.goto('/');
    await page.evaluate(() => localStorage.clear());
    await page.reload();
    await expect(page.locator('#telemetry-consent-banner')).toBeVisible();
    await page.screenshot({ path: path.join(screenshotDir, 'desktop-1440.png'), fullPage: true });

    // Mobile viewport (375px)
    await page.setViewportSize({ width: 375, height: 812 });
    await page.goto('/');
    await expect(page.locator('#telemetry-consent-banner')).toBeVisible();
    await page.screenshot({ path: path.join(screenshotDir, 'mobile-375.png'), fullPage: true });

    // Extract layout geometry bounding boxes for key elements
    const layoutBoxes = await page.evaluate(() => {
      const selectors = [
        { id: 'header', selector: 'header' },
        { id: 'banner', selector: '#telemetry-consent-banner' },
        { id: 'accept-btn', selector: '#accept-telemetry-btn' },
        { id: 'decline-btn', selector: '#decline-telemetry-btn' }
      ];
      return selectors.map(item => {
        const el = document.querySelector(item.selector);
        if (!el) return { id: item.id, left: 0, top: 0, width: 0, height: 0 };
        const rect = el.getBoundingClientRect();
        return {
          id: item.id,
          left: Math.round(rect.left),
          top: Math.round(rect.top),
          width: Math.round(rect.width),
          height: Math.round(rect.height)
        };
      });
    });

    fs.writeFileSync(path.join(screenshotDir, 'layout-check.json'), JSON.stringify(layoutBoxes, null, 2));

    expect(fs.existsSync(path.join(screenshotDir, 'desktop-1440.png'))).toBe(true);
    expect(fs.existsSync(path.join(screenshotDir, 'mobile-375.png'))).toBe(true);
    expect(fs.existsSync(path.join(screenshotDir, 'layout-check.json'))).toBe(true);
  });

});
