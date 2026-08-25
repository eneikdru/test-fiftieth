import { test, expect } from '@playwright/test';
import path from 'path';
import fs from 'fs';

test.describe('Dossier Search E2E', () => {
    test.beforeEach(async ({ page }) => {
        // Serve DossierSearch.svelte source file for the harness
        await page.route('**/src/components/DossierSearch.svelte*', async (route) => {
            const filePath = path.resolve('src/components/DossierSearch.svelte');
            const content = fs.readFileSync(filePath, 'utf8');
            await route.fulfill({
                status: 200,
                contentType: 'text/plain',
                body: content
            });
        });

        // Intercept API calls to /api/v1/dossier/documents and /api/v1/dossier/reports
        await page.route('**/api/v1/dossier/documents*', async (route) => {
            await route.fulfill({
                status: 200,
                contentType: 'application/json',
                body: JSON.stringify([
                    { id: 1, title: 'Приказ о назначении №42', docType: 'order', employeeSurname: 'Иванов', employeeId: 'EMP-101' },
                    { id: 2, title: 'Выписка из учёного совета от 12.05.2023', docType: 'extract', employeeSurname: 'Иванов', employeeId: 'EMP-101' },
                    { id: 3, title: 'Отчёт о командировке (Самара)', docType: 'report', employeeSurname: 'Иванов', employeeId: 'EMP-101' }
                ])
            });
        });

        await page.route('**/api/v1/dossier/reports*', async (route) => {
            await new Promise(r => setTimeout(r, 200));
            await route.fulfill({
                status: 201,
                contentType: 'application/json',
                body: JSON.stringify({
                    id: 101,
                    employee_id: 'EMP-101',
                    template_type: 'SUMMARY_STANDARD',
                    status: 'COMPLETED',
                    summary_text: 'Сводная справка по сотруднику EMP-101: 3 документов.',
                    document_count: 3,
                    download_url: '/api/v1/dossier/reports/101/download'
                })
            });
        });
    });

    test('Dossier search flow issues real HTTP requests to /api/v1/dossier endpoints', async ({ page }) => {
        let documentsRequested = false;
        let reportsRequested = false;

        page.on('request', req => {
            if (req.url().includes('/api/v1/dossier/documents')) {
                documentsRequested = true;
            }
            if (req.url().includes('/api/v1/dossier/reports')) {
                reportsRequested = true;
            }
        });

        await page.goto(`file://${path.resolve('dossier-harness.html')}`);

        // Wait for Svelte component to mount
        await expect(page.locator('#search-query-input')).toBeVisible();

        await page.fill('#search-query-input', 'Иванов');
        await page.click('#search-button');

        await expect(page.locator('#document-list')).toBeVisible();
        await expect(page.locator('#search-query-input')).toHaveValue('Иванов');
        expect(documentsRequested).toBe(true);

        await page.click('#generate-report-button');
        await expect(page.locator('#loading-spinner')).toBeVisible();
        await expect(page.locator('#report-result')).toBeVisible();
        expect(reportsRequested).toBe(true);
    });

    test('Dossier Search Design Check Screenshots', async ({ page }) => {
        await page.goto(`file://${path.resolve('dossier-harness.html')}`);

        await expect(page.locator('#search-query-input')).toBeVisible();
        await page.fill('#search-query-input', 'Иванов');
        await page.click('#search-button');
        await expect(page.locator('#document-list')).toBeVisible();

        // Ensure design check output directory exists
        const designCheckDir = path.join(process.cwd(), '../.eneik/records/design-check-209e3e2a-8afb-4dcf-8e66-46d407c4727f');
        if (!fs.existsSync(designCheckDir)) {
            fs.mkdirSync(designCheckDir, { recursive: true });
        }

        // Desktop screenshot
        await page.setViewportSize({ width: 1440, height: 900 });
        await page.screenshot({ path: path.join(designCheckDir, 'desktop-1440.png') });

        // Mobile screenshot
        await page.setViewportSize({ width: 375, height: 667 });
        await page.screenshot({ path: path.join(designCheckDir, 'mobile-375.png') });
    });
});
