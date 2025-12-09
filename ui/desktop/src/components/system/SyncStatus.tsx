import React from 'react';
import { getEmoji } from '@/utils/emojiMapping';

interface SyncStatusProps {
  isOnline: boolean;
  pendingOperations: number;
  lastSync: string | null;
}

export const SyncStatus: React.FC<SyncStatusProps> = ({ isOnline, pendingOperations, lastSync }) => {
  const getStatusColor = () => {
    if (!isOnline) return 'text-red-600 bg-red-100';
    if (pendingOperations > 0) return 'text-yellow-600 bg-yellow-100';
    return 'text-green-600 bg-green-100';
  };

  const getStatusText = () => {
    if (!isOnline) return `${getEmoji('status', 'offline')} Offline`;
    if (pendingOperations > 0) return `${getEmoji('status', 'syncing')} ${pendingOperations} pending`;
    return `${getEmoji('status', 'online')} Synced`;
  };


  return (
    <div className={`inline-flex items-center px-3 py-1 rounded-full text-sm font-medium ${getStatusColor()}`}>
      <span className="ml-0">{getStatusText()}</span>
      {lastSync && (
        <span className="ml-2 text-xs opacity-75">
          {new Date(lastSync).toLocaleTimeString()}
        </span>
      )}
    </div>
  );
};
