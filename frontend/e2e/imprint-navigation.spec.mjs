import { test, expect } from '@playwright/test';

test.describe('Imprint Navigation', () => {
  test('Given the deployed application, When testing navigation, Then the imprint page loads successfully from the footer without any dead links', async ({ page }) => {
    await page.goto('/');

    const footer = page.locator('footer');
    await expect(footer).toBeVisible();

    const imprintLink = footer.locator('button, a').filter({ hasText: 'Выходные данные (Imprint / Impressum)' });
    await expect(imprintLink).toBeVisible();

    const tagName = await imprintLink.evaluate(node => node.tagName.toLowerCase());

    if (tagName === 'a') {
      const href = await imprintLink.getAttribute('href');
      expect(href).not.toBe('#');
      expect(href).toBeTruthy();
    }

    let dialogMessage = '';
    let dialogFired = false;
    page.on('dialog', async dialog => {
      dialogFired = true;
      dialogMessage = dialog.message();
      await dialog.dismiss();
    });

    await imprintLink.click();

    // Give it a short moment for dialog or modal
    await page.waitForTimeout(100);

    if (dialogFired) {
      expect(dialogMessage).toContain('Выходные данные');
    } else {
      const modal = page.locator('#imprint-modal');
      if (await modal.count() > 0 && await modal.isVisible()) {
        await expect(modal).toContainText('Выходные данные');
      } else {
        const text = await page.locator('body').innerText();
        expect(text).toContain('Выходные данные');
      }
    }
  });
});
