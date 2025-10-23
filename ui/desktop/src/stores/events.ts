import { create } from 'zustand';
import { devtools } from 'zustand/middleware';
import { Event, EventDetails, CreateEventRequest, EventFilters, EventSortOptions } from '@/types/events';
import { ApiService } from '@/services/api';
import { errorHandler } from '@/services/errorHandler';
import { performanceMonitor } from '@/services/performance';

interface EventsState {
  // Data
  events: Event[];
  currentEvent: EventDetails | null;
  loading: boolean;
  error: string | null;
  
  // Pagination
  pagination: {
    page: number;
    per_page: number;
    total: number;
    total_pages: number;
  };
  
  // Filters and sorting
  filters: EventFilters;
  sortOptions: EventSortOptions;
  
  // Actions
  fetchEvents: (page?: number, perPage?: number) => Promise<void>;
  fetchEvent: (id: string) => Promise<void>;
  createEvent: (eventData: CreateEventRequest) => Promise<Event | null>;
  updateEvent: (id: string, eventData: Partial<CreateEventRequest>) => Promise<void>;
  deleteEvent: (id: string) => Promise<void>;
  
  // Filter and sort actions
  setFilters: (filters: Partial<EventFilters>) => void;
  setSortOptions: (sortOptions: EventSortOptions) => void;
  clearFilters: () => void;
  
  // Utility actions
  setLoading: (loading: boolean) => void;
  setError: (error: string | null) => void;
  clearError: () => void;
}

const defaultFilters: EventFilters = {};
const defaultSortOptions: EventSortOptions = {
  field: 'created_at',
  direction: 'desc'
};

export const useEventsStore = create<EventsState>()(
  devtools(
    (set) => ({
      // Initial state
      events: [],
      currentEvent: null,
      loading: false,
      error: null,
      pagination: {
        page: 1,
        per_page: 20,
        total: 0,
        total_pages: 0
      },
      filters: defaultFilters,
      sortOptions: defaultSortOptions,

      // Actions
      fetchEvents: async (page = 1, perPage = 20) => {
        set({ loading: true, error: null });
        
        try {
          const response = await performanceMonitor.measureAsync(
            'fetch_events',
            () => ApiService.getEvents(page, perPage)
          );
          
          set({
            events: response.data,
            pagination: response.pagination,
            loading: false
          });
        } catch (error: any) {
          const errorReport = errorHandler.handleError(error, {
            operation: 'fetch_events',
            component: 'EventsStore'
          });
          
          set({
            events: [],
            error: errorHandler.getUserFriendlyMessage(error, errorReport.context || {}),
            loading: false
          });
        }
      },

      fetchEvent: async (id: string) => {
        set({ loading: true, error: null });
        
        try {
          const event = await ApiService.getEvent(id);
          set({
            currentEvent: event,
            loading: false
          });
        } catch (error: any) {
          set({
            error: error.message || 'Failed to fetch event',
            loading: false
          });
        }
      },

      createEvent: async (eventData: CreateEventRequest) => {
        set({ loading: true, error: null });
        
        try {
          const newEvent = await ApiService.createEvent(eventData);
          
          // Add to current events list
          set((state) => ({
            events: [newEvent, ...state.events],
            loading: false
          }));
          
          return newEvent;
        } catch (error: any) {
          const errorMessage = error.message || 'Failed to create event';
          set({
            error: errorMessage,
            loading: false
          });
          
          // If offline, queue the operation
          if (!navigator.onLine || error.code === 'NETWORK_ERROR') {
            // This would integrate with the offline queue service
            console.log('Queuing event creation for offline sync:', eventData);
          }
          
          return null;
        }
      },

      updateEvent: async (id: string, eventData: Partial<CreateEventRequest>) => {
        set({ loading: true, error: null });
        
        try {
          // Note: Update endpoint not implemented in API service yet
          // This would call ApiService.updateEvent(id, eventData)
          
          // For now, just update local state
          set((state) => ({
            events: state.events.map(event => 
              event.id === id ? { ...event, ...eventData } : event
            ),
            currentEvent: state.currentEvent?.id === id 
              ? { ...state.currentEvent, ...eventData }
              : state.currentEvent,
            loading: false
          }));
        } catch (error: any) {
          set({
            error: error.message || 'Failed to update event',
            loading: false
          });
        }
      },

      deleteEvent: async (id: string) => {
        set({ loading: true, error: null });
        
        try {
          // Note: Delete endpoint not implemented in API service yet
          // This would call ApiService.deleteEvent(id)
          
          // For now, just remove from local state
          set((state) => ({
            events: state.events.filter(event => event.id !== id),
            currentEvent: state.currentEvent?.id === id ? null : state.currentEvent,
            loading: false
          }));
        } catch (error: any) {
          set({
            error: error.message || 'Failed to delete event',
            loading: false
          });
        }
      },

      // Filter and sort actions
      setFilters: (filters: Partial<EventFilters>) => {
        set((state) => ({
          filters: { ...state.filters, ...filters }
        }));
      },

      setSortOptions: (sortOptions: EventSortOptions) => {
        set({ sortOptions });
      },

      clearFilters: () => {
        set({ filters: defaultFilters });
      },

      // Utility actions
      setLoading: (loading: boolean) => {
        set({ loading });
      },

      setError: (error: string | null) => {
        set({ error });
      },

      clearError: () => {
        set({ error: null });
      }
    }),
    {
      name: 'events-store',
    }
  )
);
