import { test, expect } from '@playwright/test';

const harnessPath = new URL('../test-harness.html?mode=login', import.meta.url).href;

test.describe('Authentication, Role Access, and Recovery E2E Tests', () => {

  test('Given an employee user, When the test attempts to access the upload UI, Then it is confirmed inaccessible', async ({ page }) => {
    // Navigate to the test harness page
    await page.goto(harnessPath);

    // Verify initial login page in Russian
    await expect(page.locator('h2')).toHaveText('Вход в систему');

    // Fill in employee credentials
    await page.fill('#username-input', 'employee_user');
    await page.fill('#password-input', 'EmployeeSecret123!');
    await page.click('button[type="submit"]');

    // Verify successful authentication as standard employee (RESEARCHER)
    await expect(page.locator('main')).toContainText('Роль: RESEARCHER');

    // Confirm upload and delete UI controls are strictly inaccessible / not rendered
    const uploadButton = page.locator('button:has-text("Загрузить документ")');
    const deleteButton = page.locator('button:has-text("Удалить")');
    await expect(uploadButton).toHaveCount(0);
    await expect(deleteButton).toHaveCount(0);

    // Verify employee message notice is shown explaining restricted permissions
    await expect(page.locator('#employee-message')).toBeVisible();
    await expect(page.locator('#employee-message')).toContainText('загрузка и удаление ограничены администратором института');
  });

  test('Given a locked-out user, When the test runs the self-service recovery flow, Then access is successfully restored', async ({ page }) => {
    // Navigate to test harness page
    await page.goto(harnessPath);

    // Click forgot password link to switch to recovery view
    await page.click('#forgot-password-link');
    await expect(page.locator('h2')).toHaveText('Забыли пароль?');

    // Submit recovery request with user email / identity
    await page.fill('#recovery-identity-input', 'locked_employee@epidemiology-inst.ru');
    await page.click('button[type="submit"]');

    // Verify recovery confirmation view and Russian message
    await expect(page.locator('h3')).toHaveText('Инструкции отправлены');
    await expect(page.locator('main')).toContainText('Инструкции по восстановлению пароля отправлены на ваш электронный адрес.');

    // Return to login
    await page.click('button:has-text("Вернуться ко входу")');
    await expect(page.locator('h2')).toHaveText('Вход в систему');
  });

  test('Given a logged-in user, When they click logout, Then they are logged out successfully', async ({ page }) => {
    await page.goto(harnessPath);
    await page.fill('#username-input', 'employee_user');
    await page.fill('#password-input', 'EmployeeSecret123!');
    await page.click('button[type="submit"]');
    await expect(page.locator('main')).toContainText('Роль: RESEARCHER');
    await page.click('button:has-text("Выйти из системы")');
    await expect(page.locator('h2')).toHaveText('Вход в систему');
  });
});
