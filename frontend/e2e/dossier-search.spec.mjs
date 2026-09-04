import { test, expect } from '@playwright/test';
import path from 'path';

test.describe('Dossier Search E2E', () => {
    test('Dossier search flow', async ({ page }) => {
        await page.goto('/dossier-harness.html');
        await page.fill('#search-query-input', 'Иванов');
        await page.route('**/api/v1/dossier/documents*', route => {
            route.fulfill({
                status: 200,
                contentType: 'application/json',
                body: JSON.stringify([{ id: 1, title: 'Document 1' }])
            });
        });
        await page.click('#search-button');
        await expect(page.locator('#search-query-input')).toHaveValue('Иванов');
        await expect(page.locator('#document-list')).toBeVisible();

        await page.click('#generate-report-button');
        await expect(page.locator('#loading-spinner')).toBeVisible();
    });

    test('Dossier Search Design Check Screenshots', async ({ page }) => {
        await page.goto('/dossier-harness.html');
        await page.fill('#search-query-input', 'Иванов');

        await page.route('**/api/v1/dossier/documents*', route => {
            route.fulfill({
                status: 200,
                contentType: 'application/json',
                body: JSON.stringify([{ id: 1, title: 'Document 1' }])
            });
        });
        await page.click('#search-button');
        await expect(page.locator('#document-list')).toBeVisible();

        // Desktop screenshot
        await page.setViewportSize({ width: 1440, height: 900 });
        await page.screenshot({ path: 'desktop-1440.png' });

        // Mobile screenshot
        await page.setViewportSize({ width: 375, height: 667 });
        await page.screenshot({ path: 'mobile-375.png' });
    });
});
