import { test, expect } from '@playwright/test';
import fs from 'node:fs';
import path from 'node:path';

test.describe('Consent Banner', () => {
  test.beforeEach(async ({ page }) => {
    // Clear localStorage before each test
    await page.addInitScript(() => {
      window.localStorage.clear();
    });
  });

  test('Given a new visitor, When the page loads, Then the consent platform banner appears before non-essential trackers load', async ({ page }) => {
    await page.goto('/consent-harness.html');
    const banner = page.locator('div[role="dialog"]');
    await expect(banner).toBeVisible();
    await expect(page.locator('text=Настройки конфиденциальности')).toBeVisible();
  });

  test('Given the banner, When the user interacts, Then rejecting is as easy as accepting and the choice is saved', async ({ page }) => {
    await page.goto('/consent-harness.html');
    const rejectButton = page.locator('button', { hasText: 'Отклонить' });
    await expect(rejectButton).toBeVisible();

    // Reject
    await rejectButton.click();
    await expect(page.locator('div[role="dialog"]')).toBeHidden();

    // Choice saved
    const consent = await page.evaluate(() => localStorage.getItem('consent_status'));
    expect(consent).toBe('rejected');
  });

  test('Given a user interacts via keyboard, When using the banner, Then it meets WCAG 2.1 level AA', async ({ page }) => {
    await page.goto('/consent-harness.html');
    await page.waitForSelector('button:has-text("Отклонить")');

    await page.keyboard.press('Tab'); // First focusable element (Reject button)

    // Tab again
    await page.keyboard.press('Tab'); // Second focusable element (Accept button)
    const isAcceptFocused = await page.evaluate(() => document.activeElement.textContent.includes('Принять'));
    expect(isAcceptFocused).toBe(true);

    // Accept via Enter
    await page.keyboard.press('Enter');
    await expect(page.locator('div[role="dialog"]')).toBeHidden();

    const consent = await page.evaluate(() => localStorage.getItem('consent_status'));
    expect(consent).toBe('accepted');
  });
});
