import { create } from 'zustand';
import { devtools } from 'zustand/middleware';
import { ApiService } from '@/services/api';
import { offlineQueue, SyncStatus } from '@/services/offlineQueue';

interface SyncState {
  // Sync status
  syncStatus: SyncStatus | null;
  isOnline: boolean;
  lastSync: string | null;
  pendingOperations: number;
  syncInProgress: boolean;
  
  // Loading and error states
  loading: boolean;
  error: string | null;
  
  // Actions
  fetchSyncStatus: () => Promise<void>;
  startSync: () => Promise<void>;
  stopSync: () => void;
  
  // Connection management
  setOnlineStatus: (isOnline: boolean) => void;
  checkConnection: () => Promise<boolean>;
  
  // Utility actions
  setLoading: (loading: boolean) => void;
  setError: (error: string | null) => void;
  clearError: () => void;
}

export const useSyncStore = create<SyncState>()(
  devtools(
    (set, get) => ({
      // Initial state
      syncStatus: offlineQueue.getStatus(),
      isOnline: navigator.onLine,
      lastSync: null,
      pendingOperations: 0,
      syncInProgress: false,
      loading: false,
      error: null,

      // Actions
      fetchSyncStatus: async () => {
        set({ loading: true, error: null });
        
        try {
          const syncStatus = await ApiService.getSyncStatus();
          const offlineStatus = offlineQueue.getStatus();
          set({
            syncStatus: offlineStatus,
            isOnline: syncStatus.is_online,
            lastSync: syncStatus.last_sync,
            pendingOperations: offlineStatus.pendingOperations,
            syncInProgress: offlineStatus.syncInProgress,
            loading: false
          });
        } catch (error: any) {
          const offlineStatus = offlineQueue.getStatus();
          set({
            error: error.message || 'Failed to fetch sync status',
            loading: false,
            isOnline: false,
            syncStatus: offlineStatus,
            pendingOperations: offlineStatus.pendingOperations,
            syncInProgress: offlineStatus.syncInProgress
          });
        }
      },

      startSync: async () => {
        set({ syncInProgress: true, loading: true });
        
        try {
          await offlineQueue.syncPendingOperations();
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
