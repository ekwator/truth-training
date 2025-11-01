// Event-related types based on data-model.md

export interface Event {
  id: string;
  title: string;
  description?: string;
  category_id?: number;
  forma_id?: number;
  cause_id?: number;
  develop_id?: number;
  effect_id?: number;
  start_date?: string;
  end_date?: string;
  created_at: string;
  updated_at?: string;
  status: EventStatus;
}

export type EventStatus = 'active' | 'inactive' | 'archived' | 'pending';

export interface EventDetails extends Event {
  consensus?: any | null;
  judgments: any[];
  impacts: any[];
  summary?: any;
  participant_count?: number;
  last_activity?: string;
}

export interface CreateEventRequest {
  title: string;
  description?: string;
  category_id?: number;
  forma_id?: number;
  cause_id?: number;
  develop_id?: number;
  effect_id?: number;
  start_date?: string;
  end_date?: string;
}

export interface UpdateEventRequest {
  title?: string;
  description?: string;
  category?: string;
  tags?: string[];
  status?: EventStatus;
}

export interface EventFilters {
  status?: EventStatus;
  category?: string;
  tags?: string[];
  date_from?: string;
  date_to?: string;
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
export type EventSortField = 'created_at' | 'updated_at' | 'title' | 'status' | 'participant_count';
export type SortDirection = 'asc' | 'desc';

export interface EventSortOptions {
  field: EventSortField;
  direction: SortDirection;
}
