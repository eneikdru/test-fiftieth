import { test, expect } from '@playwright/test';

test.describe('SPA QA Verification - Telemetry Removal', () => {

  test('Given the frontend application, When attempting to navigate to the telemetry route or finding the tab, Then the route does not exist and the tab is not rendered in the DOM', async ({ page }) => {
    // 1. Load initial index page
    await page.goto('/');

    // 2. Verify catalog tab is visible by default
    await expect(page.locator('#tab-catalog')).toBeVisible();

    // 3. Confirm telemetry tab (#tab-telemetry) is NOT in the DOM
    await expect(page.locator('#tab-telemetry')).toHaveCount(0);

    // 4. Confirm telemetry panel (#panel-telemetry) is NOT in the DOM
    await expect(page.locator('#panel-telemetry')).toHaveCount(0);

    // 5. Attempt direct navigation to telemetry tab via URL query parameter
    await page.goto('/?tab=telemetry');

    // 6. Confirm telemetry route does not activate telemetry tab or panel
    await expect(page.locator('#tab-telemetry')).toHaveCount(0);
    await expect(page.locator('#panel-telemetry')).toHaveCount(0);
  });

});
