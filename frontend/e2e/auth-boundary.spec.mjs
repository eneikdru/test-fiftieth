import { test, expect } from '@playwright/test';

const harnessPath = '/test-harness.html?mode=login';

test.describe('Authentication Boundary Validation E2E Tests', () => {

  test('Given valid credentials, When a user attempts to log in, Then the system grants access to protected data', async ({ page }) => {
    await page.goto(harnessPath);

    await expect(page.locator('h2')).toHaveText('Вход в систему');

    await page.fill('#username-input', 'employee_user');
    await page.fill('#password-input', 'EmployeeSecret123!');
    await page.click('button[type="submit"]');

    // System grants access and navigates to catalog / protected view
    await expect(page.locator('main')).toBeVisible();
    await expect(page.locator('main')).toContainText('База знаний по эпидемиологии');
    await expect(page.locator('main')).toContainText('Роль: RESEARCHER');
  });

  test('Given invalid credentials, When a user attempts to log in, Then access is denied and an error message is shown', async ({ page }) => {
    await page.goto(harnessPath);

    await expect(page.locator('h2')).toHaveText('Вход в систему');

    await page.fill('#username-input', 'invalid_user');
    await page.fill('#password-input', 'WrongPassword123!');
    await page.click('button[type="submit"]');

    // System denies access and displays error message in Russian
    const errorMessage = page.locator('#login-error-message');
    await expect(errorMessage).toBeVisible();
    await expect(errorMessage).toContainText('Неверное имя пользователя или пароль');

    // Confirm system remains on login form and does not render protected content
    await expect(page.locator('h2')).toHaveText('Вход в систему');
    await expect(page.locator('.catalog-container')).toHaveCount(0);
  });

});
