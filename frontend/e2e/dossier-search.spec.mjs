import { test, expect } from '@playwright/test';
import fs from 'fs';
import path from 'path';

test.describe('Dossier Search and Signature E2E', () => {
    test('Dossier search and signature flow', async ({ page }) => {
        await page.goto('/dossier-harness.html');
        await page.fill('#search-query-input', 'Иванов');
        await page.click('#search-button');
        await expect(page.locator('#document-list')).toBeVisible();
        await expect(page.locator('#search-query-input')).toHaveValue('Иванов');

        await page.click('#generate-report-button');
        await expect(page.locator('#report-signature-container')).toBeVisible();
        await expect(page.locator('#report-status-badge')).toHaveText('Статус: Черновик');

        await page.fill('#signature-input', 'Подписано: Эпидемиолог Иванов И.И.');
        await page.click('#sign-report-button');

        await expect(page.locator('#report-status-badge')).toHaveText('Статус: Подписан');
        await expect(page.locator('#signature-success-notice')).toContainText('Подписано: Эпидемиолог Иванов И.И.');
    });

    test('Dossier Search Design Check Screenshots and Layout Check', async ({ page }) => {
        const recordDir = path.join(process.cwd(), '..', '.eneik', 'records', 'design-check-24d0ef35-452e-4c5c-907a-630d12975761');
        fs.mkdirSync(recordDir, { recursive: true });

        await page.goto('/dossier-harness.html');
        await page.fill('#search-query-input', 'Иванов');
        await page.click('#search-button');
        await expect(page.locator('#document-list')).toBeVisible();
        await page.click('#generate-report-button');
        await expect(page.locator('#report-signature-container')).toBeVisible();

        // Desktop screenshot
        await page.setViewportSize({ width: 1440, height: 900 });
        await page.screenshot({ path: path.join(recordDir, 'desktop-1440.png') });

        // Mobile screenshot
        await page.setViewportSize({ width: 375, height: 667 });
        await page.screenshot({ path: path.join(recordDir, 'mobile-375.png') });

        // Layout check bounding boxes
        const layoutBoxes = await page.evaluate(() => {
            const elements = [
                { id: 'search-query-input', selector: '#search-query-input' },
                { id: 'search-button', selector: '#search-button' },
                { id: 'results-container', selector: '#results-container' },
                { id: 'generate-report-button', selector: '#generate-report-button' },
                { id: 'report-signature-container', selector: '#report-signature-container' },
                { id: 'signature-input', selector: '#signature-input' },
                { id: 'sign-report-button', selector: '#sign-report-button' }
            ];

            return elements.map(item => {
                const el = document.querySelector(item.selector);
                if (!el) return { id: item.id, left: 0, top: 0, width: 0, height: 0 };
                const rect = el.getBoundingClientRect();
                return {
                    id: item.id,
                    left: Math.round(rect.left),
                    top: Math.round(rect.top),
                    width: Math.round(rect.width),
                    height: Math.round(rect.height)
                };
            });
        });

        fs.writeFileSync(path.join(recordDir, 'layout-check.json'), JSON.stringify(layoutBoxes, null, 2));
    });
});
