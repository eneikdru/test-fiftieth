import { test, expect } from '@playwright/test';
import path from 'node:path';
import fs from 'node:fs';

test.describe('Design Check Screenshots for Moodle Role Hierarchy UI Display', () => {
  test('Given the Moodle role management interface, When rendered, Then capture desktop and mobile design check screenshots', async ({ page }) => {
    const recordDir = path.resolve('/app/.eneik/records/design-check-d7580f6a-5599-429d-a9f5-178c79cae03a');
    if (!fs.existsSync(recordDir)) {
      fs.mkdirSync(recordDir, { recursive: true });
    }

    // Desktop Viewport (1440px)
    await page.setViewportSize({ width: 1440, height: 900 });
    await page.goto('/moodle-role-override-harness.html');
    await page.waitForSelector('table[aria-label="Таблица сопоставления ролей Moodle"]');
    await page.screenshot({ path: path.join(recordDir, 'desktop-1440.png'), fullPage: true });

    // Mobile Viewport (375px)
    await page.setViewportSize({ width: 375, height: 812 });
    await page.goto('/moodle-role-override-harness.html');
    await page.waitForSelector('table[aria-label="Таблица сопоставления ролей Moodle"]');
    await page.screenshot({ path: path.join(recordDir, 'mobile-375.png'), fullPage: true });

    expect(fs.existsSync(path.join(recordDir, 'desktop-1440.png'))).toBeTruthy();
    expect(fs.existsSync(path.join(recordDir, 'mobile-375.png'))).toBeTruthy();
  });
});
