import { test, expect } from '@playwright/test';
import path from 'path';

test.describe('Dossier Search E2E', () => {
    test('Dossier search flow with pagination', async ({ page }) => {
        await page.goto('/dossier-harness.html');
        await page.fill('#search-query-input', 'Иванов');
        await page.click('#search-button');
        await expect(page.locator('#document-list')).toBeVisible();
        await expect(page.locator('#search-query-input')).toHaveValue('Иванов');

        // Verify page indicator
        await expect(page.locator('#page-indicator')).toContainText('Страница 1');

        // Test pagination next button
        await expect(page.locator('#next-page-btn')).toBeEnabled();
        await page.click('#next-page-btn');
        await expect(page.locator('#page-indicator')).toContainText('Страница 2');

        // Test report generation
        await page.click('#generate-report-button');
        await expect(page.locator('#loading-spinner')).toBeVisible();
        await expect(page.locator('#feedback-notice')).toBeVisible();
    });

    test('Dossier Search Design Check Screenshots', async ({ page }) => {
        await page.goto('/dossier-harness.html');
        await page.fill('#search-query-input', 'Иванов');
        await page.click('#search-button');
        await expect(page.locator('#document-list')).toBeVisible();

        const recordDir = path.resolve('../.eneik/records/design-check-71bed14c-3fa3-4c29-84d5-71843fd2f2b1');

        // Desktop screenshot
        await page.setViewportSize({ width: 1440, height: 900 });
        await page.screenshot({ path: path.join(recordDir, 'desktop-1440.png') });

        // Mobile screenshot
        await page.setViewportSize({ width: 375, height: 667 });
        await page.screenshot({ path: path.join(recordDir, 'mobile-375.png') });
    });
});
