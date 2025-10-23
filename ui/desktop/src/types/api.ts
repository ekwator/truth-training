// General API types and utilities

export interface ApiResponse<T> {
  data: T;
  status: number;
  message?: string;
  timestamp?: string;
}

export interface ApiError {
  error: string;
  message: string;
  details?: Record<string, any>;
  code?: string;
  timestamp: string;
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

// HTTP Methods
export type HttpMethod = 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE';

// API Configuration
export interface ApiConfig {
  baseURL: string;
  timeout: number;
  retryAttempts: number;
  retryDelay: number;
}

// Request/Response interceptors
export interface RequestInterceptor {
  onFulfilled?: (config: any) => any;
  onRejected?: (error: any) => any;
}

export interface ResponseInterceptor {
  onFulfilled?: (response: any) => any;
  onRejected?: (error: any) => any;
}

// API Client options
export interface ApiClientOptions {
  baseURL?: string;
  timeout?: number;
  headers?: Record<string, string>;
  retryAttempts?: number;
  retryDelay?: number;
  enableLogging?: boolean;
}

// Request options
export interface RequestOptions {
  method: HttpMethod;
  url: string;
  data?: any;
  params?: Record<string, any>;
  headers?: Record<string, string>;
  timeout?: number;
}

// Response types
export interface SuccessResponse<T = any> {
  success: true;
  data: T;
  status: number;
  headers: Record<string, string>;
}

export interface ErrorResponse {
  success: false;
  error: ApiError;
  status: number;
  headers: Record<string, string>;
}

export type ApiResult<T = any> = SuccessResponse<T> | ErrorResponse;

// API Status
export interface ApiStatus {
  isOnline: boolean;
  lastSync: string | null;
  pendingOperations: number;
  syncInProgress: boolean;
  connectionQuality: 'excellent' | 'good' | 'poor' | 'offline';
}

// Retry configuration
export interface RetryConfig {
  maxAttempts: number;
  baseDelay: number;
  maxDelay: number;
  backoffMultiplier: number;
  retryCondition?: (error: any) => boolean;
}

// Cache configuration
export interface CacheConfig {
  enabled: boolean;
  ttl: number; // Time to live in milliseconds
  maxSize: number;
  storage: 'memory' | 'localStorage' | 'sessionStorage';
}

// Offline queue
export interface OfflineOperation {
  id: string;
  type: string;
  payload: any;
  timestamp: string;
  retryCount: number;
  maxRetries: number;
  priority: number;
}

export interface OfflineQueue {
  operations: OfflineOperation[];
  isProcessing: boolean;
  lastProcessed: string | null;
}

// API Health
export interface HealthCheck {
  status: 'healthy' | 'degraded' | 'unhealthy';
  timestamp: string;
  services: {
    database: 'up' | 'down';
    api: 'up' | 'down';
    cache: 'up' | 'down';
  };
  metrics: {
    responseTime: number;
    errorRate: number;
    throughput: number;
  };
}

// API Versioning
export interface ApiVersion {
  version: string;
  deprecated: boolean;
  sunsetDate?: string;
  migrationGuide?: string;
}

// API Rate Limiting
export interface RateLimit {
  limit: number;
  remaining: number;
  reset: number;
  retryAfter?: number;
}

// API Authentication
export interface AuthToken {
  accessToken: string;
  refreshToken?: string;
  expiresAt: string;
  tokenType: 'Bearer' | 'Basic';
}

// API Monitoring
export interface ApiMetrics {
  requestCount: number;
  errorCount: number;
  averageResponseTime: number;
  successRate: number;
  lastUpdated: string;
}
