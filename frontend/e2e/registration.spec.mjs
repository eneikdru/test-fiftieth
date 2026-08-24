import { test, expect } from '@playwright/test';
import fs from 'fs';
import path from 'path';

test.describe('Registration and Onboarding Flow', () => {
  const SCREENSHOT_DIR = path.join(process.cwd(), '../.eneik/records/design-check-49ab0369-19a0-4339-9211-0f773901eec3');

  test.beforeAll(() => {
    if (!fs.existsSync(SCREENSHOT_DIR)) {
      fs.mkdirSync(SCREENSHOT_DIR, { recursive: true });
    }
  });

  test('Mobile interaction - Validation and preservation of input', async ({ page }) => {
    // Mobile viewport
    await page.setViewportSize({ width: 375, height: 667 });
    await page.goto('http://localhost:18080/registration-harness.html');

    // Wait for Svelte app to mount
    await page.waitForTimeout(1000);

    // Navigate to registration
    await page.click('button:has-text("Создать аккаунт")');
    await page.waitForSelector('h2:has-text("Создать аккаунт")');

    // Fill in partial form (trigger validation error)
    await page.fill('input#username', 'testuser');
    await page.fill('input#email', 'test@example.com');
    await page.fill('input#password', 'password123');
    await page.fill('input#confirm-password', 'wrongpassword'); // mismatch

    // Submit
    await page.click('button[type="submit"]');

    // Expect error
    await expect(page.locator('div[role="alert"]')).toContainText('Пароли не совпадают');

    // Expect typed input preserved
    await expect(page.locator('input#username')).toHaveValue('testuser');
    await expect(page.locator('input#email')).toHaveValue('test@example.com');

    // Fix password
    await page.fill('input#confirm-password', 'password123');

    // Mobile screenshot on registration form
    await page.screenshot({ path: path.join(SCREENSHOT_DIR, 'mobile-375.png'), fullPage: true });

    // Submit successfully
    await page.click('button[type="submit"]');

    // Expect onboarding step 1
    await page.waitForSelector('h2:has-text("Ваш профиль")');
  });

  test('Desktop interaction - Full onboarding flow', async ({ page }) => {
    // Desktop viewport
    await page.setViewportSize({ width: 1440, height: 900 });
    await page.goto('http://localhost:18080/registration-harness.html');

    // Wait for Svelte app to mount
    await page.waitForTimeout(1000);

    // Screenshot landing page (for complete flow visibility, but we need specific files for the gate)
    // Actually the design check screenshot could be on the Dashboard step, or any step really
    // Let's take the desktop screenshot on the final onboarding step to show the full flow was completed.

    // Landing -> Registration
    await page.click('button:has-text("Создать аккаунт")');
    await page.waitForSelector('h2:has-text("Создать аккаунт")');

    // Fill Registration correctly
    await page.fill('input#username', 'dr.smith');
    await page.fill('input#email', 'smith@epidemiology-inst.ru');
    await page.fill('input#password', 'securePass123');
    await page.fill('input#confirm-password', 'securePass123');
    await page.click('button[type="submit"]');

    // Onboarding Step 1
    await page.waitForSelector('h2:has-text("Ваш профиль")');
    await page.fill('input#firstName', 'Ivan');
    await page.fill('input#lastName', 'Ivanov');
    await page.fill('input#organization', 'Lab 4');
    await page.click('button[type="submit"]');

    // Onboarding Step 2
    await page.waitForSelector('h2:has-text("Настройка рабочего пространства")');

    // Desktop screenshot on final step
    await page.screenshot({ path: path.join(SCREENSHOT_DIR, 'desktop-1440.png'), fullPage: true });

    // Finish setup
    await page.on('dialog', dialog => dialog.accept());
    await page.click('button:has-text("Завершить настройку")');
  });
});
