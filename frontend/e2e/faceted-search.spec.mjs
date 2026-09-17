import { test, expect } from '@playwright/test';

test.describe('Extended Mind Faceted Search E2E QA Verification', () => {

  test('Given a selected Document Type facet in the UI, When search is executed E2E, Then the returned results must strictly belong to that document type', async ({ page }) => {
    await page.goto('/');

    // Select 'Протокол расследования' as Document Type facet
    await page.selectOption('#search-doctype-input', 'Протокол расследования');
    await page.click('#search-submit-btn');

    // Verify document grid contains results
    const documentCards = page.locator('.document-card');
    await expect(documentCards.first()).toBeVisible();

    const count = await documentCards.count();
    expect(count).toBeGreaterThan(0);

    // Verify all returned document cards strictly belong to 'Протокол расследования'
    for (let i = 0; i < count; i++) {
      const card = documentCards.nth(i);
      await expect(card).toContainText('Протокол расследования');
      await expect(card).not.toContainText('Отчёт эпиднадзора');
      await expect(card).not.toContainText('Методическое руководство');
    }
  });

  test('Given a different Document Type facet selected, When search is executed, Then the returned results match that document type strictly', async ({ page }) => {
    await page.goto('/');

    // Select 'Отчёт эпиднадзора' as Document Type facet
    await page.selectOption('#search-doctype-input', 'Отчёт эпиднадзора');
    await page.click('#search-submit-btn');

    const documentCards = page.locator('.document-card');
    await expect(documentCards.first()).toBeVisible();

    const count = await documentCards.count();
    expect(count).toBeGreaterThan(0);

    for (let i = 0; i < count; i++) {
      const card = documentCards.nth(i);
      await expect(card).toContainText('Отчёт эпиднадзора');
      await expect(card).not.toContainText('Протокол расследования');
    }
  });

  test('Given multiple selected facets (Document Type, Year, and Author), When a search is executed, Then the results represent the intersection of the facets', async ({ page }) => {
    await page.goto('/');

    // Select multiple facets: Document Type, Year, and Author
    await page.selectOption('#search-doctype-input', 'Протокол расследования');
    await page.selectOption('#search-year-input', '2023');
    await page.fill('#search-author-input', 'НИИ Эпидемиологии');
    await page.click('#search-submit-btn');

    // Verify results reflect the exact intersection of selected facets
    const documentCards = page.locator('.document-card');
    await expect(documentCards).toHaveCount(1);

    const targetCard = documentCards.first();
    await expect(targetCard).toContainText('Протокол расследования');
    await expect(targetCard).toContainText('2023');
    await expect(targetCard).toContainText('НИИ Эпидемиологии');
  });

  test('Given multiple selected facets with no matching items in intersection, When search is executed, Then an explicit empty state is displayed', async ({ page }) => {
    await page.goto('/');

    // Select conflicting facets with no intersection (e.g., Document Type = 'Протокол расследования', Year = '2021')
    await page.selectOption('#search-doctype-input', 'Протокол расследования');
    await page.selectOption('#search-year-input', '2021');
    await page.click('#search-submit-btn');

    // Verify empty catalog message appears
    const emptyMessage = page.locator('#empty-catalog-message h3');
    await expect(emptyMessage).toBeVisible();
    await expect(emptyMessage).toHaveText('нет материалов');
  });

});
