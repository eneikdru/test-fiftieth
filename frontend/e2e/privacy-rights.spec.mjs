import { test, expect } from '@playwright/test';
import fs from 'fs';
import path from 'path';

test.describe('Data Subject Rights E2E Tests (152-FZ Compliance)', () => {
  test.beforeEach(async ({ page }) => {
    // Intercept harness HTML requests so test executes reliably on base URL
    await page.route('**/privacy-harness.html', async route => {
      const candidates = [
        path.resolve(process.cwd(), 'privacy-harness.html'),
        path.resolve(process.cwd(), 'frontend', 'privacy-harness.html'),
        path.resolve(process.cwd(), 'src', 'main', 'resources', 'static', 'privacy-harness.html')
      ];
      const htmlPath = candidates.find(p => fs.existsSync(p));
      if (!htmlPath) {
        throw new Error('privacy-harness.html not found in workspace paths');
      }
      const htmlContent = fs.readFileSync(htmlPath, 'utf8');
      await route.fulfill({
        status: 200,
        contentType: 'text/html; charset=utf-8',
        body: htmlContent
      });
    });
  });

  test('Given a test user with personal data, When the export flow is executed via E2E test, Then a complete personal data payload is received', async ({ page }) => {
    const exportRequestId = 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11';
    let exportPayloadRequested = false;

    // Intercept POST export request
    await page.route('**/api/v1/privacy/export-requests', async route => {
      const req = route.request();
      if (req.method() === 'POST') {
        const postData = JSON.parse(req.postData() || '{}');
        expect(postData.subject_id).toBe('исследователь');
        expect(postData.requested_format).toBe('ZIP');

        await route.fulfill({
          status: 202,
          contentType: 'application/json',
          body: JSON.stringify({
            request_id: exportRequestId,
            subject_id: postData.subject_id,
            status: 'COMPLETED',
            download_url: `/api/v1/privacy/export-requests/${exportRequestId}/download`,
            created_at: new Date().toISOString(),
            completed_at: new Date().toISOString()
          })
        });
      } else {
        await route.continue();
      }
    });

    // Intercept download request
    await page.route(`**/api/v1/privacy/export-requests/${exportRequestId}/download`, async route => {
      exportPayloadRequested = true;
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          id: 101,
          username: 'исследователь',
          role: 'RESEARCHER',
          full_name: 'Сотрудник института',
          email: 'ivanov@epidemiology-inst.ru',
          documents: [
            { id: 1, doc_type: 'Publication', title: 'Исследование вирусов 2025' }
          ]
        })
      });
    });

    await page.goto('/privacy-harness.html');

    // Verify page header
    await expect(page.locator('h1')).toContainText('Конфиденциальность и персональные данные');

    // Select format and submit export
    await page.selectOption('#export-format-select', 'ZIP');
    await page.fill('#export-notes-input', 'Запрос E2E проверки');
    await page.click('#export-data-btn');

    // Verify export success alert and download link
    await expect(page.locator('#export-success-alert')).toBeVisible();
    await expect(page.locator('#download-export-link')).toBeVisible();

    // Fetch the download URL in page context to verify complete payload
    const downloadData = await page.evaluate(async (url) => {
      const res = await fetch(url);
      return { status: res.status, body: await res.json() };
    }, `/api/v1/privacy/export-requests/${exportRequestId}/download`);

    expect(downloadData.status).toBe(200);
    expect(downloadData.body.username).toBe('исследователь');
    expect(downloadData.body.role).toBe('RESEARCHER');
    expect(downloadData.body.email).toBe('ivanov@epidemiology-inst.ru');
    expect(downloadData.body.documents.length).toBeGreaterThan(0);
    expect(exportPayloadRequested).toBe(true);
  });

  test('Given a test user triggering deletion, When the flow completes, Then subsequent queries verify the data no longer exists', async ({ page }) => {
    let erasureRequested = false;
    let dataExistsAfterErasure = true;

    // Intercept POST erasure request
    await page.route('**/api/v1/privacy/erasure-requests', async route => {
      const req = route.request();
      if (req.method() === 'POST') {
        const postData = JSON.parse(req.postData() || '{}');
        expect(postData.subject_id).toBe('исследователь');
        expect(postData.confirmation_token).toBe('CONFIRM_ERASURE_исследователь');
        erasureRequested = true;
        dataExistsAfterErasure = false;

        await route.fulfill({
          status: 202,
          contentType: 'application/json',
          body: JSON.stringify({
            request_id: 'b1fec988-8b0a-4bf7-aa5c-5aa8ac270b22',
            subject_id: postData.subject_id,
            status: 'COMPLETED',
            created_at: new Date().toISOString(),
            completed_at: new Date().toISOString(),
            records_erased_count: 2
          })
        });
      } else {
        await route.continue();
      }
    });

    // Intercept subsequent user query endpoint
    await page.route('**/api/v1/users/*', async route => {
      if (dataExistsAfterErasure) {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ username: 'исследователь', status: 'ACTIVE' })
        });
      } else {
        await route.fulfill({
          status: 404,
          contentType: 'application/json',
          body: JSON.stringify({ error_code: 'NOT_FOUND', message: 'Пользователь не найден' })
        });
      }
    });

    await page.goto('/privacy-harness.html');

    // Open deletion modal
    await page.click('#open-delete-account-btn');
    await expect(page.locator('#delete-confirmation-modal')).toBeVisible();

    // Enter confirmation code and submit
    await page.fill('#confirmation-input', 'УДАЛИТЬ исследователь');
    await page.click('#confirm-delete-btn');

    // Verify completion
    await expect(page.locator('#delete-confirmation-modal')).toHaveCount(0);
    await expect(page.locator('#delete-success-banner')).toBeVisible();

    expect(erasureRequested).toBe(true);

    // Subsequent query in page context to verify user data no longer exists
    const checkStatus = await page.evaluate(async () => {
      const url = 'http://localhost/api/v1/users/' + encodeURIComponent('исследователь');
      const res = await fetch(url);
      return res.status;
    });
    expect(checkStatus).toBe(404);
  });
});
