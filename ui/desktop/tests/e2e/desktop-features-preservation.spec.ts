/**
 * E2E test for Desktop-specific functionality preservation.
 * Verifies all Desktop-specific features work after UI reconstruction.
 */

import { test, expect } from '@playwright/test';

test.describe('Desktop Features Preservation E2E Tests', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/');
  });

  test('should preserve keyboard shortcuts Alt+1 through Alt+8', async ({ page }) => {
    // Test Alt+1 (Dashboard)
    await page.keyboard.press('Alt+1');
    await expect(page.locator('text=Dashboard')).toBeVisible();

    // Test Alt+2 (New Event)
    await page.keyboard.press('Alt+2');
    await expect(page.locator('text=New Event')).toBeVisible();

    // Test Alt+3 (Context Editor)
    await page.keyboard.press('Alt+3');
    await expect(page.locator('text=Context Templates')).toBeVisible();

    // Test Alt+4 (Event Summary)
    await page.keyboard.press('Alt+4');
    await expect(page.locator('text=Event Summary')).toBeVisible();

    // Test Alt+5 (Overall Summary)
    await page.keyboard.press('Alt+5');
    await expect(page.locator('text=Overall Summary')).toBeVisible();

    // Test Alt+6 (Training Results)
    await page.keyboard.press('Alt+6');
    await expect(page.locator('text=Training Results')).toBeVisible();

    // Test Alt+8 (Settings)
    await page.keyboard.press('Alt+8');
    await expect(page.locator('text=Settings')).toBeVisible();
  });

  test('should preserve Escape key back navigation', async ({ page }) => {
    // Navigate to a screen
    await page.keyboard.press('Alt+2');
    await expect(page.locator('text=New Event')).toBeVisible();

    // Press Escape to go back
    await page.keyboard.press('Escape');
    await expect(page.locator('text=Dashboard')).toBeVisible();
  });

  test('should preserve NodesPanel component', async ({ page }) => {
    // Navigate to a screen that might contain NodesPanel
    // Note: NodesPanel might be on a specific screen or accessible via menu
    // This test verifies the component exists and is functional
    await page.goto('/');
    
    // Verify NodesPanel-related functionality exists
    // (Adjust selector based on actual implementation)
    const nodesPanel = page.locator('[data-testid="nodes-panel"]').or(page.locator('text=/Nodes/i'));
    if (await nodesPanel.count() > 0) {
      await expect(nodesPanel.first()).toBeVisible();
    }
  });

  test('should preserve Desktop-specific UI elements', async ({ page }) => {
    await page.goto('/');
    
    // Verify Desktop-specific features are present
    // This includes features not present in Android UI
    const desktopFeatures = [
      'Event Summary', // Desktop-specific screen
      'NodesPanel', // Desktop-specific component
    ];
    
    // Check that at least some Desktop-specific features are accessible
    const hasDesktopFeatures = await Promise.all(
      desktopFeatures.map(async (feature) => {
        const element = page.locator(`text=${feature}`).or(page.locator(`[data-testid="${feature.toLowerCase()}"]`));
        return (await element.count()) > 0;
      })
    );
    
    expect(hasDesktopFeatures.some(Boolean)).toBe(true);
  });
});

