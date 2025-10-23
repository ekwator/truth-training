import { describe, it, expect } from '@jest/globals';

describe('Events List Contract Tests', () => {
  it('should validate Events list response with pagination', () => {
    const mockEventsResponse = {
      events: [
        {
          id: '550e8400-e29b-41d4-a716-446655440000',
          title: 'Test Event',
          description: 'A test event for validation',
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
    };

    // Validate events array structure
    expect(Array.isArray(mockEventsResponse.events)).toBe(true);
    expect(mockEventsResponse.events[0]).toHaveProperty('id');
    expect(mockEventsResponse.events[0]).toHaveProperty('title');
    expect(mockEventsResponse.events[0]).toHaveProperty('description');
    expect(mockEventsResponse.events[0]).toHaveProperty('created_at');
    expect(mockEventsResponse.events[0]).toHaveProperty('status');

    // Validate pagination structure
    expect(mockEventsResponse.pagination).toHaveProperty('page');
    expect(mockEventsResponse.pagination).toHaveProperty('per_page');
    expect(mockEventsResponse.pagination).toHaveProperty('total');
    expect(mockEventsResponse.pagination).toHaveProperty('total_pages');

    // Validate types
    expect(typeof mockEventsResponse.pagination.page).toBe('number');
    expect(typeof mockEventsResponse.pagination.per_page).toBe('number');
    expect(typeof mockEventsResponse.pagination.total).toBe('number');
    expect(typeof mockEventsResponse.pagination.total_pages).toBe('number');
  });

  it('should handle empty events list', () => {
    const emptyResponse = {
      events: [],
      pagination: {
        page: 1,
        per_page: 20,
        total: 0,
        total_pages: 0
      }
    };

    expect(Array.isArray(emptyResponse.events)).toBe(true);
    expect(emptyResponse.events.length).toBe(0);
    expect(emptyResponse.pagination.total).toBe(0);
  });
});
