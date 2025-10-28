import axios, { AxiosInstance, AxiosResponse } from 'axios';
import { config } from '@/config/env';

// API Configuration
const API_BASE_URL = config.API_BASE_URL;

// Create axios instance with default config
let apiClient: AxiosInstance = axios.create({
  baseURL: API_BASE_URL,
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Function to set custom client for testing
export const setApiClient = (client: AxiosInstance) => {
  apiClient = client;
};

// Check if we're running in Tauri (robust for v1/v2)
const isTauri = () => {
  try {
    // Vite + Tauri injects this flag
    if ((import.meta as any)?.env?.TAURI) return true;
  } catch {}
  if (typeof window === 'undefined') return false;
  const w = window as any;
  return Boolean(w.__TAURI__ || w.__TAURI_IPC__ || w.__TAURI_INTERNALS__);
};

// Request interceptor for logging
if (apiClient.interceptors) {
  apiClient.interceptors.request.use(
    (config) => {
      console.log(`API Request: ${config.method?.toUpperCase()} ${config.url}`);
      return config;
    },
    (error) => {
      console.error('API Request Error:', error);
      return Promise.reject(error);
    }
  );

  // Response interceptor for error handling
  apiClient.interceptors.response.use(
    (response: AxiosResponse) => {
      console.log(`API Response: ${response.status} ${response.config.url}`);
      return response;
    },
    (error) => {
      console.error('API Response Error:', error.response?.status, error.message);
      return Promise.reject(error);
    }
  );
}

// Types for API responses
export interface ApiResponse<T> {
  data: T;
  status: number;
  message?: string;
}

export interface PaginationMeta {
  page: number;
  per_page: number;
  total: number;
  total_pages: number;
}

export interface PaginatedResponse<T> {
  data: T[];
  pagination: PaginationMeta;
}

// Events API
export interface Event {
  id: string;
  title: string;
  description: string;
  created_at: string;
  updated_at?: string;
  status: 'active' | 'inactive' | 'archived';
}

export interface EventDetails extends Event {
  consensus?: Consensus | null;
  judgments: Judgment[];
}

export interface CreateEventRequest {
  title: string;
  description: string;
  category?: string;
}

// Judgments API
export interface Judgment {
  id: string;
  participant_id: string;
  event_id: string;
  assessment: 'true' | 'false' | 'uncertain';
  confidence_level: number;
  reasoning?: string;
  submitted_at: string;
  signature: string;
}

export interface CreateJudgmentRequest {
  event_id: string;
  assessment: 'true' | 'false' | 'uncertain';
  confidence_level: number;
  reasoning?: string;
  signature: string;
}

// Consensus API
export interface Consensus {
  event_id: string;
  consensus_value: 'true' | 'false' | 'uncertain' | null;
  confidence_score: number;
  participant_count: number;
  algorithm_version: string;
  calculated_at: string;
  judgments_used: Judgment[];
}

export interface CalculateConsensusRequest {
  algorithm_version?: string;
  force_recalculation?: boolean;
}

// Sync API
export interface SyncStatus {
  is_online: boolean;
  last_sync: string | null;
  pending_operations: number;
  sync_in_progress: boolean;
}

// API Service Class
export class ApiService {
  // Events endpoints
  static async getEvents(page: number = 1, perPage: number = 20): Promise<PaginatedResponse<Event>> {
    if (isTauri()) {
      try {
        const { invoke } = await import('@tauri-apps/api/core');
        const res = await invoke('list_events_fast', { page, perPage });
        const { data, total } = res as { data: Event[]; total: number };
        return {
          data,
          pagination: {
            page,
            per_page: perPage,
            total,
            total_pages: Math.max(1, Math.ceil(total / perPage))
          }
        };
      } catch (error) {
        console.error('Tauri list_events error:', error);
        throw new Error('Failed to fetch events from desktop backend');
      }
    } else {
      // Use HTTP API for web development
      const response = await apiClient.get(`/events?page=${page}&per_page=${perPage}`);
      return response.data;
    }
  }

  static async getEvent(id: string): Promise<EventDetails> {
    if (isTauri()) {
      try {
        const { invoke } = await import('@tauri-apps/api/core');
        const event = await invoke('get_event_fast', { eventId: id });
        return {
          ...event as Event,
          consensus: null,
          judgments: []
        };
      } catch (error) {
        console.error('Tauri getEvent error:', error);
        throw new Error('Failed to fetch event from desktop backend');
      }
    } else {
      const response = await apiClient.get(`/events/${id}`);
      return response.data;
    }
  }

  static async createEvent(eventData: CreateEventRequest): Promise<Event> {
    if (isTauri()) {
      try {
        const now = new Date().toISOString();
        const newEvent: Event = {
          id: crypto.randomUUID(),
          title: eventData.title,
          description: eventData.description,
          created_at: now,
          status: 'active'
        };
        const raw = localStorage.getItem('tt_events');
        const items: Event[] = raw ? JSON.parse(raw) : [];
        items.unshift(newEvent);
        localStorage.setItem('tt_events', JSON.stringify(items));
        return newEvent;
      } catch (error) {
        console.error('Desktop local storage write error:', error);
        throw new Error('Failed to create event in desktop storage');
      }
    } else {
      const response = await apiClient.post('/events', eventData);
      return response.data;
    }
  }

  // Judgments endpoints
  static async getJudgments(eventId?: string, page: number = 1, perPage: number = 20): Promise<PaginatedResponse<Judgment>> {
    if (isTauri()) {
      const { invoke } = await import('@tauri-apps/api/core');
      const res = await invoke('judgments_list_fast', { eventId: eventId || '', page, perPage });
      const { data, total } = res as { data: Judgment[]; total: number };
      return {
        data,
        pagination: { page, per_page: perPage, total, total_pages: Math.max(1, Math.ceil(total / perPage)) }
      };
    } else {
      const params = new URLSearchParams({ page: page.toString(), per_page: perPage.toString() });
      if (eventId) params.append('event_id', eventId);
      const response = await apiClient.get(`/judgments?${params.toString()}`);
      return response.data;
    }
  }

  static async createJudgment(judgmentData: CreateJudgmentRequest): Promise<Judgment> {
    if (isTauri()) {
      const { invoke } = await import('@tauri-apps/api/core');
      const res = await invoke('submit_judgment_fast', {
        request: {
          eventId: judgmentData.event_id,
          assessment: judgmentData.assessment,
          confidenceLevel: judgmentData.confidence_level,
          reasoning: judgmentData.reasoning ?? null,
        }
      });
      return res as Judgment;
    } else {
      const response = await apiClient.post('/judgments', judgmentData);
      return response.data;
    }
  }

  // Consensus endpoints
  static async getConsensus(eventId: string): Promise<Consensus | null> {
    try {
      const response = await apiClient.get(`/consensus/${eventId}`);
      return response.data;
    } catch (error: any) {
      if (error.response?.status === 404) {
        return null;
      }
      throw error;
    }
  }

  static async calculateConsensus(eventId: string, request: CalculateConsensusRequest = {}): Promise<Consensus> {
    const response = await apiClient.post(`/consensus/${eventId}/calculate`, request);
    return response.data;
  }

  // Sync endpoints
  static async getSyncStatus(): Promise<SyncStatus> {
    if (isTauri()) {
      // Return mock sync status for desktop app
      return {
        is_online: true,
        last_sync: new Date().toISOString(),
        pending_operations: 0,
        sync_in_progress: false
      };
    } else {
      const response = await apiClient.get('/sync/status');
      return response.data;
    }
  }

  // Health check
  static async healthCheck(): Promise<boolean> {
    if (isTauri()) {
      try {
        const { invoke } = await import('@tauri-apps/api/core');
        const result = await invoke('health_check_core');
        return result !== null;
      } catch (error) {
        console.error('Tauri health check error:', error);
        return false;
      }
    } else {
      try {
        await apiClient.get('/health');
        return true;
      } catch {
        return false;
      }
    }
  }

  // Knowledge Base
  static async getKnowledgeBaseItems(): Promise<{ id: string; label: string }[]> {
    if (isTauri()) {
      const { invoke } = await import('@tauri-apps/api/core');
      const res = await invoke('knowledge_base_list');
      const { items } = res as { items: { id: string; label: string }[] };
      return items;
    }
    return [];
  }
}

// Export the axios instance for custom requests
export { apiClient };

// Export default service
export default ApiService;
