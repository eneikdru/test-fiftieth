import { test, expect } from '@playwright/test';

const harnessPath = '/test-harness.html?mode=login';

test.describe('Authentication Boundary Validation E2E Tests', () => {

  test('Given a running application, When E2E tests exercise the login form with valid credentials, Then the system must grant access to the protected data', async ({ page }) => {
    await page.goto(harnessPath);

    await expect(page.locator('h2')).toHaveText('Вход в систему');
    await page.fill('#username-input', 'admin_user');
    await page.fill('#password-input', 'ValidPassword123!');
    await page.click('button[type="submit"]');

    await expect(page.locator('h1')).toContainText('База знаний по эпидемиологии');
    await expect(page.locator('#document-grid')).toBeVisible();
  });

  test('Given invalid credentials, When the tests attempt to log in, Then access must be explicitly denied and an error shown', async ({ page }) => {
    await page.goto(harnessPath);

    await expect(page.locator('h2')).toHaveText('Вход в систему');
    await page.fill('#username-input', 'invalid_user');
    await page.fill('#password-input', 'WrongPassword!');

    // Mock backend endpoint to reject invalid credentials
    await page.route('/api/v1/auth/login', route => {
      route.fulfill({
        status: 401,
        contentType: 'application/json',
        body: JSON.stringify({
          error_code: 'INVALID_CREDENTIALS',
          message: 'Неверное имя пользователя или пароль.'
        })
      });
    });

    await page.click('button[type="submit"]');

    const errorAlert = page.locator('#login-error-alert');
    await expect(errorAlert).toBeVisible();
    await expect(errorAlert).toContainText('Неверное имя пользователя или пароль');
  });

});
