import { test, expect } from '@playwright/test';
import path from 'path';
import fs from 'fs';

const harnessPath = '/test-harness.html';

test.describe('Catalog Search and Document Management E2E Tests', () => {

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

    // Verify real backend API endpoint responds with valid document payload
    const response = await request.get('/api/v1/documents/1/download', { timeout: 5000 });
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

  test('Given the search interface, When rendered, Then interactive faceted filters (Document Type, Publication Year) are visible, selectable, and serialize into API requests', async ({ page }) => {
    await page.goto(harnessPath);

    // Verify Document Type and Year faceted filters exist and are visible
    const docTypeSelect = page.locator('#search-doctype-select');
    const yearSelect = page.locator('#search-year-input');

    await expect(docTypeSelect).toBeVisible();
    await expect(yearSelect).toBeVisible();

    // Select Document Type facet and Publication Year facet
    await docTypeSelect.selectOption('Протокол расследования');
    await yearSelect.selectOption('2023');

    // Intercept search API request to verify facet serialization
    const [request] = await Promise.all([
      page.waitForRequest(req => req.url().includes('/api/v1/documents/search')),
      page.click('#search-submit-btn')
    ]);

    const requestUrl = new URL(request.url());
    expect(requestUrl.searchParams.get('docType')).toBe('Протокол расследования');
    expect(requestUrl.searchParams.get('year')).toBe('2023');

    // Verify search results are filtered based on selected facets
    const documentCards = page.locator('#document-grid .document-card');
    await expect(documentCards.first()).toBeVisible();
    await expect(page.locator('#document-grid')).toContainText('Протокол расследования');
  });

  test('Given selected facets, When a search is performed, Then active facets bar displays visual chips/badges and allows individual clearing', async ({ page }) => {
    await page.goto(harnessPath);

    // Select Document Type and Year facets
    await page.selectOption('#search-doctype-select', 'Отчёт эпиднадзора');
    await page.selectOption('#search-year-input', '2023');
    await page.click('#search-submit-btn');

    // Verify active facets bar is visible
    const facetsBar = page.locator('#active-facets-bar');
    await expect(facetsBar).toBeVisible();
    await expect(facetsBar).toContainText('Тип: Отчёт эпиднадзора');
    await expect(facetsBar).toContainText('Год: 2023');

    // Clear Document Type facet individually via chip clear button
    const clearDocTypeBtn = facetsBar.locator('button[aria-label="Удалить фильтр по типу документа"]');
    await clearDocTypeBtn.click();

    // Verify Document Type option is reset and chip is removed
    await expect(facetsBar).not.toContainText('Тип: Отчёт эпиднадзора');
    await expect(facetsBar).toContainText('Год: 2023');
    await expect(page.locator('#search-doctype-select')).toHaveValue('');
  });

  test('Given faceted search filters, When navigating via keyboard, Then all controls are focusable and operable', async ({ page }) => {
    await page.goto(harnessPath);

    // Focus first input in search section
    await page.focus('#search-query-input');
    await expect(page.locator('#search-query-input')).toBeFocused();

    // Tab to Document Type filter dropdown
    await page.keyboard.press('Tab');
    await expect(page.locator('#search-doctype-select')).toBeFocused();

    // Tab to Author input
    await page.keyboard.press('Tab');
    await expect(page.locator('#search-author-input')).toBeFocused();

    // Tab to Year select dropdown
    await page.keyboard.press('Tab');
    await expect(page.locator('#search-year-input')).toBeFocused();

    // Tab to Search Submit button
    await page.keyboard.press('Tab');
    await expect(page.locator('#search-submit-btn')).toBeFocused();
  });

  test('Given a user accesses the site on desktop and mobile devices, When the UI renders, Then screenshots and layout JSON are saved for design verification', async ({ page }) => {
    // Resolve relative to repo root
    const repoRoot = fs.existsSync(path.resolve(process.cwd(), '../pom.xml')) || fs.existsSync(path.resolve(process.cwd(), '../README.md'))
      ? path.resolve(process.cwd(), '..')
      : process.cwd();

    const taskRecordDir = path.resolve(repoRoot, '.eneik/records/design-check-d8eb306e-c724-4f88-9657-a3d1f3979fdd');
    if (!fs.existsSync(taskRecordDir)) {
      fs.mkdirSync(taskRecordDir, { recursive: true });
    }

    const recordDirLegacy = path.resolve(repoRoot, '.eneik/records/design-check-72f86240-fcf6-4d30-9ff6-02ae1b8fb711');
    if (!fs.existsSync(recordDirLegacy)) {
      fs.mkdirSync(recordDirLegacy, { recursive: true });
    }

    // Desktop viewport (1440px width)
    await page.setViewportSize({ width: 1440, height: 900 });
    await page.goto(harnessPath);
    await page.waitForLoadState('networkidle');
    await page.screenshot({ path: path.join(taskRecordDir, 'desktop-1440.png'), fullPage: true });
    await page.screenshot({ path: path.join(recordDirLegacy, 'desktop-1440.png'), fullPage: true });

    // Mobile viewport (375px width)
    await page.setViewportSize({ width: 375, height: 812 });
    await page.goto(harnessPath);
    await page.waitForLoadState('networkidle');
    await page.screenshot({ path: path.join(taskRecordDir, 'mobile-375.png'), fullPage: true });
    await page.screenshot({ path: path.join(recordDirLegacy, 'mobile-375.png'), fullPage: true });

    // Extract bounding boxes for key layout elements for layout-check.json
    const layoutBoundingBoxes = await page.evaluate(() => {
      const elementsToMeasure = [
        { id: 'search-query-input' },
        { id: 'search-doctype-select' },
        { id: 'search-author-input' },
        { id: 'search-year-input' },
        { id: 'search-submit-btn' },
        { id: 'document-grid' }
      ];

      return elementsToMeasure.map(item => {
        const el = document.getElementById(item.id);
        if (!el) return { id: item.id, left: 0, top: 0, width: 0, height: 0 };
        const rect = el.getBoundingClientRect();
        return {
          id: item.id,
          left: Math.round(rect.left),
          top: Math.round(rect.top),
          width: Math.round(rect.width),
          height: Math.round(rect.height)
        };
      });
    });

    fs.writeFileSync(
      path.join(taskRecordDir, 'layout-check.json'),
      JSON.stringify(layoutBoundingBoxes, null, 2),
      'utf-8'
    );

    expect(fs.existsSync(path.join(taskRecordDir, 'desktop-1440.png'))).toBe(true);
    expect(fs.existsSync(path.join(taskRecordDir, 'mobile-375.png'))).toBe(true);
    expect(fs.existsSync(path.join(taskRecordDir, 'layout-check.json'))).toBe(true);
  });
});
