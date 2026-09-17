import { test, expect } from '@playwright/test';

test.describe('Extended Mind Faceted Search E2E QA Verification', () => {

  test('Given a selected Document Type facet in the UI, When a search is executed E2E, Then the returned results must strictly belong to that document type', async ({ page }) => {
    await page.goto('/');

    // Select Document Type facet: "Отчёт эпиднадзора"
    await page.selectOption('#search-doctype-input', 'Отчёт эпиднадзора');

    // Click search submit button
    await page.click('#search-submit-btn');

    // Verify results are visible
    const cards = page.locator('#document-grid .document-card');
    await expect(cards).toHaveCount(1);

    // Verify all returned card titles/badges strictly belong to "Отчёт эпиднадзора"
    const cardBadge = cards.first().locator('.inline-block');
    await expect(cardBadge).toHaveText('Отчёт эпиднадзора');
    const docTitle = cards.first().locator('.doc-title');
    await expect(docTitle).toContainText('Отчет эпиднадзора по гриппу и ОРВИ');
  });

  test('Given multiple selected facets, When a search is executed, Then the results must represent the intersection of the facets', async ({ page }) => {
    await page.goto('/');

    // Select 'Publication Year' facet: "2023"
    await page.selectOption('#search-year-input', '2023');

    // Select 'Document Type' facet: "Протокол расследования"
    await page.selectOption('#search-doctype-input', 'Протокол расследования');

    // Submit search with both facets
    await page.click('#search-submit-btn');

    // Verify returned results represent exact intersection
    const cards = page.locator('#document-grid .document-card');
    await expect(cards).toHaveCount(1);

    const firstCard = cards.first();
    await expect(firstCard.locator('.doc-title')).toContainText('Протокол эпидемиологического расследования вспышки сальмонеллеза');
    await expect(firstCard.locator('.inline-block')).toHaveText('Протокол расследования');

    // Now select a non-matching intersection: Year "2021" and DocType "Протокол расследования"
    await page.selectOption('#search-year-input', '2021');
    await page.click('#search-submit-btn');

    // Verify empty catalog message appears when intersection has no matching documents
    const emptyMsg = page.locator('#empty-catalog-message h3');
    await expect(emptyMsg).toBeVisible();
    await expect(emptyMsg).toHaveText('нет материалов');
  });

});
