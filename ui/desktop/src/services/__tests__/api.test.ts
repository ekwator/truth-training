import { describe, it, expect, beforeEach, afterEach, jest } from '@jest/globals';

// Mock axios before importing ApiService
jest.mock('axios');
import axios from 'axios';
const mockedAxios = jest.mocked(axios);

// Mock axios.create to return a proper instance
mockedAxios.create.mockReturnValue({
  get: jest.fn(),
  post: jest.fn(),
  put: jest.fn(),
  delete: jest.fn(),
  interceptors: {
    request: { use: jest.fn() },
    response: { use: jest.fn() }
  }
} as any);

// Mock Tauri API
jest.mock('@tauri-apps/api/core', () => ({
  invoke: jest.fn()
}));

// Mock the isTauri function by mocking the module
jest.mock('../api', () => {
  const originalModule = jest.requireActual('../api');
  return {
    ...originalModule,
    // Override the isTauri detection to return false for tests
  };
});

import { ApiService, setApiClient } from '../api';

describe('ApiService', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe('getEvents', () => {
    it('should fetch events successfully', async () => {
      const mockResponse = {
        data: {
          data: [
            {
              id: 1,
              description: 'Test Event',
              vector: true,
              corrected: false,
              timestamp_start: 1_700_000_000,
              code: 1
            }
          ],
          pagination: {
            page: 1,
            per_page: 20,
            total: 1,
            total_pages: 1
          }
        }
      };

      const mockApiClient = {
        get: jest.fn().mockResolvedValue(mockResponse),
        post: jest.fn(),
        put: jest.fn(),
        delete: jest.fn(),
        interceptors: {
          request: { use: jest.fn() },
          response: { use: jest.fn() }
        }
      };

      setApiClient(mockApiClient as any);

      const result = await ApiService.getEvents(1, 20);
      
      expect(result.data).toEqual(mockResponse.data.data);
      expect(result.pagination).toEqual(mockResponse.data.pagination);
    });

    it('should handle API errors', async () => {
      const mockError = new Error('Network error');
      const mockApiClient = {
        get: jest.fn().mockRejectedValue(mockError),
        post: jest.fn(),
        put: jest.fn(),
        delete: jest.fn(),
        interceptors: {
          request: { use: jest.fn() },
          response: { use: jest.fn() }
        }
      };

      setApiClient(mockApiClient as any);

      await expect(ApiService.getEvents()).rejects.toThrow('Network error');
    });
  });

  describe('createEvent', () => {
    it('should create event successfully', async () => {
      const mockEvent = {
        id: 1,
        description: 'New Description',
        vector: true,
        corrected: false,
        timestamp_start: 1_700_000_100,
        code: 1
      };

      const mockApiClient = {
        get: jest.fn(),
        post: jest.fn().mockResolvedValue({ data: mockEvent }),
        put: jest.fn(),
        delete: jest.fn(),
        interceptors: {
          request: { use: jest.fn() },
          response: { use: jest.fn() }
        }
      };

      setApiClient(mockApiClient as any);

      const result = await ApiService.createEvent({
        description: 'New Description',
        vector: true
      });
      
      expect(result).toEqual(mockEvent);
    });
  });

  describe('getJudgments', () => {
    it('should fetch judgments with event filter', async () => {
      const mockResponse = {
        data: {
          data: [
            {
              id: '1',
              participant_id: 'p1',
              event_id: 1,
              assessment: 'confirm',
              confidence_level: 0.8,
              submitted_at: '2024-01-01T00:00:00Z',
              signature: 'sig1'
            }
          ],
          pagination: {
            page: 1,
            per_page: 20,
            total: 1,
            total_pages: 1
          }
        }
      };

      const mockApiClient = {
        get: jest.fn().mockResolvedValue(mockResponse),
        post: jest.fn(),
        put: jest.fn(),
        delete: jest.fn(),
        interceptors: {
          request: { use: jest.fn() },
          response: { use: jest.fn() }
        }
      };

      setApiClient(mockApiClient as any);

      const result = await ApiService.getJudgments(1, 1, 20);
      
      expect(result.data).toEqual(mockResponse.data.data);
      expect(result.pagination).toEqual(mockResponse.data.pagination);
    });
  });

  describe('getSyncStatus', () => {
    it('should fetch sync status successfully', async () => {
      const mockStatus = {
        is_online: true,
        last_sync: '2024-01-01T00:00:00Z',
        pending_operations: 0,
        sync_in_progress: false
      };

      const mockApiClient = {
        get: jest.fn().mockResolvedValue({ data: mockStatus }),
        post: jest.fn(),
        put: jest.fn(),
        delete: jest.fn(),
        interceptors: {
          request: { use: jest.fn() },
          response: { use: jest.fn() }
        }
      };

      setApiClient(mockApiClient as any);

      const result = await ApiService.getSyncStatus();
      
      expect(result).toEqual(mockStatus);
    });
  });

  describe('healthCheck', () => {
    it('should return true when healthy', async () => {
      const mockApiClient = {
        get: jest.fn().mockResolvedValue({ status: 200 }),
        post: jest.fn(),
        put: jest.fn(),
        delete: jest.fn(),
        interceptors: {
          request: { use: jest.fn() },
          response: { use: jest.fn() }
        }
      };

      setApiClient(mockApiClient as any);

      const result = await ApiService.healthCheck();
      expect(result).toBe(true);
    });

    it('should return false when unhealthy', async () => {
      const mockApiClient = {
        get: jest.fn().mockRejectedValue(new Error('Network error')),
        post: jest.fn(),
        put: jest.fn(),
        delete: jest.fn(),
        interceptors: {
          request: { use: jest.fn() },
          response: { use: jest.fn() }
        }
      };

      setApiClient(mockApiClient as any);

      const result = await ApiService.healthCheck();
      expect(result).toBe(false);
    });
  });
});
