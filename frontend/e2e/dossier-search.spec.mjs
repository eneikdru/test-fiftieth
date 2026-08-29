import { test, expect } from '@playwright/test';
import fs from 'fs';
import path from 'path';

test.describe('Dossier Search E2E', () => {
    test('Dossier search flow with period and direction filtering', async ({ page }) => {
        await page.goto('/dossier-harness.html');

        // Fill employee surname filter and click search
        await page.fill('#search-query-input', 'Иванов');
        await page.click('#search-button');
        await expect(page.locator('#document-list')).toBeVisible();
        await expect(page.locator('#search-query-input')).toHaveValue('Иванов');

        // Verify initial filtered documents for 'Иванов'
        const listItems = page.locator('#document-list li');
        await expect(listItems).toHaveCount(3);

        // Filter by Scientific Direction: Virology
        await page.selectOption('#filter-direction', 'virology');
        await expect(page.locator('#document-list li')).toHaveCount(1);
        await expect(page.locator('#document-list')).toContainText('Вирусология');

        // Reset direction and filter by Period: 2024
        await page.selectOption('#filter-direction', 'ALL');
        await page.selectOption('#filter-period', '2024');
        await expect(page.locator('#document-list')).toContainText('2024 г.');
    });

    test('Dossier report export flow and PDF download', async ({ page }) => {
        await page.goto('/dossier-harness.html');
        await page.fill('#search-query-input', 'Иванов');
        await page.click('#search-button');
        await expect(page.locator('#document-list')).toBeVisible();

        // Listen for PDF download event
        const downloadPromise = page.waitForEvent('download');

        // Click export report button
        await page.click('#generate-report-button');
        await expect(page.locator('#loading-spinner')).toBeVisible();

        // Verify PDF download completed
        const download = await downloadPromise;
        expect(download.suggestedFilename()).toContain('.pdf');

        // Verify visible status feedback notice after report generation
        await expect(page.locator('[role="status"]')).toContainText('Сводный PDF-документ успешно сформирован');
    });

    test('Dossier Search Design Check Screenshots', async ({ page }) => {
        const screenshotDir = path.resolve('../.eneik/records/design-check-9b85e119-9c4d-4a55-998f-88a1718bf074');
        if (!fs.existsSync(screenshotDir)) {
            fs.mkdirSync(screenshotDir, { recursive: true });
        }

        await page.goto('/dossier-harness.html');
        await page.fill('#search-query-input', 'Иванов');
        await page.click('#search-button');
        await expect(page.locator('#document-list')).toBeVisible();

        // Desktop screenshot at 1440px viewport width
        await page.setViewportSize({ width: 1440, height: 900 });
        await page.screenshot({ path: path.join(screenshotDir, 'desktop-1440.png'), fullPage: true });

        // Mobile screenshot at 375px viewport width
        await page.setViewportSize({ width: 375, height: 812 });
        await page.screenshot({ path: path.join(screenshotDir, 'mobile-375.png'), fullPage: true });
    });
});
