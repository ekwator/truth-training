/**
 * E2E test for dark theme consistency.
 * Verifies dark theme applied consistently across the application.
 */

import { test, expect } from '@playwright/test';

test.describe('Dark Theme Consistency E2E Tests', () => {
  test.beforeEach(async ({ page }) => {
    // Set dark theme preference in localStorage
    await page.goto('/');
    await page.evaluate(() => {
      localStorage.setItem('truth-theme', 'dark');
    });
  });

  test('should apply dark theme to Dashboard screen', async ({ page }) => {
    await page.goto('/');
    await expect(page.locator('html')).toHaveClass(/dark/);
    await expect(page.locator('html')).not.toHaveClass(/light/);
  });

  test('should maintain dark theme when navigating to NewEvent screen', async ({ page }) => {
    await page.goto('/');
    await page.click('text=New Event');
    await expect(page.locator('html')).toHaveClass(/dark/);
  });

  test('should maintain dark theme when navigating to Events screen', async ({ page }) => {
    await page.goto('/');
    await page.click('text=Events');
    await expect(page.locator('html')).toHaveClass(/dark/);
  });

  test('should maintain dark theme when navigating to ContextEditor screen', async ({ page }) => {
    await page.goto('/');
    await page.click('text=Context Editor');
    await expect(page.locator('html')).toHaveClass(/dark/);
  });

  test('should maintain dark theme across all screens', async ({ page }) => {
    await page.goto('/');
    
    // Navigate through all screens
    const screens = ['New Event', 'Events', 'Context Editor', 'Judgments', 'Settings'];
    
    for (const screenName of screens) {
      await page.click(`text=${screenName}`);
      await expect(page.locator('html')).toHaveClass(/dark/);
    }
  });

  test('should persist dark theme preference after page reload', async ({ page }) => {
    await page.goto('/');
    await expect(page.locator('html')).toHaveClass(/dark/);
    
    await page.reload();
    await expect(page.locator('html')).toHaveClass(/dark/);
  });
});

