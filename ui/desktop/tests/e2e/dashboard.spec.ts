import { test, expect } from '@playwright/test';

test.describe('Dashboard', () => {
  test.beforeEach(async ({ page }) => {
    // Mock API responses
    await page.route('**/api/v1/events**', async route => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          data: [
            {
              id: '1',
              title: 'Test Event 1',
              description: 'Test Description 1',
              created_at: '2024-01-01T00:00:00Z',
              status: 'active'
            },
            {
              id: '2',
              title: 'Test Event 2',
              description: 'Test Description 2',
              created_at: '2024-01-01T01:00:00Z',
              status: 'active'
            }
          ],
          pagination: {
            page: 1,
            per_page: 20,
            total: 2,
            total_pages: 1
          }
        })
      });
    });

    await page.route('**/api/v1/sync/status**', async route => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          is_online: true,
          last_sync: '2024-01-01T12:00:00Z',
          pending_operations: 0,
          sync_in_progress: false
        })
      });
    });

    await page.goto('/');
  });

  test('should display dashboard with events', async ({ page }) => {
    // Check page title
    await expect(page.getByRole('heading', { name: 'Truth Training' })).toBeVisible();
    await expect(page.getByText('Collective Intelligence Dashboard')).toBeVisible();

    // Check stats cards
    await expect(page.getByText('Total Events')).toBeVisible();
    await expect(page.getByText('Active Events')).toBeVisible();
    await expect(page.getByText('With Consensus')).toBeVisible();
    await expect(page.getByText('Participants')).toBeVisible();

    // Check events list
    await expect(page.getByText('Recent Events')).toBeVisible();
    await expect(page.getByText('Test Event 1')).toBeVisible();
    await expect(page.getByText('Test Event 2')).toBeVisible();

    // Check sync status
    await expect(page.getByText('Synced')).toBeVisible();
  });

  test('should display loading state', async ({ page }) => {
    // Navigate to page without mocking API
    await page.unroute('**/api/v1/events**');
    await page.goto('/');

    // Check loading spinner
    await expect(page.locator('.animate-spin')).toBeVisible();
  });

  test('should display error state', async ({ page }) => {
    // Mock API error
    await page.route('**/api/v1/events**', async route => {
      await route.fulfill({
        status: 500,
        contentType: 'application/json',
        body: JSON.stringify({
          error: 'Internal Server Error'
        })
      });
    });

    await page.goto('/');

    // Check error message
    await expect(page.getByText('Error Loading Dashboard')).toBeVisible();
    await expect(page.getByRole('button', { name: 'Retry' })).toBeVisible();
  });

  test('should handle retry button', async ({ page }) => {
    // Mock initial error
    await page.route('**/api/v1/events**', async route => {
      await route.fulfill({
        status: 500,
        contentType: 'application/json',
        body: JSON.stringify({
          error: 'Internal Server Error'
        })
      });
    });

    await page.goto('/');
    await expect(page.getByText('Error Loading Dashboard')).toBeVisible();

    // Mock successful response for retry
    await page.route('**/api/v1/events**', async route => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          data: [
            {
              id: '1',
              title: 'Test Event 1',
              description: 'Test Description 1',
              created_at: '2024-01-01T00:00:00Z',
              status: 'active'
            }
          ],
          pagination: {
            page: 1,
            per_page: 20,
            total: 1,
            total_pages: 1
          }
        })
      });
    });

    // Click retry button
    await page.getByRole('button', { name: 'Retry' }).click();

    // Check that dashboard loads successfully
    await expect(page.getByText('Test Event 1')).toBeVisible();
  });

  test('should display empty state when no events', async ({ page }) => {
    // Mock empty events response
    await page.route('**/api/v1/events**', async route => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          data: [],
          pagination: {
            page: 1,
            per_page: 20,
            total: 0,
            total_pages: 0
          }
        })
      });
    });

    await page.goto('/');

    // Check empty state
    await expect(page.getByText('No events yet')).toBeVisible();
    await expect(page.getByText('Get started by creating your first event.')).toBeVisible();
    await expect(page.getByRole('button', { name: 'Create Event' })).toBeVisible();
  });

  test('should handle offline state', async ({ page }) => {
    // Mock offline sync status
    await page.route('**/api/v1/sync/status**', async route => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          is_online: false,
          last_sync: null,
          pending_operations: 3,
          sync_in_progress: false
        })
      });
    });

    await page.goto('/');

    // Check offline status
    await expect(page.getByText('Offline')).toBeVisible();
  });
});
