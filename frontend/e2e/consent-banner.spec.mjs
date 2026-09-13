import { test, expect } from '@playwright/test';
import fs from 'node:fs';
import path from 'node:path';

test.describe('Frontend Consent Banner and Opt-in Telemetry Blocking', () => {

  test.beforeEach(async ({ page }) => {
    // Clear localStorage before each test
    await page.goto('/');
    await page.evaluate(() => localStorage.clear());
    await page.reload();
  });

  test('Given a new visitor, When loading the application, Then a WCAG 2.1 AA consent banner is shown and telemetry is blocked', async ({ page }) => {
    await page.goto('/');

    // Verify consent banner is displayed
    const banner = page.locator('#consent-banner');
    await expect(banner).toBeVisible();

    // Verify WCAG accessibility attributes
    await expect(banner).toHaveAttribute('role', 'region');
    await expect(banner).toHaveAttribute('aria-label', 'Согласие на использование файлов cookie и аналитики');

    // Verify buttons exist
    const acceptBtn = page.locator('#consent-accept-btn');
    const declineBtn = page.locator('#consent-decline-btn');
    await expect(acceptBtn).toBeVisible();
    await expect(declineBtn).toBeVisible();
    await expect(acceptBtn).toContainText('Принять все');
    await expect(declineBtn).toContainText('Отклонить');

    // Click around application while unconsented
    await page.click('#search-query-input');
    await page.click('#search-submit-btn');

    // Switch to Telemetry tab
    await page.click('#tab-telemetry');
    await expect(page.locator('#panel-telemetry')).toBeVisible();

    // Verify telemetry total clicks remain 0 while unconsented
    const clickDistanceText = await page.locator('#metric-click-distance-value').innerText();
    expect(clickDistanceText).toContain('0.0');
  });

  test('Given a visitor, When clicking "Принять все", Then consent is recorded and telemetry tracking is enabled', async ({ page }) => {
    await page.goto('/');

    // Click accept button
    await page.click('#consent-accept-btn');

    // Banner should hide
    await expect(page.locator('#consent-banner')).not.toBeVisible();

    // Verify localStorage key
    const consentVal = await page.evaluate(() => localStorage.getItem('cookie_consent'));
    expect(consentVal).toBe('accepted');

    // Click elements and check telemetry update
    await page.click('#tab-telemetry');
    await expect(page.locator('#panel-telemetry')).toBeVisible();

    const loadSuccessText = await page.locator('#metric-tab-load-success-value').innerText();
    expect(loadSuccessText).toContain('100');
  });

  test('Given a visitor, When clicking "Отклонить", Then consent choice is recorded as declined and banner hides', async ({ page }) => {
    await page.goto('/');

    // Click decline button
    await page.click('#consent-decline-btn');

    // Banner should hide
    await expect(page.locator('#consent-banner')).not.toBeVisible();

    // Verify localStorage key
    const consentVal = await page.evaluate(() => localStorage.getItem('cookie_consent'));
    expect(consentVal).toBe('declined');
  });

  test('Design verification screenshots', async ({ page }) => {
    const rootRepoDir = path.resolve(process.cwd(), '..');
    const screenshotDir = path.join(rootRepoDir, '.eneik/records/design-check-550727fa-e327-4180-8fb2-eb537224e577');
    fs.mkdirSync(screenshotDir, { recursive: true });

    // 1. Desktop Viewport (1440px)
    await page.setViewportSize({ width: 1440, height: 900 });
    await page.goto('/');
    await expect(page.locator('#consent-banner')).toBeVisible();
    await page.screenshot({ path: path.join(screenshotDir, 'desktop-1440.png'), fullPage: true });

    // 2. Mobile Viewport (375px)
    await page.setViewportSize({ width: 375, height: 812 });
    await page.goto('/');
    await expect(page.locator('#consent-banner')).toBeVisible();
    await page.screenshot({ path: path.join(screenshotDir, 'mobile-375.png'), fullPage: true });

    expect(fs.existsSync(path.join(screenshotDir, 'desktop-1440.png'))).toBe(true);
    expect(fs.existsSync(path.join(screenshotDir, 'mobile-375.png'))).toBe(true);
  });

});
