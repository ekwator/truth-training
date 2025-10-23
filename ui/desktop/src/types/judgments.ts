// Judgment-related types based on data-model.md

export interface Judgment {
  id: string;
  participant_id: string;
  event_id: string;
  assessment: JudgmentAssessment;
  confidence_level: number;
  reasoning?: string;
  submitted_at: string;
  signature: string;
  weight?: number;
}

export type JudgmentAssessment = 'true' | 'false' | 'uncertain';

export interface CreateJudgmentRequest {
  event_id: string;
  assessment: JudgmentAssessment;
  confidence_level: number;
  reasoning?: string;
  signature: string;
}

export interface UpdateJudgmentRequest {
  assessment?: JudgmentAssessment;
  confidence_level?: number;
  reasoning?: string;
  signature?: string;
}

export interface JudgmentFilters {
  event_id?: string;
  participant_id?: string;
  assessment?: JudgmentAssessment;
  confidence_min?: number;
  confidence_max?: number;
  date_from?: string;
  date_to?: string;
}

export interface JudgmentListResponse {
  judgments: Judgment[];
  pagination: PaginationMeta;
  filters?: JudgmentFilters;
}

export interface PaginationMeta {
  page: number;
  per_page: number;
  total: number;
  total_pages: number;
}

// Judgment statistics
export interface JudgmentStats {
  total_judgments: number;
  judgments_by_assessment: {
    true: number;
    false: number;
    uncertain: number;
  };
  average_confidence: number;
  recent_judgments_count: number;
}

// Judgment validation
export interface JudgmentValidation {
  assessment: {
    allowed_values: JudgmentAssessment[];
  };
  confidence_level: {
    min: number;
    max: number;
  };
  reasoning: {
    max_length: number;
  };
  signature: {
    required: boolean;
    format: string;
  };
}

// Judgment sorting options
export type JudgmentSortField = 'submitted_at' | 'confidence_level' | 'assessment' | 'participant_id';
export type SortDirection = 'asc' | 'desc';

export interface JudgmentSortOptions {
  field: JudgmentSortField;
  direction: SortDirection;
}

// Judgment analysis
export interface JudgmentAnalysis {
  consensus_trend: 'increasing' | 'decreasing' | 'stable';
  confidence_distribution: {
    high: number;    // > 0.8
    medium: number;  // 0.5 - 0.8
    low: number;     // < 0.5
  };
  participant_diversity: number; // unique participants
  assessment_breakdown: {
    true: number;
    false: number;
    uncertain: number;
  };
}

// Judgment quality metrics
export interface JudgmentQuality {
  accuracy_score?: number;
  consistency_score?: number;
  reliability_score?: number;
  last_updated?: string;
}
