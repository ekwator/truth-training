import axios, { AxiosInstance, AxiosResponse } from 'axios';
import { config } from '@/config/env';
import { AppConfig, ConnectionTestResult, DiscoverySettings, DiscoverRun, NodeRecord } from '@/types/api';
// Import shared domain types
import type { Event as TruthEvent, EventDetails, CreateEventRequest } from '@/types/events';
import type { Judgment, CreateJudgmentRequest } from '@/types/judgments';

export type { TruthEvent as Event, EventDetails, CreateEventRequest };
export type { Judgment, CreateJudgmentRequest };

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
  // Force web mode in tests for predictable mocking
  if (typeof process !== 'undefined' && (process.env.JEST_WORKER_ID || process.env.NODE_ENV === 'test')) {
    return false;
  }
  try {
    // Check for Tauri runtime objects
    if (typeof window === 'undefined') return false;
    const w = window as any;
    return Boolean(w.__TAURI__ || w.__TAURI_IPC__ || w.__TAURI_INTERNALS__);
  } catch {
    return false;
  }
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

// Impact API
export interface Impact {
  id: string;
  event_id: string | number;
  impact_level: number; // 1-5
  notes?: string;
  created_at: string;
}

export interface AddImpactRequest {
  event_id: string | number;
  impact_level: number;
  notes?: string;
}

// Summary API
export interface Summary {
  id: string;
  event_id: string | number;
  summary_text?: string;
  recommendations?: string;
  updated_at: string;
}

// Logs API
export interface LogItem {
  id: string;
  timestamp: string;
  source: string;
  level: string;
  message: string;
}

interface NodeFilterOptions {
  nodeType?: string;
  reachable?: boolean;
}

// Consensus API
export interface Consensus {
  event_id: string;
  consensus_value: 'confirm' | 'reject' | 'abstain' | null;
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
  static async getEvents(page: number = 1, perPage: number = 20): Promise<PaginatedResponse<TruthEvent>> {
    if (isTauri()) {
      try {
        const { invoke } = await import('@tauri-apps/api/core');
        const res = await invoke('list_events_fast', { page, perPage });
        const { data, total } = res as { data: TruthEvent[]; total: number };
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
        throw error;
      }
    } else {
      // Use HTTP API for web development
      const response = await apiClient.get(`/events?page=${page}&per_page=${perPage}`);
      return response.data;
    }
  }

  static async getEvent(id: number): Promise<TruthEvent> {
    if (isTauri()) {
      try {
        const { invoke } = await import('@tauri-apps/api/core');
        const event = await invoke('get_event_fast', { eventId: id });
        return event as TruthEvent;
      } catch (error) {
        console.error('Tauri getEvent error:', error);
        throw new Error('Failed to fetch event from desktop backend');
      }
    } else {
      const response = await apiClient.get(`/events/${id}`);
      return response.data;
    }
  }

  static async createEvent(eventData: CreateEventRequest): Promise<TruthEvent> {
    if (isTauri()) {
      try {
        const { invoke } = await import('@tauri-apps/api/core');
        const event = await invoke('create_event_fast', { request: eventData });
        return event as TruthEvent;
      } catch (error) {
        console.error('Tauri createEvent error:', error);
        throw new Error('Failed to create event in desktop backend');
      }
    } else {
      // Map UI request to core API: use embedded context fields
      const payload = {
        description: eventData.description,
        category_id: eventData.category_id,
        forma_id: eventData.forma_id,
        cause_id: eventData.cause_id,
        develop_id: eventData.develop_id,
        effect_id: eventData.effect_id,
        vector: eventData.vector
      };
      const response = await apiClient.post('/events', payload);
      return response.data as TruthEvent;
    }
  }

  // Judgments endpoints
  static async getJudgments(eventId?: number, page: number = 1, perPage: number = 20): Promise<PaginatedResponse<Judgment>> {
    if (isTauri()) {
      const { invoke } = await import('@tauri-apps/api/core');
      const res = await invoke('judgments_list_fast', { eventId: eventId ? eventId.toString() : '', page, perPage });
      const { data, total } = res as { data: any[]; total: number };
      const normalized = data.map((item) => ({
        ...item,
        event_id: typeof item.event_id === 'string' ? Number(item.event_id) : item.event_id,
      })) as Judgment[];
      return {
        data: normalized,
        pagination: { page, per_page: perPage, total, total_pages: Math.max(1, Math.ceil(total / perPage)) }
      };
    } else {
      const params = new URLSearchParams({ page: page.toString(), per_page: perPage.toString() });
      if (eventId !== undefined) params.append('event_id', eventId.toString());
      const response = await apiClient.get(`/judgments?${params.toString()}`);
      return response.data;
    }
  }

