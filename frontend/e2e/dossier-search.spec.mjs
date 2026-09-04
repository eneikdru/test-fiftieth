import { test, expect } from '@playwright/test';
import fs from 'fs';
import path from 'path';

test.describe('Dossier Search E2E', () => {
    test('Dossier search flow present state', async ({ page }) => {
        await page.goto('/dossier-harness.html');
        await page.fill('#search-query-input', 'Иванов');
        await page.click('#search-button');
        await expect(page.locator('#document-list')).toBeVisible();
        await expect(page.locator('#search-query-input')).toHaveValue('Иванов');

        await page.click('#generate-report-button');
        await expect(page.locator('#loading-spinner')).toBeVisible();
    });

    test('Dossier search flow empty state when server returns zero rows', async ({ page }) => {
        await page.goto('/dossier-harness.html');
        await page.fill('#search-query-input', 'unknown');
        await page.click('#search-button');
        await expect(page.locator('#empty-state')).toBeVisible();
        await expect(page.locator('#empty-state')).toContainText('нет материалов');
        await expect(page.locator('#document-list')).not.toBeVisible();
    });

    test('Dossier search flow error state when server returns error', async ({ page }) => {
        await page.goto('/dossier-harness.html');
        await page.fill('#search-query-input', 'error');
        await page.click('#search-button');
        await expect(page.locator('#error-state')).toBeVisible();
        await expect(page.locator('#document-list')).not.toBeVisible();
    });

    test('Dossier Search Design Check Screenshots', async ({ page }) => {
        await page.goto('/dossier-harness.html');
        await page.fill('#search-query-input', 'Иванов');
        await page.click('#search-button');
        await expect(page.locator('#document-list')).toBeVisible();

        const recordDir = path.resolve('../.eneik/records/design-check-9fb5f997-973e-4bd8-8695-adf9fd6c6aad');
        if (!fs.existsSync(recordDir)) {
            fs.mkdirSync(recordDir, { recursive: true });
        }

        // Desktop screenshot (1440px)
        await page.setViewportSize({ width: 1440, height: 900 });
        await page.screenshot({ path: path.join(recordDir, 'desktop-1440.png') });

        // Mobile screenshot (375px)
        await page.setViewportSize({ width: 375, height: 667 });
        await page.screenshot({ path: path.join(recordDir, 'mobile-375.png') });
    });
});
