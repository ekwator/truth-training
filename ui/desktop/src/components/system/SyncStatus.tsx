import React from 'react';

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
    if (!isOnline) return 'Offline';
    if (pendingOperations > 0) return `${pendingOperations} pending`;
    return 'Synced';
  };

  const getStatusIcon = () => {
    if (!isOnline) {
      return (
        <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M18.364 5.636l-12.728 12.728m0-12.728l12.728 12.728" />
        </svg>
      );
    }
    if (pendingOperations > 0) {
      return (
        <svg className="w-4 h-4 animate-spin" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
        </svg>
      );
    }
    return (
      <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
      </svg>
    );
  };

  return (
    <div className={`inline-flex items-center px-3 py-1 rounded-full text-sm font-medium ${getStatusColor()}`}>
      {getStatusIcon()}
      <span className="ml-2">{getStatusText()}</span>
      {lastSync && (
        <span className="ml-2 text-xs opacity-75">
          {new Date(lastSync).toLocaleTimeString()}
        </span>
      )}
    </div>
  );
};
