import { test, expect } from '@playwright/test';
import path from 'path';
import fs from 'fs';

test.describe('Design Check Screenshots for Frontend Secure Authentication', () => {
  const SCREENSHOT_DIR = path.join(process.cwd(), '../.eneik/records/design-check-460a8f48-6884-4bfe-8f8b-562981bac2d5');

  test.beforeAll(() => {
    if (!fs.existsSync(SCREENSHOT_DIR)) {
      fs.mkdirSync(SCREENSHOT_DIR, { recursive: true });
    }
  });

  test('Generate 1440px desktop and 375px mobile screenshots', async ({ page }) => {
    // Desktop 1440px
    await page.setViewportSize({ width: 1440, height: 900 });
    await page.goto('/index.html?mode=login');
    await page.waitForSelector('h2:has-text("Вход в систему")');
    await page.fill('#username-input', 'admin_user');
    await page.fill('#password-input', 'SecretPassword123!');
    await page.screenshot({ path: path.join(SCREENSHOT_DIR, 'desktop-1440.png'), fullPage: true });

    // Mobile 375px
    await page.setViewportSize({ width: 375, height: 812 });
    await page.goto('/index.html?mode=login');
    await page.waitForSelector('h2:has-text("Вход в систему")');
    await page.fill('#username-input', 'employee_user');
    await page.fill('#password-input', 'SecretPassword123!');
    await page.screenshot({ path: path.join(SCREENSHOT_DIR, 'mobile-375.png'), fullPage: true });
  });
});
