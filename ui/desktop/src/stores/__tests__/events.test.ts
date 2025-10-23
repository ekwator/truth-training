import { describe, it, expect, beforeEach, afterEach, jest } from '@jest/globals';
import { useEventsStore } from '../events';
import { ApiService } from '@/services/api';

// Mock API service
jest.mock('@/services/api', () => ({
  ApiService: {
    getEvents: jest.fn(),
    getEvent: jest.fn(),
    createEvent: jest.fn(),
    updateEvent: jest.fn(),
    deleteEvent: jest.fn()
  }
}));

// Mock error handler
jest.mock('@/services/errorHandler', () => ({
  errorHandler: {
    handleError: jest.fn().mockReturnValue({
      context: {
        operation: 'test',
        component: 'test',
        timestamp: '2024-01-01T00:00:00Z',
        userAgent: 'test'
      }
    }),
    getUserFriendlyMessage: jest.fn().mockReturnValue('Test error message')
  }
}));

// Mock performance monitor
jest.mock('@/services/performance', () => ({
  performanceMonitor: {
    measureAsync: jest.fn()
  }
}));

describe('EventsStore', () => {
  beforeEach(() => {
    // Reset store state
    useEventsStore.getState().clearError();
    useEventsStore.getState().setLoading(false);
  });

  afterEach(() => {
    jest.clearAllMocks();
  });

  describe('fetchEvents', () => {
    it('should fetch events successfully', async () => {
      const mockEvents = [
        {
          id: '1',
          title: 'Test Event 1',
          description: 'Test Description 1',
          created_at: '2024-01-01T00:00:00Z',
          status: 'active'
        }
      ];

      const mockResponse = {
        data: mockEvents,
        pagination: {
          page: 1,
          per_page: 20,
          total: 1,
          total_pages: 1
        }
      };

      jest.mocked(ApiService.getEvents).mockResolvedValue(mockResponse);
      jest.mocked(require('@/services/performance').performanceMonitor.measureAsync)
        .mockImplementation(async (name, fn) => fn());

      await useEventsStore.getState().fetchEvents();

      const state = useEventsStore.getState();
      expect(state.events).toEqual(mockEvents);
      expect(state.pagination).toEqual(mockResponse.pagination);
      expect(state.loading).toBe(false);
      expect(state.error).toBeNull();
    });

    it('should handle fetch errors', async () => {
      const mockError = new Error('Network error');
      jest.mocked(ApiService.getEvents).mockRejectedValue(mockError);
      jest.mocked(require('@/services/performance').performanceMonitor.measureAsync)
        .mockImplementation(async (name, fn) => fn());

      await useEventsStore.getState().fetchEvents();

      const state = useEventsStore.getState();
      expect(state.events).toEqual([]);
      expect(state.loading).toBe(false);
      expect(state.error).toBeTruthy();
    });
  });

  describe('createEvent', () => {
    it('should create event successfully', async () => {
      const mockEvent = {
        id: '1',
        title: 'New Event',
        description: 'New Description',
        created_at: '2024-01-01T00:00:00Z',
        status: 'active'
      };

      jest.mocked(ApiService.createEvent).mockResolvedValue(mockEvent);
      jest.mocked(require('@/services/performance').performanceMonitor.measureAsync)
        .mockImplementation(async (name, fn) => fn());

      const result = await useEventsStore.getState().createEvent({
        title: 'New Event',
        description: 'New Description'
      });

      expect(result).toEqual(mockEvent);
      
      const state = useEventsStore.getState();
      expect(state.events).toContain(mockEvent);
      expect(state.loading).toBe(false);
    });

    it('should handle create event errors', async () => {
      const mockError = new Error('Validation error');
      jest.mocked(ApiService.createEvent).mockRejectedValue(mockError);
      jest.mocked(require('@/services/performance').performanceMonitor.measureAsync)
        .mockImplementation(async (name, fn) => fn());

      const result = await useEventsStore.getState().createEvent({
        title: 'New Event',
        description: 'New Description'
      });

      expect(result).toBeNull();
      
      const state = useEventsStore.getState();
      expect(state.loading).toBe(false);
      expect(state.error).toBeTruthy();
    });
  });

  describe('filters and sorting', () => {
    it('should set filters correctly', () => {
      const filters = { status: 'active' as const };
      useEventsStore.getState().setFilters(filters);
      
      const state = useEventsStore.getState();
      expect(state.filters.status).toBe('active');
    });

    it('should set sort options correctly', () => {
      const sortOptions = { field: 'title' as const, direction: 'asc' as const };
      useEventsStore.getState().setSortOptions(sortOptions);
      
      const state = useEventsStore.getState();
      expect(state.sortOptions).toEqual(sortOptions);
    });

    it('should clear filters', () => {
      useEventsStore.getState().setFilters({ status: 'active' });
      useEventsStore.getState().clearFilters();
      
      const state = useEventsStore.getState();
      expect(state.filters).toEqual({});
    });
  });

  describe('utility functions', () => {
    it('should set loading state', () => {
      useEventsStore.getState().setLoading(true);
      expect(useEventsStore.getState().loading).toBe(true);
    });

    it('should set error state', () => {
      const error = 'Test error';
      useEventsStore.getState().setError(error);
      expect(useEventsStore.getState().error).toBe(error);
    });

    it('should clear error state', () => {
      useEventsStore.getState().setError('Test error');
      useEventsStore.getState().clearError();
      expect(useEventsStore.getState().error).toBeNull();
    });
  });
});
