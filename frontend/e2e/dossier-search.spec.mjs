import { test, expect } from '@playwright/test';
import path from 'path';
import fs from 'fs';

test.describe('Dossier Search E2E', () => {
    test('Dossier search flow', async ({ page }) => {
        await page.goto('/dossier-harness.html');
        await page.fill('#search-query-input', 'Иванов');

        await page.selectOption('#filter-period', 'last_month');
        await page.selectOption('#filter-direction', 'virology');

        await page.click('#search-button');
        await expect(page.locator('#document-list')).toBeVisible();
        await expect(page.locator('#search-query-input')).toHaveValue('Иванов');
        await expect(page.locator('#document-list')).toContainText('Вирусология');
        await expect(page.locator('#document-list')).toContainText('Конкретный месяц');

        const downloadPromise = page.waitForEvent('download');
        await page.click('#generate-report-button');
        await expect(page.locator('#loading-spinner')).toBeVisible();

        const download = await downloadPromise;
        expect(download.suggestedFilename()).toBe('dossier-report.pdf');

        await expect(page.locator('#feedback-notice')).toBeVisible();
        await expect(page.locator('#feedback-notice')).toContainText('✓ Итоговая справка успешно сформирована.');
    });

    test('Dossier Search Design Check Screenshots', async ({ page }) => {
        await page.goto('/dossier-harness.html');
        await page.fill('#search-query-input', 'Иванов');

        await page.selectOption('#filter-period', 'last_month');
        await page.selectOption('#filter-direction', 'virology');

        await page.click('#search-button');
        await expect(page.locator('#document-list')).toBeVisible();

        // Ensure the directory exists before taking screenshots
        const recordsDir = path.resolve('../.eneik/records/design-check-9b85e119-9c4d-4a55-998f-88a1718bf074');
        if (!fs.existsSync(recordsDir)) {
             fs.mkdirSync(recordsDir, { recursive: true });
        }

        // Desktop screenshot
        await page.setViewportSize({ width: 1440, height: 900 });
        await page.screenshot({ path: path.join(recordsDir, 'desktop-1440.png'), fullPage: true });

        // Mobile screenshot
        await page.setViewportSize({ width: 375, height: 667 });
        await page.screenshot({ path: path.join(recordsDir, 'mobile-375.png'), fullPage: true });
    });
});
