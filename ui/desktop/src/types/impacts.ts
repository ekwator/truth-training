/**
 * Impact-related types based on data-model.md
 */

/**
 * Impact entity representing a recorded impact for an event
 */
export interface Impact {
  id: string;
  event_id: string | number;
  impact_level: number;  // 1-5 (mapped from type_id for UI)
  notes?: string;
  created_at: string;    // ISO 8601 timestamp
}

/**
 * Request to add a new impact
 */
export interface AddImpactRequest {
  event_id: string | number;
  impact_level: number;  // 1-5
  notes?: string;
}

