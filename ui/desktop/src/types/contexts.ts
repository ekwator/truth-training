// Context template types based on API contracts

export interface ContextTemplate {
  id: number;
  name: string;
  category_id?: number;
  forma_id?: number;
  cause_id?: number;
  develop_id?: number;
  effect_id?: number;
  description?: string;
}

export interface CreateContextRequest {
  name: string;
  category_id?: number;
  forma_id?: number;
  cause_id?: number;
  develop_id?: number;
  effect_id?: number;
  description?: string;
}

export interface MatchContextRequest {
  category_id?: number;
  forma_id?: number;
  cause_id?: number;
  develop_id?: number;
  effect_id?: number;
}

export interface MatchContextResponse {
  matched: boolean;
  template: ContextTemplate | null;
}

export interface CreateContextFromEventRequest {
  name: string;
  event_id: number;
  description?: string;
}

export interface ContextListResponse {
  data: ContextTemplate[];
  total: number;
}

