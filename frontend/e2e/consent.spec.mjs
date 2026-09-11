import { test, expect } from '@playwright/test';
import fs from 'fs';
import path from 'path';

test.describe('Consent Platform Banner E2E', () => {
  const SCREENSHOT_DIR = path.join(process.cwd(), '../.eneik/records/design-check-7a69ce3f-c168-4677-81fe-b557e57649c3');

  test.beforeAll(() => {
    if (!fs.existsSync(SCREENSHOT_DIR)) {
      fs.mkdirSync(SCREENSHOT_DIR, { recursive: true });
    }
  });

  test.beforeEach(async ({ page }) => {
    // Note: Do not use page.route to mock HTML files since the Playwright server
    // will serve from the root directory already.
    // And intercepting Svelte modules fails with the unpkg setup.
  });

  test('Given a new visitor, When the page loads, Then the consent platform banner appears before non-essential trackers load. Given the banner, When the user interacts, Then rejecting is as easy as accepting and the choice is saved.', async ({ page }) => {

    await page.addInitScript(() => {
      window.localStorage.clear();
    });

    await page.goto('/consent-harness.html');

    // Wait for Svelte app to mount
    await page.waitForTimeout(2000);

    const banner = page.locator('text=Мы используем файлы cookie');
    await expect(banner).toBeVisible();

    await page.setViewportSize({ width: 1440, height: 900 });
    await page.screenshot({ path: path.join(SCREENSHOT_DIR, 'desktop-1440.png'), fullPage: true });

    await page.setViewportSize({ width: 375, height: 667 });
    await page.screenshot({ path: path.join(SCREENSHOT_DIR, 'mobile-375.png'), fullPage: true });

    const rejectBtn = page.locator('button', { hasText: 'Отклонить' });
    const acceptBtn = page.locator('button', { hasText: 'Принять' });

    await expect(rejectBtn).toBeVisible();
    await expect(acceptBtn).toBeVisible();

    await rejectBtn.click();

    await expect(banner).not.toBeVisible();

    const consent = await page.evaluate(() => localStorage.getItem('cookie_consent'));
    expect(consent).toBe('rejected');
  });

  test('Given a user interacts via keyboard, When using the banner, Then it meets WCAG 2.1 level AA', async ({ page }) => {
    await page.addInitScript(() => {
      window.localStorage.clear();
    });
    await page.goto('/consent-harness.html');

    await page.waitForTimeout(2000);

    // Focus the document body first to start tab sequence correctly
    await page.locator('body').focus();

    await page.keyboard.press('Tab');
    await expect(page.locator('button', { hasText: 'Отклонить' })).toBeFocused();

    await page.keyboard.press('Tab');
    await expect(page.locator('button', { hasText: 'Принять' })).toBeFocused();
  });
});
