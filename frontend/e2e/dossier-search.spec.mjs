import { test, expect } from '@playwright/test';
import path from 'path';

test.describe('Dossier Search E2E', () => {
    test.beforeEach(async ({ page }) => {
        await page.route('**/api/v1/dossier/documents*', async route => {
            const url = new URL(route.request().url());
            const pageNum = url.searchParams.get('page');
            if (pageNum === '1') {
                await route.fulfill({
                    status: 200,
                    contentType: 'application/json',
                    body: JSON.stringify([
                        { id: '4', title: 'Дополнительный документ 1' }
                    ])
                });
            } else {
                await route.fulfill({
                    status: 200,
                    contentType: 'application/json',
                    body: JSON.stringify([
                        { id: '1', title: 'Приказ о назначении №42' },
                        { id: '2', title: 'Выписка из учёного совета от 12.05.2023' },
                        { id: '3', title: 'Отчёт о командировке (Самара)' },
                        { id: 'a', title: 'Doc A' },
                        { id: 'b', title: 'Doc B' },
                        { id: 'c', title: 'Doc C' },
                        { id: 'd', title: 'Doc D' },
                        { id: 'e', title: 'Doc E' },
                        { id: 'f', title: 'Doc F' },
                        { id: 'g', title: 'Doc G' }
                    ])
                });
            }
        });
    });

    test('Dossier search flow', async ({ page }) => {
        await page.goto('/dossier-harness.html');
        await page.fill('#search-query-input', 'Иванов');
        await page.click('#search-button');
        await expect(page.locator('#document-list')).toBeVisible();
        await expect(page.locator('#document-list li')).toHaveCount(10);

        await expect(page.locator('#page-indicator')).toHaveText('Страница 1');
        await expect(page.locator('#prev-page-btn')).toBeDisabled();
        await expect(page.locator('#next-page-btn')).toBeEnabled();

        // Test pagination
        await page.click('#next-page-btn');
        await expect(page.locator('#document-list li')).toHaveCount(1);
        await expect(page.locator('#page-indicator')).toHaveText('Страница 2');
        await expect(page.locator('#prev-page-btn')).toBeEnabled();
        await expect(page.locator('#next-page-btn')).toBeDisabled();
        await expect(page.locator('#search-query-input')).toHaveValue('Иванов');

        await page.click('#generate-report-button');
        await expect(page.locator('#loading-spinner')).toBeVisible();
    });

    test('Dossier Search Design Check Screenshots', async ({ page }) => {
        await page.goto('/dossier-harness.html');
        await page.fill('#search-query-input', 'Иванов');
        await page.click('#search-button');
        await expect(page.locator('#document-list')).toBeVisible();
        await expect(page.locator('#document-list li')).toHaveCount(10);

        // Desktop screenshot
        await page.setViewportSize({ width: 1440, height: 900 });
        await page.screenshot({ path: 'desktop-1440.png' });

        // Mobile screenshot
        await page.setViewportSize({ width: 375, height: 667 });
        await page.screenshot({ path: 'mobile-375.png' });
    });
});
