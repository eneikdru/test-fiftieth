import { test, expect } from '@playwright/test';
import path from 'path';
import fs from 'fs';

test.describe('Dossier Search E2E', () => {
    test('Dossier search flow', async ({ page }) => {
        await page.goto('/dossier-harness.html');
        await page.fill('#search-query-input', 'Иванов');
        await page.click('#search-button');
        await expect(page.locator('#document-list')).toBeVisible();
        await expect(page.locator('#search-query-input')).toHaveValue('Иванов');

        // Verify pagination controls
        await expect(page.locator('#page-indicator')).toContainText('Страница 1');
        await expect(page.locator('#next-page-button')).toBeEnabled();

        // Go to next page
        await page.click('#next-page-button');
        await expect(page.locator('#page-indicator')).toContainText('Страница 2');

        await page.click('#generate-report-button');
        await expect(page.locator('#loading-spinner')).toBeVisible();
    });

    test('Dossier Search Design Check Screenshots', async ({ page }) => {
        await page.goto('/dossier-harness.html');
        await page.fill('#search-query-input', 'Иванов');
        await page.click('#search-button');
        await expect(page.locator('#document-list')).toBeVisible();

        // Ensure screenshot directory exists
        const dir = '../.eneik/records/design-check-76dbb85d-d483-49b9-891d-38b8515537a3';
        if (!fs.existsSync(dir)) {
            fs.mkdirSync(dir, { recursive: true });
        }

        // Desktop screenshot
        await page.setViewportSize({ width: 1440, height: 900 });
        await page.screenshot({ path: `${dir}/desktop-1440.png`, fullPage: true });

        // Mobile screenshot
        await page.setViewportSize({ width: 375, height: 667 });
        await page.screenshot({ path: `${dir}/mobile-375.png`, fullPage: true });
    });
});
