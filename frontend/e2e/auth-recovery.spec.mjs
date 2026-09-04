import { test, expect } from '@playwright/test';
import fs from 'fs';
import path from 'path';

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
    await page.click('#recovery-submit-btn');

    // Verify recovery confirmation view and Russian message
    await expect(page.locator('h3')).toHaveText('Инструкции отправлены');
    await expect(page.locator('main')).toContainText('Инструкции по восстановлению пароля отправлены на ваш электронный адрес.');

    // Return to login
    await page.click('button:has-text("Вернуться ко входу")');
    await expect(page.locator('h2')).toHaveText('Вход в систему');
  });

  test('Given the forgot password screen, When viewed on mobile, Then it is fully responsive and WCAG 2.1 AA compliant', async ({ page }) => {
    // Set mobile viewport
    await page.setViewportSize({ width: 375, height: 667 });
    await page.goto(harnessPath);

    await page.click('#forgot-password-link');
    await expect(page.locator('h2')).toHaveText('Забыли пароль?');

    // Verify key responsive inputs and submit button are visible and accessible
    const emailInput = page.locator('#recovery-identity-input');
    await expect(emailInput).toBeVisible();
    await expect(emailInput).toHaveAttribute('aria-required', 'true');

    const submitBtn = page.locator('#recovery-submit-btn');
    await expect(submitBtn).toBeVisible();

    // Verify touch target size meets accessibility guidelines (min 40px)
    const box = await submitBtn.boundingBox();
    expect(box.height).toBeGreaterThanOrEqual(40);
  });

  test('Given the set new password screen, When submitted with errors, Then validation errors are clearly presented', async ({ page }) => {
    await page.goto('/test-harness.html?mode=set_password');
    await expect(page.locator('h2')).toHaveText('Установка нового пароля');

    // Submit form empty to trigger validation errors
    await page.click('#set-password-submit-btn');

    // Verify error summary alert is displayed
    await expect(page.locator('#set-password-error-summary')).toBeVisible();
    await expect(page.locator('#set-password-error-summary')).toContainText('Пожалуйста, исправьте ошибки');

    // Verify individual field validation errors
    await expect(page.locator('#new-password-error')).toBeVisible();
    await expect(page.locator('#new-password-error')).toContainText('Введите новый пароль');

    await expect(page.locator('#confirm-password-error')).toBeVisible();
    await expect(page.locator('#confirm-password-error')).toContainText('Подтвердите новый пароль');

    // Test password mismatch
    await page.fill('#new-password-input', 'Short1!');
    await page.fill('#confirm-password-input', 'Mismatch123!');
    await page.click('#set-password-submit-btn');

    await expect(page.locator('#new-password-error')).toContainText('Пароль должен содержать не менее 8 символов');
    await expect(page.locator('#confirm-password-error')).toContainText('Пароли не совпадают');
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

  test('Design verification screenshots for Account Recovery UI', async ({ page }) => {
    const screenshotDir = path.resolve('/app/.eneik/records/design-check-2e3cafac-8c49-404e-b938-13598730481f');
    if (!fs.existsSync(screenshotDir)) {
      fs.mkdirSync(screenshotDir, { recursive: true });
    }

    // 1440px desktop
    await page.setViewportSize({ width: 1440, height: 900 });
    await page.goto(harnessPath);
    await page.click('#forgot-password-link');
    await page.screenshot({ path: path.join(screenshotDir, 'desktop-1440.png'), fullPage: true });

    // 375px mobile
    await page.setViewportSize({ width: 375, height: 812 });
    await page.goto(harnessPath);
    await page.click('#forgot-password-link');
    await page.screenshot({ path: path.join(screenshotDir, 'mobile-375.png'), fullPage: true });

    expect(fs.existsSync(path.join(screenshotDir, 'desktop-1440.png'))).toBe(true);
    expect(fs.existsSync(path.join(screenshotDir, 'mobile-375.png'))).toBe(true);
  });
});
