// Event-related types based on Data_Schema.md v1.0.0

export interface Event {
  id: number;  // INTEGER PRIMARY KEY AUTOINCREMENT
  description: string;  // TEXT NOT NULL
  category_id?: number;  // INTEGER FK → category.id, nullable
  forma_id?: number;  // INTEGER FK → forma.id, nullable
  cause_id?: number;  // INTEGER FK → cause.id, nullable
  develop_id?: number;  // INTEGER FK → develop.id, nullable
  effect_id?: number;  // INTEGER FK → effect.id, nullable
  vector: boolean;  // BOOLEAN - true = outgoing, false = incoming
  detected?: boolean;  // BOOLEAN nullable - whether event was identified as truth or lie
  corrected: boolean;  // BOOLEAN - event correction indicator
  timestamp_start: number;  // INTEGER (UNIX timestamp)
  timestamp_end?: number;  // INTEGER nullable (UNIX timestamp)
  code: number;  // INTEGER - event classification code (default: 1)
  collective_score?: number;  // REAL nullable - Collective truth score (0–1)
  // Display helpers (loaded via JOIN)
  category_name?: string;
  forma_name?: string;
  cause_name?: string;
  develop_name?: string;
  effect_name?: string;
}

export interface EventDetails extends Event {
  consensus?: any | null;
  judgments: any[];
  impacts: any[];
  summary?: any;
  participant_count?: number;
  last_activity?: string;
}

export interface CreateEventRequest {
  description: string;
  category_id?: number;
  forma_id?: number;
  cause_id?: number;
  develop_id?: number;
  effect_id?: number;
  vector: boolean;
}

export interface UpdateEventRequest {
  description?: string;
  category_id?: number;
  forma_id?: number;
  cause_id?: number;
  develop_id?: number;
  effect_id?: number;
  vector?: boolean;
  detected?: boolean;
  corrected?: boolean;
}

export interface EventFilters {
  category_id?: number;
  forma_id?: number;
  cause_id?: number;
  develop_id?: number;
  effect_id?: number;
  vector?: boolean;
  detected?: boolean;
  search?: string;
  recognition?: 'confirm' | 'reject' | 'abstain';
}

export interface EventListResponse {
  events: Event[];
  pagination: PaginationMeta;
  filters?: EventFilters;
}

export interface PaginationMeta {
  page: number;
  per_page: number;
  total: number;
  total_pages: number;
}

// Event statistics
export interface EventStats {
  total_events: number;
  active_events: number;
  events_with_consensus: number;
  average_judgments_per_event: number;
  recent_activity_count: number;
}

// Event creation validation
export interface EventValidation {
  title: {
    required: boolean;
    min_length: number;
    max_length: number;
  };
  description: {
    required: boolean;
    min_length: number;
    max_length: number;
  };
  category: {
    allowed_values: string[];
  };
}

// Event sorting options
export type EventSortField = 'timestamp_start' | 'timestamp_end' | 'description' | 'detected' | 'collective_score';
export type SortDirection = 'asc' | 'desc';

export interface EventSortOptions {
  field: EventSortField;
  direction: SortDirection;
}
