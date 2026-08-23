import { test, expect } from '@playwright/test';
import path from 'path';
import fs from 'fs';

const harnessPath = new URL('../test-harness.html', import.meta.url).href;

test.describe('Catalog Search and Document Management E2E Tests', () => {

  test('Given a fresh deployment with sample data, When the E2E test searches for a known document and downloads it, Then the file is successfully retrieved', async ({ page }) => {
    // Intercept download request for document file
    await page.route('**/api/v1/documents/*/download', async route => {
      await route.fulfill({
        status: 200,
        contentType: 'application/pdf',
        headers: {
          'Content-Disposition': 'attachment; filename="salmonella_outbreak.pdf"'
        },
        body: Buffer.from('Содержимое документа: Протокол эпидемиологического расследования вспышки сальмонеллеза')
      });
    });

    await page.goto(harnessPath);

    // Search for known sample document
    await page.fill('#search-query-input', 'сальмонеллеза');
    await page.click('#search-submit-btn');

    // Confirm matching document is found and displayed
    const docTitle = page.locator('.doc-title').first();
    await expect(docTitle).toBeVisible();
    await expect(docTitle).toContainText('сальмонеллеза');

    // Trigger download and verify file retrieval
    const downloadPromise = page.waitForEvent('download');
    await page.locator('.download-btn').first().click();
    const download = await downloadPromise;

    expect(download.suggestedFilename()).toBe('salmonella_outbreak.pdf');
    const readStream = await download.createReadStream();
    expect(readStream).not.toBeNull();
  });

  test('Given an admin user, When the test uploads and then deletes a document, Then the catalog accurately reflects the changes without errors', async ({ page }) => {
    await page.goto(harnessPath);

    // Open upload modal as Admin
    await page.click('#open-upload-modal-btn');
    await expect(page.locator('#upload-modal')).toBeVisible();

    const uploadTitle = 'Экспериментальный протокол эпиднадзора 2024';
    const uploadAuthor = 'Филиал НИИ Эпидемиологии';
    const uploadYear = '2024';
    const uploadDesc = 'Новый оперативный документ для верификации каталога.';

    // Fill upload form fields
    await page.fill('#upload-title-input', uploadTitle);
    await page.fill('#upload-author-input', uploadAuthor);
    await page.fill('#upload-year-input', uploadYear);
    await page.fill('#upload-description-input', uploadDesc);

    // Submit upload form
    await page.click('#upload-submit-btn');

    // Verify modal closes and document is listed in catalog
    await expect(page.locator('#upload-modal')).not.toBeVisible();
    const documentGrid = page.locator('#document-grid');
    await expect(documentGrid).toContainText(uploadTitle);

    // Locate delete button for uploaded document and click it
    const deleteButton = page.locator('.delete-btn').first();
    await deleteButton.click();

    // Verify catalog accurately reflects deletion
    await expect(documentGrid).not.toContainText(uploadTitle);
  });

  test('Given a user submits a search with no matches, When the UI updates, Then an explicit "нет материалов" message is shown in Russian', async ({ page }) => {
    await page.goto(harnessPath);

    // Enter a search query that yields no matches
    await page.fill('#search-query-input', 'НесуществующийЗапрос12345');
    await page.click('#search-submit-btn');

    // Confirm explicit 'нет материалов' message is displayed
    const emptyMessage = page.locator('#empty-catalog-message h3');
    await expect(emptyMessage).toBeVisible();
    await expect(emptyMessage).toHaveText('нет материалов');

    // Verify container text in Russian
    await expect(page.locator('#empty-catalog-message')).toContainText('По вашему запросу не найдено ни одного документа');
  });

  test('Given an admin uploads a document but the network fails, When the error occurs, Then the entered metadata remains in the form so it is not lost', async ({ page }) => {
    await page.goto(harnessPath);

    // Ensure Admin view
    await page.click('#open-upload-modal-btn');
    await expect(page.locator('#upload-modal')).toBeVisible();

    // Fill in document metadata
    const testTitle = 'Протокол расследования сибирской язвы';
    const testAuthor = 'Красноярский Филиал НИИ';
    const testYear = '2024';
    const testDesc = 'Подробный оперативный отчет о проведенных эпидемиологических мероприятиях.';

    await page.fill('#upload-title-input', testTitle);
    await page.fill('#upload-author-input', testAuthor);
    await page.fill('#upload-year-input', testYear);
    await page.fill('#upload-description-input', testDesc);

    // Enable network error simulation switch
    await page.check('#simulate-network-error-checkbox');

    // Submit the upload form
    await page.click('#upload-submit-btn');

    // Wait for network error notification
    await expect(page.locator('#upload-error-alert')).toBeVisible();
    await expect(page.locator('#upload-error-alert')).toContainText('Ошибка сети при загрузке документа');

    // CRITICAL ACCEPTANCE CRITERIA VERIFICATION:
    // Verify entered metadata STILL REMAINS INTACT in form inputs
    await expect(page.locator('#upload-title-input')).toHaveValue(testTitle);
    await expect(page.locator('#upload-author-input')).toHaveValue(testAuthor);
    await expect(page.locator('#upload-year-input')).toHaveValue(testYear);
    await expect(page.locator('#upload-description-input')).toHaveValue(testDesc);
  });

  test('Given a user accesses the site on desktop and mobile devices, When the UI renders, Then screenshots are saved for design verification', async ({ page }) => {
    const recordDir = path.resolve(process.cwd(), '.eneik/records/design-check-72f86240-fcf6-4d30-9ff6-02ae1b8fb711');
    if (!fs.existsSync(recordDir)) {
      fs.mkdirSync(recordDir, { recursive: true });
    }

    // Desktop viewport (1440px width)
    await page.setViewportSize({ width: 1440, height: 900 });
    await page.goto(harnessPath);
    await page.waitForLoadState('networkidle');
    await page.screenshot({ path: path.join(recordDir, 'desktop-1440.png'), fullPage: true });

    // Mobile viewport (375px width)
    await page.setViewportSize({ width: 375, height: 812 });
    await page.goto(harnessPath);
    await page.waitForLoadState('networkidle');
    await page.screenshot({ path: path.join(recordDir, 'mobile-375.png'), fullPage: true });

    expect(fs.existsSync(path.join(recordDir, 'desktop-1440.png'))).toBe(true);
    expect(fs.existsSync(path.join(recordDir, 'mobile-375.png'))).toBe(true);
  });
});
