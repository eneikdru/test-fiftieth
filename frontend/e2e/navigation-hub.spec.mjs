import { test, expect } from '@playwright/test';

test.describe('SPA Navigation Hub and Tab Routing E2E Tests', () => {

  test('Given the main index page is loaded, When clicking #tab-dossier and #tab-privacy tabs, Then modules display dynamically without page reload', async ({ page }) => {
    // Navigate to root index page
    await page.goto('/');

    // Check catalog tab is active by default
    await expect(page.locator('#tab-catalog')).toBeVisible();
    await expect(page.locator('#panel-catalog')).toBeVisible();

    // Track navigation / reload events to ensure page does NOT reload during tab clicks
    let pageReloaded = false;
    page.on('framenavigated', (frame) => {
      if (frame === page.mainFrame()) {
        pageReloaded = true;
      }
    });

    // Reset pageReloaded flag after initial page load
    pageReloaded = false;

    // Click Dossier tab (#tab-dossier)
    await page.click('#tab-dossier');

    // Confirm Dossier panel is rendered
    await expect(page.locator('#panel-dossier')).toBeVisible();
    await expect(page.locator('#panel-catalog')).not.toBeVisible();
    await expect(page.locator('#panel-dossier')).toContainText('Аналитика досье сотрудников');

    // Test Dossier Search functionality
    await page.fill('#panel-dossier #search-query-input', 'Петров');
    await page.click('#panel-dossier #search-button');
    await expect(page.locator('#document-list')).toContainText('Приказ о назначении №42');

    // Click Privacy tab (#tab-privacy)
    await page.click('#tab-privacy');

    // Confirm Privacy panel is rendered
    await expect(page.locator('#panel-privacy')).toBeVisible();
    await expect(page.locator('#panel-dossier')).not.toBeVisible();
    await expect(page.locator('#panel-privacy')).toContainText('Экспорт персональных данных');

    // Click Foci tab (#tab-foci)
    await page.click('#tab-foci');
    await expect(page.locator('#panel-foci')).toBeVisible();
    await expect(page.locator('#panel-foci')).toContainText('Категоризация очагов');

    // Click back to Catalog tab (#tab-catalog)
    await page.click('#tab-catalog');
    await expect(page.locator('#panel-catalog')).toBeVisible();

    // Strictly verify no page reload occurred during tab switching
    expect(pageReloaded).toBe(false);
  });

  test('Given the UI navigation header is rendered, When evaluated for Fitts Law and Miller Law, Then tabs have min 44x44px targets and total nav count <= 9', async ({ page }) => {
    await page.goto('/');

    const tabs = page.locator('nav[aria-label="Навигация по модулям"] button');
    const tabCount = await tabs.count();

    // Miller's Law: max 9 blocks in navigation chunk
    expect(tabCount).toBeLessThanOrEqual(9);
    expect(tabCount).toBe(4);

    // Fitts's Law: check interactive zone size >= 44x44px
    for (let i = 0; i < tabCount; i++) {
      const tab = tabs.nth(i);
      const box = await tab.boundingBox();
      expect(box).not.toBeNull();
      expect(box.width).toBeGreaterThanOrEqual(44);
      expect(box.height).toBeGreaterThanOrEqual(44);
    }
  });

});
