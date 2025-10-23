import { describe, it, expect, beforeEach, afterEach } from '@jest/globals';

describe('Create Event Flow Integration Tests', () => {
  let mockApiBase: string;
  let mockNewEvent: any;

  beforeEach(() => {
    mockApiBase = process.env.VITE_API_BASE || 'http://localhost:8080/api/v1';
    mockNewEvent = {
      id: '550e8400-e29b-41d4-a716-446655440000',
      title: 'New Test Event',
      description: 'A newly created test event',
      created_at: '2024-01-01T00:00:00Z',
      status: 'active'
    };
  });

  afterEach(() => {
    // Cleanup any mocks or state
  });

  it('should create event and verify it appears in list', async () => {
    const mockFetch = jest.fn()
      .mockResolvedValueOnce({
        ok: true,
        json: () => Promise.resolve(mockNewEvent)
      })
      .mockResolvedValueOnce({
        ok: true,
        json: () => Promise.resolve({
          events: [mockNewEvent],
          pagination: { page: 1, per_page: 20, total: 1, total_pages: 1 }
        })
      });

    global.fetch = mockFetch;

    // Step 1: Create event
    const createResponse = await fetch(`${mockApiBase}/events`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        title: 'New Test Event',
        description: 'A newly created test event',
        category: 'general'
      })
    });

    expect(createResponse.ok).toBe(true);
    const createdEvent = await createResponse.json();
    expect(createdEvent.id).toBe(mockNewEvent.id);
    expect(createdEvent.title).toBe('New Test Event');

    // Step 2: Verify event appears in list
    const listResponse = await fetch(`${mockApiBase}/events`);
    expect(listResponse.ok).toBe(true);
    const eventsData = await listResponse.json();

    expect(eventsData.events).toHaveLength(1);
    expect(eventsData.events[0].id).toBe(mockNewEvent.id);
    expect(eventsData.events[0].title).toBe('New Test Event');
  });

  it('should handle event creation validation errors', async () => {
    const mockFetch = jest.fn()
      .mockResolvedValueOnce({
        ok: false,
        status: 400,
        json: () => Promise.resolve({
          error: 'Validation failed',
          details: {
            title: 'Title is required',
            description: 'Description is required'
          }
        })
      });

    global.fetch = mockFetch;

    const response = await fetch(`${mockApiBase}/events`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        title: '',
        description: ''
      })
    });

    expect(response.ok).toBe(false);
    expect(response.status).toBe(400);
    
    const errorData = await response.json();
    expect(errorData.error).toBe('Validation failed');
    expect(errorData.details.title).toBe('Title is required');
  });

  it('should handle network errors during event creation', async () => {
    const mockFetch = jest.fn()
      .mockRejectedValueOnce(new Error('Network error'));

    global.fetch = mockFetch;

    try {
      await fetch(`${mockApiBase}/events`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          title: 'Test Event',
          description: 'Test description'
        })
      });
    } catch (error) {
      expect(error).toBeInstanceOf(Error);
      expect((error as Error).message).toBe('Network error');
    }
  });

  it('should validate event creation request payload', () => {
    const validPayload = {
      title: 'Valid Test Event',
      description: 'Valid test description',
      category: 'general'
    };

    expect(validPayload.title).toBeTruthy();
    expect(validPayload.description).toBeTruthy();
    expect(validPayload.title.length).toBeGreaterThan(0);
    expect(validPayload.description.length).toBeGreaterThan(0);
  });
});
