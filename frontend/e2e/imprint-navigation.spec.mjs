import { test, expect } from '@playwright/test';

test.describe('Imprint Navigation and Compliance E2E Tests', () => {

  test('Given the application home page is loaded, When clicking the Imprint link in the footer, Then legal entity details dialog/content is accessible without dead links', async ({ page }) => {
    // Register dialog listener BEFORE triggering navigation or click
    let dialogMessage = '';
    page.on('dialog', async dialog => {
      dialogMessage = dialog.message();
      await dialog.accept();
    });

    // Navigate to root index page
    await page.goto('/');

    // Locate the Imprint link in footer
    const imprintLink = page.locator('#imprint-link-harness');
    await expect(imprintLink).toBeVisible();

    // Verify it is an interactive element (button or valid link) and not a dead anchor (#)
    const href = await imprintLink.getAttribute('href');
    expect(href).not.toBe('#');

    // Click the Imprint footer link
    await imprintLink.click();

    // Confirm dialog was triggered and contains expected Imprint legal entity details
    expect(dialogMessage).toContain('Imprint / Impressum');
    expect(dialogMessage).toContain('Эпидемиологии');
  });

});
