import { test, expect } from '@playwright/test';
import fs from 'fs';
import path from 'path';

test.describe('Registration and Onboarding Flow', () => {
  const SCREENSHOT_DIR = path.join(process.cwd(), 'test-results', 'design-check-registration');

  test.beforeAll(() => {
    if (!fs.existsSync(SCREENSHOT_DIR)) {
      fs.mkdirSync(SCREENSHOT_DIR, { recursive: true });
    }
  });

  test.beforeEach(async ({ page }) => {
    // Intercept harness HTML requests so test executes reliably on base URL offline
    await page.route('**/registration-harness.html', async route => {
      const candidates = [
        path.resolve(process.cwd(), 'registration-harness.html'),
        path.resolve(process.cwd(), 'frontend', 'registration-harness.html')
      ];
      const htmlPath = candidates.find(p => fs.existsSync(p));
      if (!htmlPath) {
        throw new Error('registration-harness.html not found in workspace paths');
      }
      const htmlContent = fs.readFileSync(htmlPath, 'utf8');
      await route.fulfill({
        status: 200,
        contentType: 'text/html; charset=utf-8',
        body: htmlContent
      });
    });
  });

  test('Given the RegistrationForm, When invalid data is entered, Then real-time validation errors are visually rendered', async ({ page }) => {
    await page.goto('/registration-harness.html');
    await page.click('button:has-text("Создать аккаунт")');
    await page.waitForSelector('h2:has-text("Создать аккаунт")');

    // Type invalid short username
    await page.fill('input#username', 'ab');
    await page.evaluate(() => document.querySelector('input#username').blur());
    await expect(page.locator('.validation-error', { hasText: 'Имя пользователя должно быть не менее 3 символов' })).toBeVisible();

    // Type invalid email format
    await page.fill('input#email', 'invalid-email');
    await page.evaluate(() => document.querySelector('input#email').blur());
    await expect(page.locator('.validation-error', { hasText: 'Введите корректный адрес электронной почты' })).toBeVisible();

    // Type mismatched password
    await page.fill('input#password', 'password123');
    await page.fill('input#confirm-password', 'wrongpassword');
    await page.evaluate(() => document.querySelector('input#confirm-password').blur());

    // Submit
    await page.click('button[type="submit"]');

    // Expect error alert
    await expect(page.locator('#registration-error-alert')).toContainText('Пароли не совпадают');
  });

  test('Mobile interaction - Validation and preservation of input', async ({ page }) => {
    // Mobile viewport
    await page.setViewportSize({ width: 375, height: 667 });
    await page.goto('/registration-harness.html');

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
    await expect(page.locator('#registration-error-alert')).toContainText('Пароли не совпадают');

    // Expect typed input preserved
    await expect(page.locator('input#username')).toHaveValue('testuser');
    await expect(page.locator('input#email')).toHaveValue('test@example.com');

    // Fix password
    await page.fill('input#confirm-password', 'password123');

    // Submit successfully
    await page.click('button[type="submit"]');

    // Expect onboarding step 1
    await page.waitForSelector('h2:has-text("Ваш профиль")');
  });

  test('Desktop interaction - Full onboarding flow', async ({ page }) => {
    // Desktop viewport
    await page.setViewportSize({ width: 1440, height: 900 });
    await page.goto('/registration-harness.html');

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

    // Finish setup
    await page.on('dialog', dialog => dialog.accept());
    await page.click('button:has-text("Завершить настройку")');
  });
});
