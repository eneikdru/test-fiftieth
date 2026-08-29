import { test, expect } from '@playwright/test';

const harnessPath = '/test-harness.html?mode=login';

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

    // Complete password reset flow
    await page.fill('#recovery-token-input', 'rec_tok_test_restoration_123');
    await page.fill('#new-password-input', 'RestoredPass789!');
    await page.click('#reset-password-submit-btn');

    // Verify access restored confirmation
    await expect(page.locator('h3')).toHaveText('Доступ успешно восстановлен');
    await expect(page.locator('#recovery-success-message')).toContainText('Ваш пароль успешно изменен');

    // Return to login and authenticate with restored password
    await page.click('button:has-text("Перейти ко входу")');
    await expect(page.locator('h2')).toHaveText('Вход в систему');

    await page.fill('#username-input', 'locked_employee');
    await page.fill('#password-input', 'RestoredPass789!');
    await page.click('button[type="submit"]');
    await expect(page.locator('main')).toContainText('Роль: RESEARCHER');
  });

  test('Given the login UI is displayed, When accessed via a screen reader, Then all form controls are correctly labeled and accessible (WCAG AA)', async ({ page }) => {
    await page.goto(harnessPath);

    // Verify accessible labels for login form controls
    const usernameInput = page.locator('#username-input');
    await expect(usernameInput).toBeVisible();
    const usernameLabel = page.locator('label[for="username-input"]');
    await expect(usernameLabel).toBeVisible();
    await expect(usernameLabel).toContainText('Имя пользователя');

    const passwordInput = page.locator('#password-input');
    await expect(passwordInput).toBeVisible();
    const passwordLabel = page.locator('label[for="password-input"]');
    await expect(passwordLabel).toBeVisible();
    await expect(passwordLabel).toContainText('Пароль');

    // Check recovery form accessibility labels
    await page.click('#forgot-password-link');
    const recoveryInput = page.locator('#recovery-identity-input');
    await expect(recoveryInput).toBeVisible();
    const recoveryLabel = page.locator('label[for="recovery-identity-input"]');
    await expect(recoveryLabel).toBeVisible();
    await expect(recoveryLabel).toContainText('Электронная почта');
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
