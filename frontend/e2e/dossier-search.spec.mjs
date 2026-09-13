import { test, expect } from '@playwright/test';
import path from 'path';

test.describe('Dossier Search E2E', () => {
    test('Dossier search flow', async ({ page }) => {
        await page.goto('/dossier-harness.html');
        await page.fill('#search-query-input', 'Иванов');
        await page.click('#search-button');
        await expect(page.locator('#document-list')).toBeVisible();
        await expect(page.locator('#search-query-input')).toHaveValue('Иванов');

        await page.click('#generate-report-button');
        await expect(page.locator('#loading-spinner')).toBeVisible();
        await expect(page.locator('text=✓ Итоговая справка успешно сформирована.')).toBeVisible();

        await page.fill('#signature-input', 'Test Signature');
        await page.click('#sign-report-button');
        await expect(page.locator('text=✓ Справка успешно подписана.')).toBeVisible();
    });

    test('Dossier Search Design Check Screenshots', async ({ page }) => {
        await page.goto('/dossier-harness.html');
        await page.fill('#search-query-input', 'Иванов');
        await page.click('#search-button');
        await expect(page.locator('#document-list')).toBeVisible();

        await page.click('#generate-report-button');
        await expect(page.locator('text=✓ Итоговая справка успешно сформирована.')).toBeVisible();
        await page.fill('#signature-input', 'Test Signature');

        // Desktop screenshot
        await page.setViewportSize({ width: 1440, height: 900 });
        await page.screenshot({ path: 'desktop-1440.png' });

        // Mobile screenshot
        await page.setViewportSize({ width: 375, height: 667 });
        await page.screenshot({ path: 'mobile-375.png' });
    });
});
