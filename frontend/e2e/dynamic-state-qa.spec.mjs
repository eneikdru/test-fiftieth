import { test, expect } from '@playwright/test';

test.describe('UI Dynamic State & Network Mapping QA', () => {

  test('Given E2E tests, When triggering the Imprint, Then no unexpected browser alerts are intercepted', async ({ page }) => {
    let alertInterrupted = false;
    let alertMessage = '';

    page.on('dialog', dialog => {
      alertInterrupted = true;
      alertMessage = dialog.message();
      dialog.dismiss();
    });

    await page.goto('/');

    // Locate and click the Imprint link
    const imprintLink = page.locator('#imprint-link, #imprint-link-harness').first();
    await expect(imprintLink).toBeVisible();
    await imprintLink.click();

    // Verify no native browser alert dialog was intercepted
    expect(alertInterrupted).toBe(false);

    // If ImprintModal is rendered, verify it is visible in DOM
    const imprintModal = page.locator('#imprint-modal');
    if (await imprintModal.count() > 0) {
      await expect(imprintModal).toBeVisible();
      await expect(imprintModal).toContainText('Выходные данные');
    }
  });

  test('Given simulated network latency, When generating a dossier, Then the UI loading state duration exactly matches the network delay', async ({ page }) => {
    const delayMs = 700;

    // Intercept dossier report generation route and simulate exact network latency
    await page.route('**/api/v1/dossier/reports', async route => {
      await new Promise(resolve => setTimeout(resolve, delayMs));
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          id: 101,
          employee_id: 'EMP-101',
          report_type: 'SUMMARY_STANDARD',
          status: 'COMPLETED',
          summary: 'Сводная справка по сотруднику',
          download_url: '/api/v1/dossier/reports/101/download'
        })
      });
    });

    await page.goto('/#dossier');

    // Navigate to Dossier tab
    const dossierTab = page.locator('#tab-dossier');
    if (await dossierTab.isVisible()) {
      await dossierTab.click();
    }

    // Perform search to reveal dossier results and report generation button
    const searchInput = page.locator('#search-query-input, #panel-dossier #search-query-input').first();
    const searchBtn = page.locator('#search-button, #panel-dossier #search-button').first();

    await searchInput.fill('Иванов');
    await searchBtn.click();

    const generateBtn = page.locator('#generate-report-button').first();
    await expect(generateBtn).toBeVisible();

    const startTime = Date.now();
    await generateBtn.click();

    // Verify loading state appears immediately
    const spinner = page.locator('#loading-spinner');
    await expect(spinner).toBeVisible();

    // Wait for loading spinner to disappear / report notice to appear
    await expect(spinner).not.toBeVisible({ timeout: 5000 });
    const elapsed = Date.now() - startTime;

    // Loading duration must closely match the simulated network latency (lower AND upper bounds to detect artificial timeouts)
    expect(elapsed).toBeGreaterThanOrEqual(delayMs - 150);
    expect(elapsed).toBeLessThanOrEqual(delayMs + 1000);
  });

});
