import { test, expect } from '@playwright/test';

test.describe('Events Page', () => {
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
              status: 'inactive'
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

    await page.goto('/events');
  });

  test('should display events page', async ({ page }) => {
    // Check page title
    await expect(page.getByRole('heading', { name: 'Events' })).toBeVisible();
    await expect(page.getByText('Manage and view all events')).toBeVisible();

    // Check search and filters
    await expect(page.getByPlaceholder('Search by title or description...')).toBeVisible();
    await expect(page.getByLabel('Status')).toBeVisible();

    // Check events grid
    await expect(page.getByText('Test Event 1')).toBeVisible();
    await expect(page.getByText('Test Event 2')).toBeVisible();
  });

  test('should filter events by status', async ({ page }) => {
    // Select active status filter
    await page.getByLabel('Status').selectOption('active');

    // Check that only active events are shown
    await expect(page.getByText('Test Event 1')).toBeVisible();
    await expect(page.getByText('Test Event 2')).not.toBeVisible();
  });

  test('should search events', async ({ page }) => {
    // Type in search box
    await page.getByPlaceholder('Search by title or description...').fill('Test Event 1');

    // Check that only matching event is shown
    await expect(page.getByText('Test Event 1')).toBeVisible();
    await expect(page.getByText('Test Event 2')).not.toBeVisible();
  });

  test('should display empty state when no events match search', async ({ page }) => {
    // Type in search box with no matches
    await page.getByPlaceholder('Search by title or description...').fill('Non-existent Event');

    // Check empty state
    await expect(page.getByText('No events match your search')).toBeVisible();
    await expect(page.getByText('Try adjusting your search terms.')).toBeVisible();
  });

  test('should display empty state when no events exist', async ({ page }) => {
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

    await page.goto('/events');

    // Check empty state
    await expect(page.getByText('No events yet')).toBeVisible();
    await expect(page.getByText('Get started by creating your first event.')).toBeVisible();
    await expect(page.getByRole('button', { name: 'Create Event' })).toBeVisible();
  });

  test('should handle API errors', async ({ page }) => {
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

    await page.goto('/events');

    // Check error message
    await expect(page.getByText('Error Loading Events')).toBeVisible();
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

    await page.goto('/events');
    await expect(page.getByText('Error Loading Events')).toBeVisible();

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

    // Check that events load successfully
    await expect(page.getByText('Test Event 1')).toBeVisible();
  });
});
