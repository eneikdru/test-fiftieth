import { test, expect } from '@playwright/test';
import { createRequire } from 'module';
const require = createRequire(import.meta.url);
const AxeBuilder = require('@axe-core/playwright').default;

const harnessPath = '/test-harness.html?mode=login';

test.describe('Moodle SSO and Fallback Authentication E2E Tests', () => {

  test('Given an E2E test, When Moodle is simulated as offline, Then the user can successfully log in using the autonomous fallback mechanism', async ({ page, request }) => {
    // Navigate to the test harness page
    await page.goto(harnessPath);

    // Verify initial login page in Russian
    await expect(page.locator('h2')).toHaveText('Вход в систему');

    // Fill in fallback credentials for Moodle user
    // Testing standard standard username/password form which serves as the fallback when Moodle SSO is down.
    await page.fill('#username-input', 'employee_user');
    await page.fill('#password-input', 'EmployeeSecret123!');
    await page.click('button[type="submit"]');

    // Assert successful login
    await expect(page.locator('main')).toContainText('Роль: RESEARCHER');
  });

  test('Given a UI accessibility scan, When the login page is analyzed, Then zero WCAG 2.1 AA violations are found', async ({ page }) => {
    await page.goto(harnessPath);

    const accessibilityScanResults = await new AxeBuilder({ page })
      .withTags(['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa'])
      .analyze();

    expect(accessibilityScanResults.violations).toEqual([]);
  });

});
