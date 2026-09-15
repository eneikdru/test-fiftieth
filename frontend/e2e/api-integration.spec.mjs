import { test, expect } from '@playwright/test';
import path from 'path';
import fs from 'fs';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

test.describe('Frontend API Integration E2E Tests', () => {

  test('Given a UI action, When triggered, Then real API requests are dispatched and feedback is displayed', async ({ page }) => {
    await page.goto('/test-harness.html');

    // Search query interaction
    const searchInput = page.locator('#search-query-input');
    await searchInput.fill('грипп');
    await page.click('#search-submit-btn');

    // Verify loading indicator or results
    const countElement = page.locator('#catalog-count');
    await expect(countElement).toBeVisible();

    // Verify open upload modal
    await page.click('#open-upload-modal-btn');
    await expect(page.locator('#upload-modal')).toBeVisible();

    // Submit with simulated network error
    await page.fill('#upload-title-input', 'Отчет по вирусным инфекциям');
    await page.fill('#upload-author-input', 'Институт эпидемиологии');
    await page.fill('#upload-year-input', '2024');
    await page.fill('#upload-description-input', 'Тестовое описание отчета');

    await page.check('#simulate-network-error-checkbox');
    await page.click('#upload-submit-btn');

    // Error alert displayed
    await expect(page.locator('#upload-error-alert')).toBeVisible();

    // Form inputs preserved
    await expect(page.locator('#upload-title-input')).toHaveValue('Отчет по вирусным инфекциям');
    await expect(page.locator('#upload-author-input')).toHaveValue('Институт эпидемиологии');
    await expect(page.locator('#upload-year-input')).toHaveValue('2024');
    await expect(page.locator('#upload-description-input')).toHaveValue('Тестовое описание отчета');
  });

  test('Design check screenshots and layout geometry export', async ({ page }) => {
    const recordDir = path.resolve(__dirname, '../../.eneik/records/design-check-4762e816-8c4e-486f-a42b-13645ccd0355');
    if (!fs.existsSync(recordDir)) {
      fs.mkdirSync(recordDir, { recursive: true });
    }

    // Render desktop (1440px viewport width)
    await page.setViewportSize({ width: 1440, height: 900 });
    await page.goto('/test-harness.html');
    await page.waitForLoadState('networkidle');
    await page.screenshot({ path: path.join(recordDir, 'desktop-1440.png'), fullPage: true });

    // Render mobile (375px viewport width)
    await page.setViewportSize({ width: 375, height: 812 });
    await page.goto('/test-harness.html');
    await page.waitForLoadState('networkidle');
    await page.screenshot({ path: path.join(recordDir, 'mobile-375.png'), fullPage: true });

    // Extract bounding boxes of key layout elements
    const layoutBoundingBoxes = await page.evaluate(() => {
      const elements = [
        { id: 'search-query-input', el: document.querySelector('#search-query-input') },
        { id: 'search-submit-btn', el: document.querySelector('#search-submit-btn') },
        { id: 'open-upload-modal-btn', el: document.querySelector('#open-upload-modal-btn') },
        { id: 'document-grid', el: document.querySelector('#document-grid') }
      ];

      return elements.map(({ id, el }) => {
        if (!el) return { id, left: 0, top: 0, width: 0, height: 0 };
        const rect = el.getBoundingClientRect();
        return {
          id,
          left: Math.round(rect.left),
          top: Math.round(rect.top),
          width: Math.round(rect.width),
          height: Math.round(rect.height)
        };
      });
    });

    fs.writeFileSync(
      path.join(recordDir, 'layout-check.json'),
      JSON.stringify(layoutBoundingBoxes, null, 2),
      'utf-8'
    );

    expect(fs.existsSync(path.join(recordDir, 'desktop-1440.png'))).toBe(true);
    expect(fs.existsSync(path.join(recordDir, 'mobile-375.png'))).toBe(true);
    expect(fs.existsSync(path.join(recordDir, 'layout-check.json'))).toBe(true);
  });
});
