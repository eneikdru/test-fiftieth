import { test, expect } from '@playwright/test';
import { createRequire } from 'module';
const require = createRequire(import.meta.url);

test('Given a UI accessibility scan, When the login page is analyzed, Then zero WCAG 2.1 AA violations are found.', async ({ page }) => {
  await page.goto('/test-harness.html?mode=login');
  await page.waitForTimeout(1000);

  await page.addScriptTag({ path: require.resolve('axe-core/axe.min.js') });

  const accessibilityScanResults = await page.evaluate(async () => {
    return await window.axe.run(document, {
      runOnly: {
        type: 'tag',
        values: ['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa']
      }
    });
  });

  expect(accessibilityScanResults.violations).toEqual([]);
});
