import { create } from 'zustand';
import { devtools } from 'zustand/middleware';
import { OfflineOperation, OfflineQueue } from '@/types/api';
import { ApiService } from '@/services/api';

interface SyncState {
  // Sync status
  syncStatus: {
    is_online: boolean;
    last_sync: string | null;
    pending_operations: number;
    sync_in_progress: boolean;
  } | null;
  isOnline: boolean;
  lastSync: string | null;
  pendingOperations: number;
  syncInProgress: boolean;
  
  // Offline queue
  offlineQueue: OfflineQueue;
  
  // Loading and error states
  loading: boolean;
  error: string | null;
  
  // Actions
  fetchSyncStatus: () => Promise<void>;
  startSync: () => Promise<void>;
  stopSync: () => void;
  
  // Offline queue management
  addToQueue: (operation: Omit<OfflineOperation, 'id' | 'timestamp' | 'retryCount'>) => void;
  removeFromQueue: (operationId: string) => void;
  processQueue: () => Promise<void>;
  clearQueue: () => void;
  
  // Connection management
  setOnlineStatus: (isOnline: boolean) => void;
  checkConnection: () => Promise<boolean>;
  
  // Utility actions
  setLoading: (loading: boolean) => void;
  setError: (error: string | null) => void;
  clearError: () => void;
}

const generateOperationId = (): string => {
  return `op_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
};

export const useSyncStore = create<SyncState>()(
  devtools(
    (set, get) => ({
      // Initial state
      syncStatus: null,
      isOnline: navigator.onLine,
      lastSync: null,
      pendingOperations: 0,
      syncInProgress: false,
      offlineQueue: {
        operations: [],
        isProcessing: false,
        lastProcessed: null
      },
      loading: false,
      error: null,

      // Actions
      fetchSyncStatus: async () => {
        set({ loading: true, error: null });
        
        try {
          const syncStatus = await ApiService.getSyncStatus();
          set({
            syncStatus,
            isOnline: syncStatus.is_online,
            lastSync: syncStatus.last_sync,
            pendingOperations: syncStatus.pending_operations,
            syncInProgress: syncStatus.sync_in_progress,
            loading: false
          });
        } catch (error: any) {
          set({
            error: error.message || 'Failed to fetch sync status',
            loading: false,
            isOnline: false
          });
        }
      },

      startSync: async () => {
        const { isOnline, offlineQueue } = get();
        
        if (!isOnline || offlineQueue.operations.length === 0) {
          return;
        }
        
        set({ syncInProgress: true, loading: true });
        
        try {
          await get().processQueue();
          await get().fetchSyncStatus();
        } catch (error: any) {
          set({
            error: error.message || 'Sync failed',
            loading: false
          });
        } finally {
          set({ syncInProgress: false });
        }
      },

      stopSync: () => {
        set({ syncInProgress: false, loading: false });
      },

      // Offline queue management
      addToQueue: (operationData) => {
        const operation: OfflineOperation = {
          id: generateOperationId(),
          timestamp: new Date().toISOString(),
          retryCount: 0,
          ...operationData,
          maxRetries: operationData.maxRetries ?? 3,
          priority: operationData.priority ?? 1
        };
        
        set((state) => ({
          offlineQueue: {
            ...state.offlineQueue,
            operations: [...state.offlineQueue.operations, operation]
          },
          pendingOperations: state.pendingOperations + 1
        }));
      },

      removeFromQueue: (operationId: string) => {
        set((state) => ({
          offlineQueue: {
            ...state.offlineQueue,
            operations: state.offlineQueue.operations.filter(op => op.id !== operationId)
          },
          pendingOperations: Math.max(0, state.pendingOperations - 1)
        }));
      },

      processQueue: async () => {
        const { offlineQueue, isOnline } = get();
        
        if (!isOnline || offlineQueue.isProcessing) {
          return;
        }
        
        set((state) => ({
          offlineQueue: {
            ...state.offlineQueue,
            isProcessing: true
          }
        }));
        
        const operations = [...offlineQueue.operations].sort((a, b) => b.priority - a.priority);
        const processedOperations: string[] = [];
        
        for (const operation of operations) {
          try {
            // Process operation based on type
            switch (operation.type) {
              case 'create_event':
                await ApiService.createEvent(operation.payload);
                break;
              case 'submit_judgment':
                await ApiService.createJudgment(operation.payload);
                break;
              case 'calculate_consensus':
                await ApiService.calculateConsensus(operation.payload.eventId, operation.payload.request);
                break;
              default:
                console.warn(`Unknown operation type: ${operation.type}`);
                continue;
            }
            
            processedOperations.push(operation.id);
          } catch (error: any) {
            console.error(`Failed to process operation ${operation.id}:`, error);
            
            // Increment retry count
            if (operation.retryCount < operation.maxRetries) {
              set((state) => ({
                offlineQueue: {
                  ...state.offlineQueue,
                  operations: state.offlineQueue.operations.map(op =>
                    op.id === operation.id
                      ? { ...op, retryCount: op.retryCount + 1 }
                      : op
                  )
                }
              }));
            } else {
              // Remove operation after max retries
              processedOperations.push(operation.id);
            }
          }
        }
        
        // Remove processed operations
        set((state) => ({
          offlineQueue: {
            ...state.offlineQueue,
            operations: state.offlineQueue.operations.filter(op => !processedOperations.includes(op.id)),
            isProcessing: false,
            lastProcessed: new Date().toISOString()
          },
          pendingOperations: Math.max(0, state.pendingOperations - processedOperations.length)
        }));
      },

      clearQueue: () => {
        set({
          offlineQueue: {
            operations: [],
            isProcessing: false,
            lastProcessed: null
          },
          pendingOperations: 0
        });
      },

      // Connection management
      setOnlineStatus: (isOnline: boolean) => {
        set({ isOnline });
        
        // Auto-sync when coming back online
        if (isOnline) {
          get().startSync();
        }
      },

      checkConnection: async () => {
        try {
          const isHealthy = await ApiService.healthCheck();
          get().setOnlineStatus(isHealthy);
          return isHealthy;
        } catch {
          get().setOnlineStatus(false);
          return false;
        }
      },

      // Utility actions
      setLoading: (loading: boolean) => {
        set({ loading });
      },

      setError: (error: string | null) => {
        set({ error });
      },

      clearError: () => {
        set({ error: null });
      }
    }),
    {
      name: 'sync-store',
    }
  )
);

// Listen for online/offline events
if (typeof window !== 'undefined') {
  window.addEventListener('online', () => {
    useSyncStore.getState().setOnlineStatus(true);
  });
  
  window.addEventListener('offline', () => {
    useSyncStore.getState().setOnlineStatus(false);
  });
}
