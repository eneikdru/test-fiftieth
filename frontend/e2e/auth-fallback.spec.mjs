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

});
