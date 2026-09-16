import { test, expect } from '@playwright/test';

test.describe('Moodle Role Hierarchy Display', () => {
  test('should display mapped Moodle role hierarchy when administrator views role settings', async ({ page }) => {
    // Intercept Moodle role hierarchy endpoint
    await page.route('**/api/v1/auth/moodle/role-hierarchy', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          mappings: [
            { id: 1, moodle_role_pattern: 'администратор', internal_role: 'ADMIN' },
            { id: 2, moodle_role_pattern: 'эпидемиолог', internal_role: 'EPIDEMIOLOGIST' },
            { id: 3, moodle_role_pattern: 'исследователь', internal_role: 'RESEARCHER' },
            { id: 4, moodle_role_pattern: 'студент', internal_role: 'USER' }
          ]
        })
      });
    });

    // Navigate to Moodle override harness page
    await page.goto('/moodle-override-harness.html');

    // Verify panel header
    const hierarchyHeading = page.locator('text=Иерархия маппинга ролей Moodle');
    await expect(hierarchyHeading).toBeVisible();

    // Verify mapped hierarchy table
    const listLocator = page.locator('#hierarchy-mapping-list');
    await expect(listLocator).toBeVisible();

    await expect(listLocator.getByText('администратор', { exact: false }).first()).toBeVisible();
    await expect(listLocator.getByText('Администратор (ADMIN)', { exact: false })).toBeVisible();
    await expect(listLocator.getByText('Эпидемиолог (EPIDEMIOLOGIST)', { exact: false })).toBeVisible();
  });
});
