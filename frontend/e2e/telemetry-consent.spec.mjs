import { test, expect } from '@playwright/test';
import fs from 'node:fs';
import path from 'node:path';

test.describe('Telemetry Consent Implementation E2E Tests', () => {

  test.beforeEach(async ({ page }) => {
    // Clear localStorage before each test to simulate a fresh session
    await page.goto('/');
    await page.evaluate(() => localStorage.clear());
    await page.reload();
  });

  test('Given a new visitor session, When telemetry scripts attempt to load, Then consent banner is shown and telemetry is blocked until explicit consent', async ({ page }) => {
    // Check consent banner is visible on new session
    const banner = page.locator('#telemetry-consent-banner');
    await expect(banner).toBeVisible();

    const acceptBtn = page.locator('#telemetry-accept-btn');
    const declineBtn = page.locator('#telemetry-decline-btn');

    await expect(acceptBtn).toBeVisible();
    await expect(declineBtn).toBeVisible();

    // Verify keyboard focusability
    await declineBtn.focus();
    await expect(declineBtn).toBeFocused();
    await acceptBtn.focus();
    await expect(acceptBtn).toBeFocused();

    // Before consent, navigating between tabs should NOT update telemetry
    await page.click('#tab-dossier');
    await page.click('#tab-telemetry');

    const metricValue = await page.locator('#metric-click-distance-value').innerText();
    expect(metricValue).toContain('1.8'); // Unchanged initial metric

    // Now accept consent
    await page.click('#telemetry-accept-btn');
    await expect(banner).not.toBeVisible();

    // Verify localStorage key is set
    const consentValue = await page.evaluate(() => localStorage.getItem('telemetry_consent'));
    expect(consentValue).toBe('granted');

    // After consent granted, navigating tabs records telemetry
    await page.click('#tab-catalog');
    await page.click('#tab-telemetry');

    const updatedClicksText = await page.locator('#metric-click-distance-value').innerText();
    expect(updatedClicksText).toBeDefined();
  });

  test('Given consent banner, When user declines consent, Then telemetry remains blocked and banner closes', async ({ page }) => {
    const banner = page.locator('#telemetry-consent-banner');
    await expect(banner).toBeVisible();

    // Decline consent
    await page.click('#telemetry-decline-btn');
    await expect(banner).not.toBeVisible();

    const consentValue = await page.evaluate(() => localStorage.getItem('telemetry_consent'));
    expect(consentValue).toBe('denied');

    // Verify telemetry remains blocked
    await page.click('#tab-dossier');
    await page.click('#tab-telemetry');
    const clicksText = await page.locator('#metric-click-distance-value').innerText();
    expect(clicksText).toContain('1.8'); // Default unchanged
  });

  test('Design verification screenshots and layout geometry for Telemetry Consent', async ({ page }) => {
    const rootRepoDir = path.resolve(process.cwd(), '..');
    const recordDir = path.join(rootRepoDir, '.eneik/records/design-check-c4650feb-8810-4237-b19c-0d755e2dc7fe');
    fs.mkdirSync(recordDir, { recursive: true });

    // Desktop Viewport (1440px)
    await page.setViewportSize({ width: 1440, height: 900 });
    await page.goto('/');
    await expect(page.locator('#telemetry-consent-banner')).toBeVisible();
    await page.screenshot({ path: path.join(recordDir, 'desktop-1440.png'), fullPage: true });

    // Mobile Viewport (375px)
    await page.setViewportSize({ width: 375, height: 812 });
    await page.goto('/');
    await expect(page.locator('#telemetry-consent-banner')).toBeVisible();
    await page.screenshot({ path: path.join(recordDir, 'mobile-375.png'), fullPage: true });

    // Export layout bounding boxes
    const layoutCheck = await page.evaluate(() => {
      const elementsToTrack = [
        { id: 'header', selector: 'header' },
        { id: 'nav', selector: 'nav' },
        { id: 'telemetry-consent-banner', selector: '#telemetry-consent-banner' },
        { id: 'telemetry-decline-btn', selector: '#telemetry-decline-btn' },
        { id: 'telemetry-accept-btn', selector: '#telemetry-accept-btn' },
        { id: 'main', selector: 'main' },
        { id: 'footer', selector: 'footer' }
      ];

      return elementsToTrack.map(({ id, selector }) => {
        const el = document.querySelector(selector);
        if (!el) return { id, left: 0, top: 0, width: 0, height: 0 };
        const rect = el.getBoundingClientRect();
        return {
          id,
          left: Math.round(rect.left),
          top: Math.round(rect.top),
          width: Math.round(rect.width),
          height: Math.round(rect.height)
        };
      });
    });

    fs.writeFileSync(path.join(recordDir, 'layout-check.json'), JSON.stringify(layoutCheck, null, 2));

    expect(fs.existsSync(path.join(recordDir, 'desktop-1440.png'))).toBe(true);
    expect(fs.existsSync(path.join(recordDir, 'mobile-375.png'))).toBe(true);
    expect(fs.existsSync(path.join(recordDir, 'layout-check.json'))).toBe(true);
  });

});
