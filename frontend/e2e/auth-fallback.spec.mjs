import { test, expect } from '@playwright/test';

const harnessPath = '/test-harness.html?mode=login';

test.describe('Moodle Fallback E2E Tests', () => {

  test('Given the external Moodle LMS is unreachable, When a user attempts to log in, Then the system should allow them to authenticate via an autonomous fallback mechanism using local credentials', async ({ page }) => {
    await page.goto(harnessPath);

    await expect(page.locator('h2')).toHaveText('Вход в систему');
    await page.fill('#username-input', 'moodle_user');
    await page.fill('#password-input', 'MySecureFallback!');
    await page.click('button[type="submit"]');

    await expect(page.locator('main')).toContainText('Администратор');
  });

  test('Given an unauthenticated user on the login screen, When they view login options, Then Moodle SSO button is displayed with visible feedback on click', async ({ page }) => {
    await page.goto('/index.html?mode=login');

    await expect(page.locator('h2:has-text("Вход в систему")')).toBeVisible();
    const ssoBtn = page.locator('#moodle-sso-btn');
    await expect(ssoBtn).toBeVisible();
    await expect(ssoBtn).toContainText('Войти через Moodle SSO');

    // Mock Moodle SSO endpoint failure to verify error feedback UI
    await page.route('**/api/v1/auth/moodle/config', route => {
      route.fulfill({
        status: 500,
        contentType: 'application/json',
        body: JSON.stringify({ error_code: 'SSO_UNAVAILABLE', message: 'Moodle SSO сервис недоступен' })
      });
    });

    // Click Moodle SSO button and check feedback / error handling
    await ssoBtn.click();
    await expect(page.locator('#login-error-alert')).toBeVisible();
    await expect(page.locator('#login-error-alert')).toContainText('Moodle SSO');
  });

  test('Given an administrator on the main console, When they view role hierarchy, Then Moodle role mappings are displayed and input survives on failure', async ({ page }) => {
    await page.goto('/index.html?mode=catalog');

    // Section exists for administrator
    const section = page.locator('#moodle-role-hierarchy-section');
    await expect(section).toBeVisible();

    // Verify existing mapping table
    await expect(page.locator('#moodle-role-mappings-list')).toContainText('администратор');

    // Enable failure simulation checkbox
    await page.check('#simulate-mapping-error-checkbox');

    // Fill new role pattern
    await page.fill('#moodle-pattern-input', 'методист');
    await page.click('#save-role-mapping-btn');

    // Error shown, but what the user typed survives on failure
    await expect(page.locator('#moodle-role-hierarchy-section [role="alert"]')).toBeVisible();
    await expect(page.locator('#moodle-pattern-input')).toHaveValue('методист');
  });

});
