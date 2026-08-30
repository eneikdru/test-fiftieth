import { test, expect } from '@playwright/test';

test.describe('Autonomous Fallback Authentication E2E', () => {

  test('Given an E2E test, When Moodle is simulated as offline, Then the user can successfully log in using the autonomous fallback mechanism.', async ({ page }) => {
    await page.goto('/test-harness.html?mode=login');

    await expect(page.locator('h2')).toHaveText('Вход в систему');

    await page.fill('#username-input', 'admin_anna');
    await page.fill('#password-input', 'AdminPass456!');

    // Simulate network error using playwright route interception
    await page.route('/api/v1/auth/sso/moodle', route => route.abort('failed'));

    await page.click('button[type="submit"]');

    await expect(page.locator('main')).toContainText('Роль: Администратор');
  });
});
