import { useState, useEffect } from 'react';
import { offlineQueue, SyncStatus } from '@/services/offlineQueue';

export const useSyncStatus = () => {
  const [status, setStatus] = useState<SyncStatus>(offlineQueue.getStatus());

  useEffect(() => {
    const unsubscribe = offlineQueue.subscribe(setStatus);
    return unsubscribe;
  }, []);

  return status;
};
