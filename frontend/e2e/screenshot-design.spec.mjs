import { test, expect } from '@playwright/test';
import fs from 'fs';
import path from 'path';

test('Design verification screenshots for Telemetry Consent', async ({ page }) => {
  const recordsDir = path.resolve('.eneik/records/design-check-c4650feb-8810-4237-b19c-0d755e2dc7fe');
  fs.mkdirSync(recordsDir, { recursive: true });

  await page.goto('/index.html?mode=catalog');
  await page.waitForTimeout(500);

  // Desktop
  await page.setViewportSize({ width: 1440, height: 900 });
  await page.screenshot({ path: path.join(recordsDir, 'desktop-1440.png') });

  // Mobile
  await page.setViewportSize({ width: 375, height: 812 });
  await page.screenshot({ path: path.join(recordsDir, 'mobile-375.png') });

  // Layout check
  const bounds = await page.evaluate(() => {
    const banner = document.querySelector('div[role="dialog"]');
    return banner ? [{
      id: 'consent-banner',
      left: banner.getBoundingClientRect().left,
      top: banner.getBoundingClientRect().top,
      width: banner.getBoundingClientRect().width,
      height: banner.getBoundingClientRect().height
    }] : [];
  });

  fs.writeFileSync(
    path.join(recordsDir, 'layout-check.json'),
    JSON.stringify(bounds, null, 2)
  );
});