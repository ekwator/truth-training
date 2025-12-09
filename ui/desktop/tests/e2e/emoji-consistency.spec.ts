/**
 * E2E test for emoji consistency across application.
 * Verifies emoji consistency patterns (same function = same emoji).
 */

import { test, expect } from '@playwright/test';

test.describe('Emoji Consistency E2E Tests', () => {
  test.beforeEach(async ({ page }) => {
    // Navigate to application (adjust URL as needed)
    await page.goto('http://localhost:1420'); // Default Tauri dev server URL
  });

  test('should have consistent emojis for same functions across screens', async ({ page }) => {
    // Check Dashboard screen emojis
    await page.click('text=Dashboard');
    const dashboardEmoji = await page.locator('h1').first().textContent();
    expect(dashboardEmoji).toContain('🏠'); // Dashboard emoji

    // Check navigation - home should have same emoji
    const homeNavEmoji = await page.locator('button:has-text("Dashboard")').first().textContent();
    expect(homeNavEmoji).toContain('🏠'); // Same emoji for home/dashboard

    // Check New Event screen
    await page.click('text=New Event');
    const newEventEmoji = await page.locator('h1').first().textContent();
    expect(newEventEmoji).toContain('➕'); // New Event emoji

    // Check that create buttons use same emoji
    const createButtonEmoji = await page.locator('button:has-text("Create")').first().textContent();
    expect(createButtonEmoji).toContain('➕'); // Same emoji for create actions
  });

  test('should have emojis in all navigation items', async ({ page }) => {
    const navItems = [
      'Dashboard',
      'New Event',
      'Context Editor',
      'Overall Summary',
      'Training Results',
      'Settings'
    ];

    for (const item of navItems) {
      const navButton = page.locator(`button:has-text("${item}")`).first();
      const text = await navButton.textContent();
      // Check that button text contains emoji (basic check)
      expect(text).toBeTruthy();
      // Emoji should be present (non-empty text with emoji character)
      const hasEmoji = /[\u{1F300}-\u{1F9FF}]|[\u{2600}-\u{26FF}]|[\u{2700}-\u{27BF}]/u.test(text || '');
      expect(hasEmoji || text?.includes('getEmoji')).toBeTruthy();
    }
  });

  test('should have emojis in all action buttons', async ({ page }) => {
    await page.click('text=Dashboard');
    
    // Check action buttons on dashboard
    const actionButtons = [
      'View Events',
      'View Judgments',
      'New Event',
      'Manage Context Templates',
      'Overall Summary',
      'Training Results',
      'Settings'
    ];

    for (const buttonText of actionButtons) {
      const button = page.locator(`button:has-text("${buttonText}")`).first();
      if (await button.count() > 0) {
        const text = await button.textContent();
        expect(text).toBeTruthy();
        // Button should have emoji or use getEmoji function
        const hasEmoji = /[\u{1F300}-\u{1F9FF}]|[\u{2600}-\u{26FF}]|[\u{2700}-\u{27BF}]/u.test(text || '');
        expect(hasEmoji || text?.includes('getEmoji')).toBeTruthy();
      }
    }
  });

  test('should have consistent emoji patterns for status indicators', async ({ page }) => {
    await page.click('text=Dashboard');
    
    // Check sync status (if visible)
    const syncStatus = page.locator('text=/Online|Offline|Synced/').first();
    if (await syncStatus.count() > 0) {
      const text = await syncStatus.textContent();
      // Status should have emoji
      const hasEmoji = /[\u{1F300}-\u{1F9FF}]|[\u{2600}-\u{26FF}]|[\u{2700}-\u{27BF}]/u.test(text || '');
      expect(hasEmoji).toBeTruthy();
    }
  });

  test('should have emojis in form labels', async ({ page }) => {
    await page.click('text=New Event');
    
    // Check form labels
    const labels = page.locator('label');
    const labelCount = await labels.count();
    
    for (let i = 0; i < Math.min(labelCount, 5); i++) {
      const label = labels.nth(i);
      const text = await label.textContent();
      if (text && text.trim().length > 0) {
        // Label should have emoji or use getEmoji
        const hasEmoji = /[\u{1F300}-\u{1F9FF}]|[\u{2600}-\u{26FF}]|[\u{2700}-\u{27BF}]/u.test(text);
        expect(hasEmoji || text.includes('getEmoji')).toBeTruthy();
      }
    }
  });
});

