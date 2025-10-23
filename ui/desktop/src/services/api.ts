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
    const response = await apiClient.get(`/events?page=${page}&per_page=${perPage}`);
    return response.data;
  }

  static async getEvent(id: string): Promise<EventDetails> {
    const response = await apiClient.get(`/events/${id}`);
    return response.data;
  }

  static async createEvent(eventData: CreateEventRequest): Promise<Event> {
    const response = await apiClient.post('/events', eventData);
    return response.data;
  }

  // Judgments endpoints
  static async getJudgments(eventId?: string, page: number = 1, perPage: number = 20): Promise<PaginatedResponse<Judgment>> {
    const params = new URLSearchParams({
      page: page.toString(),
      per_page: perPage.toString(),
    });
    
    if (eventId) {
      params.append('event_id', eventId);
    }

    const response = await apiClient.get(`/judgments?${params.toString()}`);
    return response.data;
  }

  static async createJudgment(judgmentData: CreateJudgmentRequest): Promise<Judgment> {
    const response = await apiClient.post('/judgments', judgmentData);
    return response.data;
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
    const response = await apiClient.get('/sync/status');
    return response.data;
  }

  // Health check
  static async healthCheck(): Promise<boolean> {
    try {
      await apiClient.get('/health');
      return true;
    } catch {
      return false;
    }
  }
}

// Export the axios instance for custom requests
export { apiClient };

// Export default service
export default ApiService;
