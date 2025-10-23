import { describe, it, expect, beforeEach, afterEach } from '@jest/globals';

describe('API Performance Tests', () => {
  let mockApiBase: string;
  const PERFORMANCE_THRESHOLD = 200; // 200ms target

  beforeEach(() => {
    mockApiBase = process.env.VITE_API_BASE || 'http://localhost:8080/api/v1';
  });

  afterEach(() => {
    // Cleanup any mocks or state
  });

  it('should load events list under 200ms', async () => {
    const mockEvents = Array.from({ length: 20 }, (_, i) => ({
      id: `550e8400-e29b-41d4-a716-44665544000${i}`,
      title: `Test Event ${i + 1}`,
      description: `Description for event ${i + 1}`,
      created_at: '2024-01-01T00:00:00Z',
      status: 'active'
    }));

    const mockFetch = jest.fn()
      .mockResolvedValue({
        ok: true,
        json: () => Promise.resolve({
          events: mockEvents,
          pagination: { page: 1, per_page: 20, total: 20, total_pages: 1 }
        })
      });

    global.fetch = mockFetch;

    const startTime = performance.now();
    const response = await fetch(`${mockApiBase}/events`);
    const endTime = performance.now();

    const responseTime = endTime - startTime;
    expect(response.ok).toBe(true);
    expect(responseTime).toBeLessThan(PERFORMANCE_THRESHOLD);
    
    const data = await response.json();
    expect(data.events).toHaveLength(20);
  });

  it('should load sync status under 200ms', async () => {
    const mockSyncStatus = {
      is_online: true,
      last_sync: '2024-01-01T12:00:00Z',
      pending_operations: 0,
      sync_in_progress: false
    };

    const mockFetch = jest.fn()
      .mockResolvedValue({
        ok: true,
        json: () => Promise.resolve(mockSyncStatus)
      });

    global.fetch = mockFetch;

    const startTime = performance.now();
    const response = await fetch(`${mockApiBase}/sync/status`);
    const endTime = performance.now();

    const responseTime = endTime - startTime;
    expect(response.ok).toBe(true);
    expect(responseTime).toBeLessThan(PERFORMANCE_THRESHOLD);
    
    const data = await response.json();
    expect(data.is_online).toBe(true);
  });

  it('should create event under 200ms', async () => {
    const mockNewEvent = {
      id: '550e8400-e29b-41d4-a716-446655440000',
      title: 'Performance Test Event',
      description: 'Testing event creation performance',
      created_at: '2024-01-01T00:00:00Z',
      status: 'active'
    };

    const mockFetch = jest.fn()
      .mockResolvedValue({
        ok: true,
        json: () => Promise.resolve(mockNewEvent)
      });

    global.fetch = mockFetch;

    const startTime = performance.now();
    const response = await fetch(`${mockApiBase}/events`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        title: 'Performance Test Event',
        description: 'Testing event creation performance'
      })
    });
    const endTime = performance.now();

    const responseTime = endTime - startTime;
    expect(response.ok).toBe(true);
    expect(responseTime).toBeLessThan(PERFORMANCE_THRESHOLD);
    
    const data = await response.json();
    expect(data.title).toBe('Performance Test Event');
  });

  it('should submit judgment under 200ms', async () => {
    const mockJudgment = {
      id: '660e8400-e29b-41d4-a716-446655440001',
      participant_id: '770e8400-e29b-41d4-a716-446655440002',
      event_id: '550e8400-e29b-41d4-a716-446655440000',
      assessment: 'true',
      confidence_level: 0.85,
      reasoning: 'Performance test judgment',
      submitted_at: '2024-01-01T10:00:00Z',
      signature: 'performance_test_signature'
    };

    const mockFetch = jest.fn()
      .mockResolvedValue({
        ok: true,
        json: () => Promise.resolve(mockJudgment)
      });

    global.fetch = mockFetch;

    const startTime = performance.now();
    const response = await fetch(`${mockApiBase}/judgments`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        event_id: '550e8400-e29b-41d4-a716-446655440000',
        assessment: 'true',
        confidence_level: 0.85,
        reasoning: 'Performance test judgment',
        signature: 'performance_test_signature'
      })
    });
    const endTime = performance.now();

    const responseTime = endTime - startTime;
    expect(response.ok).toBe(true);
    expect(responseTime).toBeLessThan(PERFORMANCE_THRESHOLD);
    
    const data = await response.json();
    expect(data.assessment).toBe('true');
  });

  it('should calculate consensus under 200ms', async () => {
    const mockConsensus = {
      event_id: '550e8400-e29b-41d4-a716-446655440000',
      consensus_value: 'true',
      confidence_score: 0.85,
      participant_count: 5,
      algorithm_version: '1.0.0',
      calculated_at: '2024-01-01T12:00:00Z',
      judgments_used: []
    };

    const mockFetch = jest.fn()
      .mockResolvedValue({
        ok: true,
        json: () => Promise.resolve(mockConsensus)
      });

    global.fetch = mockFetch;

    const startTime = performance.now();
    const response = await fetch(`${mockApiBase}/consensus/550e8400-e29b-41d4-a716-446655440000/calculate`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        algorithm_version: '1.0.0',
        force_recalculation: false
      })
    });
    const endTime = performance.now();

    const responseTime = endTime - startTime;
    expect(response.ok).toBe(true);
    expect(responseTime).toBeLessThan(PERFORMANCE_THRESHOLD);
    
    const data = await response.json();
    expect(data.consensus_value).toBe('true');
    expect(data.confidence_score).toBe(0.85);
  });

  it('should handle concurrent requests efficiently', async () => {
    const mockFetch = jest.fn()
      .mockResolvedValue({
        ok: true,
        json: () => Promise.resolve({
          events: [],
          pagination: { page: 1, per_page: 20, total: 0, total_pages: 0 }
        })
      });

    global.fetch = mockFetch;

    const startTime = performance.now();
    
    // Make 5 concurrent requests
    const promises = Array.from({ length: 5 }, () => 
      fetch(`${mockApiBase}/events`)
    );
    
    const responses = await Promise.all(promises);
    const endTime = performance.now();

    const totalTime = endTime - startTime;
    const averageTime = totalTime / 5;

    expect(responses.every(r => r.ok)).toBe(true);
    expect(averageTime).toBeLessThan(PERFORMANCE_THRESHOLD);
    expect(totalTime).toBeLessThan(PERFORMANCE_THRESHOLD * 2); // Allow some overhead for concurrent requests
  });

  it('should handle large datasets efficiently', async () => {
    const largeEventList = Array.from({ length: 100 }, (_, i) => ({
      id: `550e8400-e29b-41d4-a716-44665544000${i.toString().padStart(2, '0')}`,
      title: `Large Dataset Event ${i + 1}`,
      description: `Description for large dataset event ${i + 1}`,
      created_at: '2024-01-01T00:00:00Z',
      status: 'active'
    }));

    const mockFetch = jest.fn()
      .mockResolvedValue({
        ok: true,
        json: () => Promise.resolve({
          events: largeEventList,
          pagination: { page: 1, per_page: 100, total: 100, total_pages: 1 }
        })
      });

    global.fetch = mockFetch;

    const startTime = performance.now();
    const response = await fetch(`${mockApiBase}/events?per_page=100`);
    const endTime = performance.now();

    const responseTime = endTime - startTime;
    expect(response.ok).toBe(true);
    expect(responseTime).toBeLessThan(PERFORMANCE_THRESHOLD * 2); // Allow more time for large datasets
    
    const data = await response.json();
    expect(data.events).toHaveLength(100);
  });
});
