import { OfflineOperation, OfflineQueue, RetryConfig } from '@/types/api';

// Default retry configuration
const DEFAULT_RETRY_CONFIG: RetryConfig = {
  maxAttempts: 3,
  baseDelay: 1000, // 1 second
  maxDelay: 30000, // 30 seconds
  backoffMultiplier: 2,
  retryCondition: (error: any) => {
    // Retry on network errors, 5xx server errors, and rate limiting
    return (
      !error.response || // Network error
      error.response.status >= 500 || // Server error
      error.response.status === 429 // Rate limited
    );
  }
};

// Storage key for offline queue
const STORAGE_KEY = 'truth_offline_queue';

export class OfflineQueueService {
  private queue: OfflineQueue;
  private retryConfig: RetryConfig;
  private isProcessing: boolean = false;

  constructor(retryConfig: Partial<RetryConfig> = {}) {
    this.retryConfig = { ...DEFAULT_RETRY_CONFIG, ...retryConfig };
    this.queue = this.loadFromStorage();
  }

  // Load queue from localStorage
  private loadFromStorage(): OfflineQueue {
    try {
      const stored = localStorage.getItem(STORAGE_KEY);
      if (stored) {
        const parsed = JSON.parse(stored);
        return {
          operations: parsed.operations || [],
          isProcessing: false, // Always reset processing state
          lastProcessed: parsed.lastProcessed || null
        };
      }
    } catch (error) {
      console.error('Failed to load offline queue from storage:', error);
    }
    
    return {
      operations: [],
      isProcessing: false,
      lastProcessed: null
    };
  }

  // Save queue to localStorage
  private saveToStorage(): void {
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify({
        operations: this.queue.operations,
        lastProcessed: this.queue.lastProcessed
      }));
    } catch (error) {
      console.error('Failed to save offline queue to storage:', error);
    }
  }

  // Add operation to queue
  addOperation(operation: Omit<OfflineOperation, 'id' | 'timestamp' | 'retryCount'>): string {
    const newOperation: OfflineOperation = {
      id: this.generateOperationId(),
      timestamp: new Date().toISOString(),
      retryCount: 0,
      ...operation,
      // ensure defaults when not provided
      maxRetries: operation.maxRetries ?? this.retryConfig.maxAttempts,
      priority: operation.priority ?? 1
    };

    this.queue.operations.push(newOperation);
    this.saveToStorage();
    
    console.log(`Added operation to queue: ${newOperation.type} (${newOperation.id})`);
    return newOperation.id;
  }

  // Remove operation from queue
  removeOperation(operationId: string): boolean {
    const initialLength = this.queue.operations.length;
    this.queue.operations = this.queue.operations.filter(op => op.id !== operationId);
    
    if (this.queue.operations.length < initialLength) {
      this.saveToStorage();
      console.log(`Removed operation from queue: ${operationId}`);
      return true;
    }
    
    return false;
  }

  // Get all operations
  getOperations(): OfflineOperation[] {
    return [...this.queue.operations];
  }

  // Get operations by type
  getOperationsByType(type: string): OfflineOperation[] {
    return this.queue.operations.filter(op => op.type === type);
  }

  // Get queue status
  getQueueStatus(): OfflineQueue {
    return {
      operations: [...this.queue.operations],
      isProcessing: this.isProcessing,
      lastProcessed: this.queue.lastProcessed
    };
  }

  // Clear all operations
  clearQueue(): void {
    this.queue.operations = [];
    this.queue.lastProcessed = null;
    this.saveToStorage();
    console.log('Cleared offline queue');
  }

  // Process queue with retry logic
  async processQueue(apiCall: (operation: OfflineOperation) => Promise<any>): Promise<void> {
    if (this.isProcessing || this.queue.operations.length === 0) {
      return;
    }

    this.isProcessing = true;
    this.queue.isProcessing = true;

    try {
      // Sort operations by priority (higher priority first)
      const sortedOperations = [...this.queue.operations].sort((a, b) => b.priority - a.priority);
      const processedOperations: string[] = [];
      const failedOperations: OfflineOperation[] = [];

      for (const operation of sortedOperations) {
        try {
          await this.executeWithRetry(operation, apiCall);
          processedOperations.push(operation.id);
          console.log(`Successfully processed operation: ${operation.type} (${operation.id})`);
        } catch (error) {
          console.error(`Failed to process operation: ${operation.type} (${operation.id})`, error);
          
          if (operation.retryCount >= operation.maxRetries) {
            console.log(`Operation exceeded max retries, removing: ${operation.id}`);
            processedOperations.push(operation.id);
          } else {
            failedOperations.push(operation);
          }
        }
      }

      // Update queue state
      this.queue.operations = failedOperations;
      this.queue.lastProcessed = new Date().toISOString();
      this.saveToStorage();

      console.log(`Processed ${processedOperations.length} operations, ${failedOperations.length} failed`);
    } finally {
      this.isProcessing = false;
      this.queue.isProcessing = false;
    }
  }

  // Execute operation with retry logic
  private async executeWithRetry(
    operation: OfflineOperation,
    apiCall: (operation: OfflineOperation) => Promise<any>
  ): Promise<any> {
    let lastError: any;
    
    for (let attempt = 0; attempt <= this.retryConfig.maxAttempts; attempt++) {
      try {
        const result = await apiCall(operation);
        
        // Update retry count on success
        if (attempt > 0) {
          operation.retryCount = attempt;
        }
        
        return result;
      } catch (error: any) {
        lastError = error;
        
        // Check if we should retry
        if (attempt < this.retryConfig.maxAttempts && this.shouldRetry(error)) {
          const delay = this.calculateDelay(attempt);
          console.log(`Retrying operation ${operation.id} in ${delay}ms (attempt ${attempt + 1}/${this.retryConfig.maxAttempts})`);
          await this.sleep(delay);
        } else {
          throw error;
        }
      }
    }
    
    throw lastError;
  }

  // Check if error should trigger a retry
  private shouldRetry(error: any): boolean {
    if (this.retryConfig.retryCondition) {
      return this.retryConfig.retryCondition(error);
    }
    return true;
  }

  // Calculate delay for retry (exponential backoff)
  private calculateDelay(attempt: number): number {
    const delay = this.retryConfig.baseDelay * Math.pow(this.retryConfig.backoffMultiplier, attempt);
    return Math.min(delay, this.retryConfig.maxDelay);
  }

  // Sleep utility
  private sleep(ms: number): Promise<void> {
    return new Promise(resolve => setTimeout(resolve, ms));
  }

  // Generate unique operation ID
  private generateOperationId(): string {
    return `op_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
  }

  // Get queue statistics
  getStats(): {
    totalOperations: number;
    operationsByType: Record<string, number>;
    oldestOperation: string | null;
    retryStats: {
      totalRetries: number;
      averageRetries: number;
    };
  } {
    const operations = this.queue.operations;
    const operationsByType: Record<string, number> = {};
    let totalRetries = 0;
    let oldestTimestamp: string | null = null;

    operations.forEach(op => {
      operationsByType[op.type] = (operationsByType[op.type] || 0) + 1;
      totalRetries += op.retryCount;
      
      if (!oldestTimestamp || op.timestamp < oldestTimestamp) {
        oldestTimestamp = op.timestamp;
      }
    });

    return {
      totalOperations: operations.length,
      operationsByType,
      oldestOperation: oldestTimestamp,
      retryStats: {
        totalRetries,
        averageRetries: operations.length > 0 ? totalRetries / operations.length : 0
      }
    };
  }
}

// Export singleton instance
export const offlineQueueService = new OfflineQueueService();