  static async createJudgment(judgmentData: CreateJudgmentRequest): Promise<Judgment> {
    if (isTauri()) {
      const { invoke } = await import('@tauri-apps/api/core');
      const res = await invoke('submit_judgment_fast', {
        request: {
          eventId: judgmentData.event_id.toString(),
          assessment: judgmentData.assessment,
          confidenceLevel: judgmentData.confidence_level,
          reasoning: judgmentData.reasoning ?? null,
        }
      }) as any;
      return {
        ...res,
        event_id: typeof res.event_id === 'string' ? Number(res.event_id) : res.event_id,
      } as Judgment;
    } else {
      const response = await apiClient.post('/judgments', judgmentData);
      return response.data;
    }
  }

  // Consensus endpoints
  static async getConsensus(eventId: number): Promise<Consensus | null> {
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

  static async calculateConsensus(eventId: number, request: CalculateConsensusRequest = {}): Promise<Consensus> {
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

  // Impact endpoints
  static async addImpact(impactData: AddImpactRequest): Promise<Impact> {
    if (isTauri()) {
      try {
        const { invoke } = await import('@tauri-apps/api/core');
        const impact = await invoke('add_impact', { request: impactData });
        return impact as Impact;
      } catch (error) {
        console.error('Tauri addImpact error:', error);
        throw new Error('Failed to add impact in desktop backend');
      }
    } else {
      const response = await apiClient.post('/impacts', impactData);
      return response.data;
    }
  }

  // Logs endpoints
  static async getLogs(page: number = 1): Promise<{ items: LogItem[]; page: number; total: number }> {
    if (isTauri()) {
      try {
        const { invoke } = await import('@tauri-apps/api/core');
        const result = await invoke('list_logs', { page });
        return result as { items: LogItem[]; page: number; total: number };
      } catch (error) {
        console.error('Tauri getLogs error:', error);
        throw new Error('Failed to fetch logs from desktop backend');
      }
    } else {
      const response = await apiClient.get(`/logs?page=${page}`);
      return response.data;
    }
  }

  static async clearLogs(): Promise<void> {
    if (isTauri()) {
      try {
        const { invoke } = await import('@tauri-apps/api/core');
        await invoke('clear_logs');
      } catch (error) {
        console.error('Tauri clearLogs error:', error);
        throw new Error('Failed to clear logs in desktop backend');
      }
    } else {
      await apiClient.delete('/logs');
    }
  }

  // Summary endpoints
  static async getOverallMetrics(): Promise<{ total_events: number; average_impact_level: number; last_updated?: string }> {
    if (isTauri()) {
      try {
        const { invoke } = await import('@tauri-apps/api/core');
        const result = await invoke('get_overall_metrics');
        return result as { total_events: number; average_impact_level: number; last_updated?: string };
      } catch (error) {
        console.error('Tauri getOverallMetrics error:', error);
        throw new Error('Failed to fetch overall metrics from desktop backend');
      }
    } else {
      const response = await apiClient.get('/summary/metrics');
      return response.data;
    }
  }

  static async getEventRows(): Promise<{ event: string; summary: string; impact?: number; date: string }[]> {
    if (isTauri()) {
      try {
        const { invoke } = await import('@tauri-apps/api/core');
        const result = await invoke('list_event_rows');
        return result as { event: string; summary: string; impact?: number; date: string }[];
      } catch (error) {
        console.error('Tauri getEventRows error:', error);
        throw new Error('Failed to fetch event rows from desktop backend');
      }
    } else {
      const response = await apiClient.get('/summary/events');
      return response.data;
    }
  }

  static async exportOverallSummary(): Promise<string> {
    if (isTauri()) {
      try {
        const { invoke } = await import('@tauri-apps/api/core');
        const result = await invoke('export_overall_summary_txt');
        return result as string;
      } catch (error) {
        console.error('Tauri exportOverallSummary error:', error);
        throw new Error('Failed to export overall summary from desktop backend');
      }
    } else {
      const response = await apiClient.get('/summary/export');
      return response.data;
    }
  }

  // Knowledge Base
  static async getKnowledgeBaseItems(): Promise<{ id: string; label: string }[]> {
    if (isTauri()) {
      try {
        const { invoke } = await import('@tauri-apps/api/core');
        const res = await invoke('knowledge_base_list');
        const { items } = res as { items: { id: string; label: string }[] };
        return items;
      } catch (error) {
        console.error('Tauri getKnowledgeBaseItems error:', error);
        throw new Error('Failed to fetch knowledge base items');
      }
    } else {
      // For web development, return mock data
      return [
        { id: 'kb:context_a', label: 'Context A' },
        { id: 'kb:context_b', label: 'Context B' },
        { id: 'kb:context_c', label: 'Context C' }
      ];
    }
  }

  // Configuration Management
  static async getAppConfig(): Promise<AppConfig> {
    if (isTauri()) {
      try {
        const { invoke } = await import('@tauri-apps/api/core');
        const config = await invoke('get_app_config');
        return config as AppConfig;
      } catch (error) {
        console.error('Tauri getAppConfig error:', error);
        throw new Error('Failed to get app configuration');
      }
    } else {
      // For web development, return default config
      const raw = localStorage.getItem('truth_training_config');
      if (raw) {
        try { return JSON.parse(raw) as AppConfig; } catch {}
      }
      return { mode: 'http', server_ip: '127.0.0.1', server_port: 8080, nearby_sync: false, nearby_interval_ms: 3000 };
    }
  }

  static async saveAppConfig(config: AppConfig): Promise<void> {
    if (isTauri()) {
      try {
        const { invoke } = await import('@tauri-apps/api/core');
        await invoke('save_app_config', { config });
      } catch (error) {
        console.error('Tauri saveAppConfig error:', error);
        throw new Error('Failed to save app configuration');
      }
    } else {
      // For web development, save to localStorage
      localStorage.setItem('truth_training_config', JSON.stringify(config));
    }
  }

  static async startNearbySync(intervalMs: number): Promise<void> {
    if (isTauri()) {
      // TODO: bridge command in desktop backend if needed
      return;
    } else {
      await apiClient.post('/api/v1/nearby_sync/start', { interval_ms: intervalMs });
    }
  }

  static async stopNearbySync(): Promise<void> {
    if (isTauri()) {
      return;
    } else {
      await apiClient.post('/api/v1/nearby_sync/stop');
    }
  }

  static async testCoreConnection(): Promise<ConnectionTestResult> {
    if (isTauri()) {
      try {
        const { invoke } = await import('@tauri-apps/api/core');
        const result = await invoke('core_status');
        return result as ConnectionTestResult;
      } catch (error) {
        console.error('Tauri testCoreConnection error:', error);
        throw new Error('Failed to test core connection');
      }
    } else {
      // For web development, simulate success
      return {
        ok: true,
        message: 'Core connection simulated (web mode)'
      };
    }
  }

  static async testHttpConnection(ip: string, port: number): Promise<ConnectionTestResult> {
    if (isTauri()) {
      try {
        const { invoke } = await import('@tauri-apps/api/core');
        const result = await invoke('test_http_connection', { ip, port });
        return result as ConnectionTestResult;
      } catch (error) {
        console.error('Tauri testHttpConnection error:', error);
        throw new Error('Failed to test HTTP connection');
      }
    } else {
      // For web development, simulate HTTP test
      try {
        const response = await fetch(`http://${ip}:${port}/status`);
        return {
          ok: response.ok,
          message: response.ok ? 'HTTP connection successful' : `HTTP server responded with status: ${response.status}`
        };
      } catch (error) {
        return {
          ok: false,
          message: `HTTP connection failed: ${error}`
        };
      }
    }
  }

  static async initApp(): Promise<ConnectionTestResult> {
    if (isTauri()) {
      try {
        const { invoke } = await import('@tauri-apps/api/core');
        const result = await invoke('init_app');
        return result as ConnectionTestResult;
      } catch (error) {
        console.error('Tauri initApp error:', error);
        throw new Error('Failed to initialize application');
      }
    } else {
      // Simulate init in web mode
      return { ok: true, message: 'Initialized (web mode simulation)' };
    }
  }

  // Discovery / Nodes helpers
  static async listNodes(filter: NodeFilterOptions = {}): Promise<NodeRecord[]> {
    if (isTauri()) {
      const { invoke } = await import('@tauri-apps/api/core');
      const nodes = await invoke('list_nodes', {
        nodeType: filter.nodeType ?? null,
        reachable: filter.reachable ?? null
      });
      return nodes as NodeRecord[];
    }
    return [];
  }

  static async manualDiscover(types: string[] = ['LAN', 'WIFI', 'GLOBAL']): Promise<DiscoverRun> {
    if (isTauri()) {
      const { invoke } = await import('@tauri-apps/api/core');
      const result = await invoke('manual_discover', { nodeTypes: types });
      return result as DiscoverRun;
    }
    return { discovered: 0, updated: 0, duration_ms: 0 };
  }

  static async cleanupNodes(): Promise<number> {
    if (isTauri()) {
      const { invoke } = await import('@tauri-apps/api/core');
      const removed = await invoke('cleanup_nodes');
      return removed as number;
    }
    return 0;
  }

  static async runNodesHealthCheck(): Promise<number> {
    if (isTauri()) {
      const { invoke } = await import('@tauri-apps/api/core');
      const checked = await invoke('run_nodes_health_check');
      return checked as number;
    }
    return 0;
  }

  static async getDiscoverySettings(): Promise<DiscoverySettings> {
    if (isTauri()) {
      const { invoke } = await import('@tauri-apps/api/core');
      const settings = await invoke('get_discovery_settings');
      return settings as DiscoverySettings;
    }
    return {
      enable_background: false,
      lan_interval_secs: 30,
      wifi_interval_secs: 45,
      global_interval_secs: 3600,
      cleanup_interval_secs: 60,
      lan_ttl_secs: 120,
      wifi_ttl_secs: 300,
      global_ttl_secs: 3600,
      registry_urls: [],
      db_path: '/'
    };
  }

  static async saveDiscoverySettings(settings: DiscoverySettings): Promise<void> {
    if (isTauri()) {
      const { invoke } = await import('@tauri-apps/api/core');
      await invoke('save_discovery_settings_cmd', { settings });
    }
  }

  // Context template endpoints
  static async getContexts(): Promise<import('@/types/contexts').ContextListResponse> {
    if (isTauri()) {
      const { invoke } = await import('@tauri-apps/api/core');
      return await invoke('list_contexts');
    } else {
      const response = await apiClient.get('/contexts');
      return response.data;
    }
  }

  static async getContextByName(name: string): Promise<import('@/types/contexts').ContextTemplate> {
    if (isTauri()) {
      const { invoke } = await import('@tauri-apps/api/core');
      return await invoke('get_context_by_name', { name });
    } else {
      const response = await apiClient.get(`/contexts/by-name/${encodeURIComponent(name)}`);
      return response.data;
    }
  }

  static async createContext(request: import('@/types/contexts').CreateContextRequest): Promise<import('@/types/contexts').ContextTemplate> {
    if (isTauri()) {
      const { invoke } = await import('@tauri-apps/api/core');
      return await invoke('create_context', { request });
    } else {
      const response = await apiClient.post('/contexts', request);
      return response.data;
    }
  }

  static async matchContext(request: import('@/types/contexts').MatchContextRequest): Promise<import('@/types/contexts').MatchContextResponse> {
    if (isTauri()) {
      const { invoke } = await import('@tauri-apps/api/core');
      return await invoke('match_context', { request });
    } else {
      const response = await apiClient.post('/contexts/match', request);
      return response.data;
    }
  }

  static async createContextFromEvent(request: import('@/types/contexts').CreateContextFromEventRequest): Promise<import('@/types/contexts').ContextTemplate> {
    if (isTauri()) {
      const { invoke } = await import('@tauri-apps/api/core');
      return await invoke('create_context_from_event', { request });
    } else {
      const response = await apiClient.post('/contexts/from-event', request);
      return response.data;
    }
  }
}

// Export the axios instance for custom requests
export { apiClient };

// Export default service
export default ApiService;
