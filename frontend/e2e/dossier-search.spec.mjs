import { test, expect } from '@playwright/test';
import path from 'path';
import fs from 'fs';

test.describe('Dossier Search E2E', () => {
    test('Dossier search flow issues HTTP requests to /api/v1/dossier endpoints', async ({ page }) => {
        // Track API requests
        const requestedUrls = [];
        page.on('request', req => requestedUrls.push(req.url()));

        await page.goto('/dossier-harness.html');
        await page.fill('#search-query-input', 'Иванов');
        await page.click('#search-button');

        // Verify document list is visible and contains fetched data
        await expect(page.locator('#document-list')).toBeVisible();
        await expect(page.locator('#search-query-input')).toHaveValue('Иванов');
        await expect(page.locator('#document-list')).toContainText('Приказ о назначении №42');

        // Verify request to dossier documents endpoint was made
        const docsApiCalled = requestedUrls.some(url => url.includes('/api/v1/dossier/documents'));
        expect(docsApiCalled).toBe(true);

        // Click generate report
        await page.click('#generate-report-button');
        await expect(page.locator('.feedback-notice')).toBeVisible();
        await expect(page.locator('.feedback-notice')).toContainText('Итоговая справка успешно сформирована');

        // Verify request to dossier reports endpoint was made
        const reportsApiCalled = requestedUrls.some(url => url.includes('/api/v1/dossier/reports'));
        expect(reportsApiCalled).toBe(true);
    });

    test('Dossier Search Design Check Screenshots', async ({ page }) => {
        const recordDir = path.resolve(process.cwd(), '../.eneik/records/design-check-209e3e2a-8afb-4dcf-8e66-46d407c4727f');
        if (!fs.existsSync(recordDir)) {
            fs.mkdirSync(recordDir, { recursive: true });
        }

        await page.goto('/dossier-harness.html');
        await page.fill('#search-query-input', 'Иванов');
        await page.click('#search-button');
        await expect(page.locator('#document-list')).toBeVisible();

        // Desktop screenshot (1440px)
        await page.setViewportSize({ width: 1440, height: 900 });
        await page.screenshot({ path: path.join(recordDir, 'desktop-1440.png'), fullPage: true });

        // Mobile screenshot (375px)
        await page.setViewportSize({ width: 375, height: 667 });
        await page.screenshot({ path: path.join(recordDir, 'mobile-375.png'), fullPage: true });

        expect(fs.existsSync(path.join(recordDir, 'desktop-1440.png'))).toBe(true);
        expect(fs.existsSync(path.join(recordDir, 'mobile-375.png'))).toBe(true);
    });
});
