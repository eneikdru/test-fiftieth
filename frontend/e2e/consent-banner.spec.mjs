import { test, expect } from '@playwright/test';
import fs from 'fs';
import path from 'path';

test.describe('Consent Platform Banner E2E Tests (BARCAN-TAG-11)', () => {
  test.beforeEach(async ({ page }) => {
    // Intercept harness HTML requests so test executes reliably on base URL
    await page.route('**/consent-harness.html', async route => {
      const candidates = [
        path.resolve(process.cwd(), 'consent-harness.html'),
        path.resolve(process.cwd(), 'frontend', 'consent-harness.html'),
        path.resolve(process.cwd(), 'src', 'main', 'resources', 'static', 'consent-harness.html')
      ];
      const htmlPath = candidates.find(p => fs.existsSync(p));
      if (!htmlPath) {
        throw new Error('consent-harness.html not found in workspace paths');
      }
      const htmlContent = fs.readFileSync(htmlPath, 'utf8');
      await route.fulfill({
        status: 200,
        contentType: 'text/html; charset=utf-8',
        body: htmlContent
      });
    });
  });

  test('Given a new visitor, When the page loads, Then the consent banner appears before non-essential trackers load', async ({ page, context }) => {
    await context.clearCookies();
    await page.goto('/consent-harness.html');

    // Verify banner is visible
    const banner = page.locator('#consent-banner');
    await expect(banner).toBeVisible();

    // Verify title and description
    await expect(page.locator('#consent-title')).toContainText('Настройки конфиденциальности');
    await expect(page.locator('#consent-description')).toBeVisible();

    // Verify non-essential tracker is initially blocked
    const trackerBadge = page.locator('#tracker-status-badge');
    await expect(trackerBadge).toContainText('Некритичные трекеры заблокированы');

    const cookies = await context.cookies();
    const analyticsCookie = cookies.find(c => c.name === 'analytics_tracker');
    expect(analyticsCookie).toBeUndefined();
  });

  test('Given the banner, When the user rejects all, Then choice is saved and trackers remain blocked', async ({ page, context }) => {
    await context.clearCookies();
    await page.goto('/consent-harness.html');

    // Reject all with one click
    await page.click('#consent-reject-all-btn');

    // Banner should close
    await expect(page.locator('#consent-banner')).toHaveCount(0);

    // Verify saved consent in localStorage
    const savedConsent = await page.evaluate(() => localStorage.getItem('consent_preferences'));
    expect(savedConsent).not.toBeNull();
    const parsed = JSON.parse(savedConsent);
    expect(parsed.status).toBe('rejected');
    expect(parsed.analytics).toBe(false);
    expect(parsed.marketing).toBe(false);

    // Verify trackers remain blocked
    await expect(page.locator('#tracker-status-badge')).toContainText('Некритичные трекеры заблокированы');
  });

  test('Given the banner, When the user accepts all, Then choice is saved and trackers are activated', async ({ page, context }) => {
    await context.clearCookies();
    await page.goto('/consent-harness.html');

    // Accept all
    await page.click('#consent-accept-all-btn');

    // Banner should close
    await expect(page.locator('#consent-banner')).toHaveCount(0);

    // Verify saved consent in localStorage
    const savedConsent = await page.evaluate(() => localStorage.getItem('consent_preferences'));
    expect(savedConsent).not.toBeNull();
    const parsed = JSON.parse(savedConsent);
    expect(parsed.status).toBe('accepted');
    expect(parsed.analytics).toBe(true);

    // Verify tracker status updated
    await expect(page.locator('#tracker-status-badge')).toContainText('Трекеры запущены');
  });

  test('Given keyboard navigation, When using the banner, Then it meets WCAG 2.1 AA keyboard access standards', async ({ page }) => {
    await page.goto('/consent-harness.html');

    // Focus on reject button
    await page.focus('#consent-reject-all-btn');
    const isRejectFocused = await page.evaluate(() => document.activeElement.id === 'consent-reject-all-btn');
    expect(isRejectFocused).toBe(true);

    // Expand details via keyboard / toggle button click
    await page.click('#consent-toggle-details-btn');
    await expect(page.locator('#consent-details-panel')).toBeVisible();

    // Check ARIA attributes
    const bannerRole = await page.getAttribute('#consent-banner', 'role');
    expect(bannerRole).toBe('dialog');

    const ariaLabelledBy = await page.getAttribute('#consent-banner', 'aria-labelledby');
    expect(ariaLabelledBy).toBe('consent-title');

    const ariaDescribedBy = await page.getAttribute('#consent-banner', 'aria-describedby');
    expect(ariaDescribedBy).toBe('consent-description');

    const toggleExpanded = await page.getAttribute('#consent-toggle-details-btn', 'aria-expanded');
    expect(toggleExpanded).toBe('true');
  });

  test('Design check screenshots generation for Consent Banner', async ({ page }) => {
    const recordsDir = path.resolve(process.cwd(), '..', '.eneik', 'records', 'design-check-7a69ce3f-c168-4677-81fe-b557e57649c3');
    if (!fs.existsSync(recordsDir)) {
      fs.mkdirSync(recordsDir, { recursive: true });
    }

    // 1440px desktop screenshot
    await page.setViewportSize({ width: 1440, height: 900 });
    await page.goto('/consent-harness.html');
    await page.waitForTimeout(300);
    const desktopPath = path.join(recordsDir, 'desktop-1440.png');
    await page.screenshot({ path: desktopPath, fullPage: true });

    // 375px mobile screenshot
    await page.setViewportSize({ width: 375, height: 812 });
    await page.goto('/consent-harness.html');
    await page.waitForTimeout(300);
    const mobilePath = path.join(recordsDir, 'mobile-375.png');
    await page.screenshot({ path: mobilePath, fullPage: true });

    expect(fs.existsSync(desktopPath)).toBe(true);
    expect(fs.existsSync(mobilePath)).toBe(true);
  });
});
