import { test, expect } from '@playwright/test';
import path from 'path';
import fs from 'fs';

test.describe('Dossier Search E2E', () => {
    test('Dossier search flow and report generation', async ({ page }) => {
        await page.goto('/dossier-harness.html');
        await page.fill('#search-query-input', 'Иванов');
        await page.click('#search-button');

        await expect(page.locator('#document-list')).toBeVisible();
        await expect(page.locator('#search-query-input')).toHaveValue('Иванов');

        // Click generate report button and wait for feedback status message
        await page.click('#generate-report-button');
        await expect(page.locator('div[role="status"].feedback-notice')).toBeVisible();
        await expect(page.locator('div[role="status"].feedback-notice')).toContainText('Сводная справка');
    });

    test('Dossier Search Design Check Screenshots', async ({ page }) => {
        const screenshotDir = path.resolve(process.cwd(), '../.eneik/records/design-check-209e3e2a-8afb-4dcf-8e66-46d407c4727f');
        if (!fs.existsSync(screenshotDir)) {
            fs.mkdirSync(screenshotDir, { recursive: true });
        }

        await page.goto('/dossier-harness.html');
        await page.fill('#search-query-input', 'Иванов');
        await page.click('#search-button');
        await expect(page.locator('#document-list')).toBeVisible();

        // Desktop screenshot
        await page.setViewportSize({ width: 1440, height: 900 });
        await page.screenshot({ path: path.join(screenshotDir, 'desktop-1440.png') });

        // Mobile screenshot
        await page.setViewportSize({ width: 375, height: 667 });
        await page.screenshot({ path: path.join(screenshotDir, 'mobile-375.png') });
    });
});
