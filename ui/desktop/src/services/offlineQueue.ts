// Offline queue service for handling operations when offline
// Implements Local-wins conflict resolution strategy

export interface QueuedOperation {
  id: string;
  type: 'create_event' | 'add_impact' | 'submit_judgment' | 'update_event';
  data: any;
  timestamp: number;
  retryCount: number;
  maxRetries: number;
}

export interface SyncStatus {
  isOnline: boolean;
  lastSync: string | null;
  pendingOperations: number;
  syncInProgress: boolean;
}

class OfflineQueueService {
  private queue: QueuedOperation[] = [];
  private syncInProgress = false;
  private listeners: ((status: SyncStatus) => void)[] = [];

  constructor() {
    this.loadQueue();
    this.setupOnlineListener();
  }

  private loadQueue() {
    try {
      const stored = localStorage.getItem('tt_offline_queue');
      if (stored) {
        this.queue = JSON.parse(stored);
      }
    } catch (error) {
      console.error('Failed to load offline queue:', error);
      this.queue = [];
    }
  }

  private saveQueue() {
    try {
      localStorage.setItem('tt_offline_queue', JSON.stringify(this.queue));
    } catch (error) {
      console.error('Failed to save offline queue:', error);
    }
  }

  private setupOnlineListener() {
    window.addEventListener('online', () => {
      this.syncPendingOperations();
    });
  }

  addOperation(operation: Omit<QueuedOperation, 'id' | 'timestamp' | 'retryCount'>) {
    const queuedOp: QueuedOperation = {
      ...operation,
      id: crypto.randomUUID(),
      timestamp: Date.now(),
      retryCount: 0,
      maxRetries: operation.maxRetries || 3
    };

    this.queue.push(queuedOp);
    this.saveQueue();
    this.notifyListeners();
  }

  async syncPendingOperations() {
    if (this.syncInProgress || this.queue.length === 0) {
      return;
    }

    this.syncInProgress = true;
    this.notifyListeners();

    const operations = [...this.queue];
    const successful: string[] = [];
    const failed: QueuedOperation[] = [];

    for (const operation of operations) {
      try {
        await this.executeOperation(operation);
        successful.push(operation.id);
      } catch (error) {
        console.error(`Failed to sync operation ${operation.id}:`, error);
        operation.retryCount++;
        
        if (operation.retryCount < operation.maxRetries) {
          failed.push(operation);
        } else {
          console.error(`Operation ${operation.id} exceeded max retries, removing from queue`);
        }
      }
    }

    // Remove successful operations and update failed ones
    this.queue = failed;
    this.saveQueue();
    this.syncInProgress = false;
    this.notifyListeners();
  }

  private async executeOperation(operation: QueuedOperation) {
    const { ApiService } = await import('./api');

    switch (operation.type) {
      case 'create_event':
        await ApiService.createEvent(operation.data);
        break;
      case 'add_impact':
        await ApiService.addImpact(operation.data);
        break;
      case 'submit_judgment':
        await ApiService.createJudgment(operation.data);
        break;
      case 'update_event':
        // Note: updateEvent not implemented in API service yet
        throw new Error('Update event not implemented');
      default:
        throw new Error(`Unknown operation type: ${operation.type}`);
    }
  }

  getStatus(): SyncStatus {
    return {
      isOnline: navigator.onLine,
      lastSync: this.getLastSyncTime(),
      pendingOperations: this.queue.length,
      syncInProgress: this.syncInProgress
    };
  }

  private getLastSyncTime(): string | null {
    try {
      return localStorage.getItem('tt_last_sync');
    } catch {
      return null;
    }
  }


  subscribe(listener: (status: SyncStatus) => void) {
    this.listeners.push(listener);
    return () => {
      this.listeners = this.listeners.filter(l => l !== listener);
    };
  }

  private notifyListeners() {
    const status = this.getStatus();
    this.listeners.forEach(listener => listener(status));
  }

  // Clear all pending operations (for testing or manual reset)
  clearQueue() {
    this.queue = [];
    this.saveQueue();
    this.notifyListeners();
  }

  // Get pending operations (for debugging)
  getPendingOperations(): QueuedOperation[] {
    return [...this.queue];
  }
}

export const offlineQueue = new OfflineQueueService();
export default offlineQueue;
