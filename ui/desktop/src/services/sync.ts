import { ApiService } from './api';
import { offlineQueueService } from './offline';
import { OfflineOperation } from '@/types/api';

export interface SyncConfig {
  autoSync: boolean;
  syncInterval: number; // milliseconds
  maxRetries: number;
  retryDelay: number;
  batchSize: number;
}

export interface SyncResult {
  success: boolean;
  processedOperations: number;
  failedOperations: number;
  errors: string[];
  duration: number;
}

export class SyncService {
  private config: SyncConfig;
  private syncInterval: NodeJS.Timeout | null = null;
  private isOnline: boolean = navigator.onLine;
  private lastSyncTime: string | null = null;

  constructor(config: Partial<SyncConfig> = {}) {
    this.config = {
      autoSync: true,
      syncInterval: 30000, // 30 seconds
      maxRetries: 3,
      retryDelay: 1000,
      batchSize: 10,
      ...config
    };

    this.setupEventListeners();
  }

  // Setup event listeners for online/offline status
  private setupEventListeners(): void {
    if (typeof window !== 'undefined') {
      window.addEventListener('online', () => {
        this.setOnlineStatus(true);
        if (this.config.autoSync) {
          this.startAutoSync();
        }
      });

      window.addEventListener('offline', () => {
        this.setOnlineStatus(false);
        this.stopAutoSync();
      });
    }
  }

  // Set online status and trigger sync if needed
  private setOnlineStatus(isOnline: boolean): void {
    this.isOnline = isOnline;
    console.log(`Connection status changed: ${isOnline ? 'online' : 'offline'}`);
    
    if (isOnline && this.config.autoSync) {
      this.sync();
    }
  }

  // Get current sync status
  async getSyncStatus(): Promise<{
    is_online: boolean;
    last_sync: string | null;
    pending_operations: number;
    sync_in_progress: boolean;
  }> {
    try {
      const status = await ApiService.getSyncStatus();
      this.lastSyncTime = status.last_sync;
      return status;
    } catch (error) {
      console.error('Failed to get sync status:', error);
      return {
        is_online: this.isOnline,
        last_sync: this.lastSyncTime,
        pending_operations: offlineQueueService.getOperations().length,
        sync_in_progress: false
      };
    }
  }

  // Perform sync operation
  async sync(): Promise<SyncResult> {
    if (!this.isOnline) {
      console.log('Cannot sync: offline');
      return {
        success: false,
        processedOperations: 0,
        failedOperations: 0,
        errors: ['Cannot sync while offline'],
        duration: 0
      };
    }

    const startTime = Date.now();
    const operations = offlineQueueService.getOperations();
    
    if (operations.length === 0) {
      console.log('No operations to sync');
      return {
        success: true,
        processedOperations: 0,
        failedOperations: 0,
        errors: [],
        duration: Date.now() - startTime
      };
    }

    console.log(`Starting sync of ${operations.length} operations`);

    try {
      await offlineQueueService.processQueue(this.executeOperation.bind(this));
      
      const duration = Date.now() - startTime;
      const remainingOperations = offlineQueueService.getOperations();
      
      console.log(`Sync completed in ${duration}ms. ${operations.length - remainingOperations.length} operations processed, ${remainingOperations.length} remaining`);
      
      return {
        success: true,
        processedOperations: operations.length - remainingOperations.length,
        failedOperations: remainingOperations.length,
        errors: [],
        duration
      };
    } catch (error: any) {
      const duration = Date.now() - startTime;
      console.error('Sync failed:', error);
      
      return {
        success: false,
        processedOperations: 0,
        failedOperations: operations.length,
        errors: [error.message || 'Unknown sync error'],
        duration
      };
    }
  }

  // Execute individual operation
  private async executeOperation(operation: OfflineOperation): Promise<any> {
    console.log(`Executing operation: ${operation.type} (${operation.id})`);
    
    switch (operation.type) {
      case 'create_event':
        return await ApiService.createEvent(operation.payload);
      
      case 'submit_judgment':
        return await ApiService.createJudgment(operation.payload);
      
      case 'calculate_consensus':
        return await ApiService.calculateConsensus(
          operation.payload.eventId,
          operation.payload.request
        );
      
      default:
        throw new Error(`Unknown operation type: ${operation.type}`);
    }
  }

  // Start automatic sync
  startAutoSync(): void {
    if (this.syncInterval) {
      return; // Already running
    }

    console.log('Starting auto-sync');
    this.syncInterval = setInterval(() => {
      if (this.isOnline) {
        this.sync();
      }
    }, this.config.syncInterval);
  }

  // Stop automatic sync
  stopAutoSync(): void {
    if (this.syncInterval) {
      clearInterval(this.syncInterval);
      this.syncInterval = null;
      console.log('Stopped auto-sync');
    }
  }

  // Force sync (bypass auto-sync settings)
  async forceSync(): Promise<SyncResult> {
    console.log('Force sync requested');
    return await this.sync();
  }

  // Add operation to offline queue
  addOperation(type: string, payload: any, priority: number = 1): string {
    return offlineQueueService.addOperation({
      type,
      payload,
      priority,
      maxRetries: this.config.maxRetries
    });
  }

  // Get offline queue status
  getQueueStatus() {
    return offlineQueueService.getQueueStatus();
  }

  // Get queue statistics
  getQueueStats() {
    return offlineQueueService.getStats();
  }

  // Clear offline queue
  clearQueue(): void {
    offlineQueueService.clearQueue();
    console.log('Cleared offline queue');
  }

  // Check if sync is needed
  needsSync(): boolean {
    return this.isOnline && offlineQueueService.getOperations().length > 0;
  }

  // Get sync configuration
  getConfig(): SyncConfig {
    return { ...this.config };
  }

  // Update sync configuration
  updateConfig(newConfig: Partial<SyncConfig>): void {
    this.config = { ...this.config, ...newConfig };
    
    // Restart auto-sync if interval changed
    if (this.syncInterval && newConfig.syncInterval) {
      this.stopAutoSync();
      if (this.config.autoSync) {
        this.startAutoSync();
      }
    }
  }

  // Health check
  async healthCheck(): Promise<boolean> {
    try {
      return await ApiService.healthCheck();
    } catch {
      return false;
    }
  }

  // Cleanup
  destroy(): void {
    this.stopAutoSync();
    console.log('Sync service destroyed');
  }
}

// Export singleton instance
export const syncService = new SyncService();

// Auto-start if online
if (navigator.onLine) {
  syncService.startAutoSync();
}
