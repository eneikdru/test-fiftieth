import { test, expect } from '@playwright/test';
import path from 'path';
import fs from 'fs';

const harnessPath = '/test-harness.html';

test.describe('Catalog Search and Document Management E2E Tests', () => {

  // BLOCKER: The E2E test currently fails because the real backend is offline or not deployed during the test run.
  // We removed the try/catch gating so the test correctly enforces the API call and download assertions.
  // The backend needs to be deployed and reachable at the configured baseURL for this test to pass.
  test('Given a fresh deployment pre-populated with sample "Epidemiological Protocol" documents, When the E2E test downloads a document, Then it correctly hits the system API using a configured Playwright baseURL', async ({ page, request, baseURL }) => {
    await page.goto('/');

    // Search for known sample document (Epidemiological Protocol / сальмонеллеза)
    await page.fill('#search-query-input', 'сальмонеллеза');
    await page.click('#search-submit-btn');

    // Confirm matching document is found and displayed
    const docTitle = page.locator('.doc-title').first();
    await expect(docTitle).toBeVisible();
    await expect(docTitle).toContainText('сальмонеллеза');

    // Trigger download and verify file retrieval from real system API endpoint using Playwright baseURL / relative endpoint
    expect(baseURL).toBeTruthy();

    const response = await request.get('/api/v1/documents/1/download', { timeout: 2000 });
    expect(response.status()).toBe(200);
    const content = await response.text();
    expect(content).toContain('Содержимое документа');

    const downloadPromise = page.waitForEvent('download', { timeout: 5000 });
    await page.locator('.download-btn').first().click();
    const download = await downloadPromise;

    expect(download.suggestedFilename()).toBe('salmonella_outbreak.pdf');
    const readStream = await download.createReadStream();
    expect(readStream).not.toBeNull();
  });

  test('Given an admin user session, When the test executes, Then it uploads and deletes a document and strictly verifies that the catalog reflects these changes', async ({ page }) => {
    await page.goto(harnessPath);

    // Open upload modal as Admin
    await page.click('#open-upload-modal-btn');
    await expect(page.locator('#upload-modal')).toBeVisible();

    const uploadTitle = 'Эпидемиологический протокол 2024';
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

    // Verify modal closes and catalog document grid strictly reflects uploaded document
    await expect(page.locator('#upload-modal')).not.toBeVisible();
    const documentGrid = page.locator('#document-grid');
    await expect(documentGrid).toContainText(uploadTitle);
    const uploadedCard = documentGrid.locator('.document-card', { hasText: uploadTitle });
    await expect(uploadedCard).toBeVisible();

    // Locate delete button for uploaded document and click it
    const deleteButton = uploadedCard.locator('.delete-btn');
    await deleteButton.click();

    // Verify catalog document grid strictly reflects deletion without errors
    await expect(documentGrid).not.toContainText(uploadTitle);
    await expect(uploadedCard).toHaveCount(0);
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

  test('Given a found document in the catalog search results, When the user clicks the open/view action, Then the document content opens in an inline viewer', async ({ page }) => {
    await page.goto(harnessPath);

    // Click open/view button on first document
    await page.click('.view-btn');

    // Confirm inline viewer modal opens
    const modal = page.locator('#doc-viewer-modal');
    await expect(modal).toBeVisible();

    // Verify modal title
    const modalTitle = page.locator('#doc-viewer-title');
    await expect(modalTitle).toBeVisible();

    // Close viewer modal
    await page.click('#viewer-close-btn');
    await expect(modal).not.toBeVisible();
  });

  test('Given an unsupported document format or view error, When the viewer attempts to load, Then a clear error message and fallback download link are presented', async ({ page }) => {
    await page.goto(harnessPath);

    // Open upload modal as Admin and upload a .docx document
    await page.click('#open-upload-modal-btn');
    await page.fill('#upload-title-input', 'Методические рекомендации docx');
    await page.fill('#upload-author-input', 'Минздрав');
    await page.fill('#upload-year-input', '2024');
    await page.fill('#upload-description-input', 'Руководство docx');
    await page.selectOption('#upload-doctype-select', 'Методическое руководство');

    // Select file with .docx extension
    await page.setInputFiles('#upload-file-input', {
      name: 'recommendations.docx',
      mimeType: 'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
      buffer: Buffer.from('mock docx content')
    });

    // Submit form
    await page.click('#upload-submit-btn');
    await expect(page.locator('#upload-modal')).not.toBeVisible();

    // Click open button on newly created docx document card
    const uploadedCard = page.locator('.document-card', { hasText: 'Методические рекомендации docx' });
    await uploadedCard.locator('.view-btn').click();

    // Confirm error notification container is displayed
    const errorContainer = page.locator('#doc-viewer-error');
    await expect(errorContainer).toBeVisible();
    await expect(errorContainer).toContainText('Предпросмотр недоступен');
    await expect(errorContainer).toContainText('Формат файла не поддерживается');

    // Fallback download link/button is visible
    const fallbackBtn = page.locator('#viewer-fallback-download-btn');
    await expect(fallbackBtn).toBeVisible();
  });

  test('Given a user accesses the site on desktop and mobile devices, When the UI renders, Then screenshots are saved for design verification', async ({ page }) => {
    const recordDir = path.resolve('/app/.eneik/records/design-check-49bf6c01-253a-48a9-8073-9ab9cf4c0497');
    if (!fs.existsSync(recordDir)) {
      fs.mkdirSync(recordDir, { recursive: true });
    }

    // Desktop viewport (1440px width)
    await page.setViewportSize({ width: 1440, height: 900 });
    await page.goto(harnessPath);
    await page.waitForLoadState('domcontentloaded');
    await page.click('.view-btn');
    await page.waitForSelector('#doc-viewer-modal');
    await page.screenshot({ path: path.join(recordDir, 'desktop-1440.png'), fullPage: true });

    // Mobile viewport (375px width)
    await page.setViewportSize({ width: 375, height: 812 });
    await page.goto(harnessPath);
    await page.waitForLoadState('domcontentloaded');
    await page.click('.view-btn');
    await page.waitForSelector('#doc-viewer-modal');
    await page.screenshot({ path: path.join(recordDir, 'mobile-375.png'), fullPage: true });

    expect(fs.existsSync(path.join(recordDir, 'desktop-1440.png'))).toBe(true);
    expect(fs.existsSync(path.join(recordDir, 'mobile-375.png'))).toBe(true);
  });
});
