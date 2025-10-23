import { useState } from 'react';
import { useSyncStore } from '@/stores/sync';

export const Settings: React.FC = () => {
  const { syncStatus, isOnline, pendingOperations, clearQueue, startSync } = useSyncStore();
  const [apiBaseUrl, setApiBaseUrl] = useState(import.meta.env.VITE_API_BASE || 'http://localhost:8080/api/v1');
  const [autoSync, setAutoSync] = useState(true);
  const [syncInterval, setSyncInterval] = useState(30);

  const handleSaveSettings = () => {
    // In a real implementation, this would save to localStorage or a config file
    console.log('Settings saved:', { apiBaseUrl, autoSync, syncInterval });
  };

  const handleClearQueue = () => {
    if (window.confirm('Are you sure you want to clear the offline queue? This action cannot be undone.')) {
      clearQueue();
    }
  };

  const handleForceSync = () => {
    startSync();
  };

  return (
    <div className="min-h-screen bg-gray-50">
      {/* Header */}
      <header className="bg-white shadow-sm border-b">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between items-center py-4">
            <div>
              <h1 className="text-2xl font-bold text-gray-900">Settings</h1>
              <p className="text-sm text-gray-600">Configure application preferences</p>
            </div>
          </div>
        </div>
      </header>

      {/* Main Content */}
      <main className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <div className="space-y-8">
          {/* API Configuration */}
          <div className="bg-white shadow rounded-lg">
            <div className="px-6 py-4 border-b border-gray-200">
              <h2 className="text-lg font-medium text-gray-900">API Configuration</h2>
            </div>
            <div className="px-6 py-4 space-y-4">
              <div>
                <label htmlFor="api-base-url" className="block text-sm font-medium text-gray-700 mb-2">
                  API Base URL
                </label>
                <input
                  type="url"
                  id="api-base-url"
                  value={apiBaseUrl}
                  onChange={(e) => setApiBaseUrl(e.target.value)}
                  className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
                  placeholder="http://localhost:8080/api/v1"
                />
                <p className="mt-1 text-sm text-gray-500">
                  The base URL for the Truth Training API
                </p>
              </div>
            </div>
          </div>

          {/* Sync Configuration */}
          <div className="bg-white shadow rounded-lg">
            <div className="px-6 py-4 border-b border-gray-200">
              <h2 className="text-lg font-medium text-gray-900">Sync Configuration</h2>
            </div>
            <div className="px-6 py-4 space-y-4">
              <div className="flex items-center">
                <input
                  type="checkbox"
                  id="auto-sync"
                  checked={autoSync}
                  onChange={(e) => setAutoSync(e.target.checked)}
                  className="h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300 rounded"
                />
                <label htmlFor="auto-sync" className="ml-2 block text-sm text-gray-900">
                  Enable automatic sync
                </label>
              </div>
              
              <div>
                <label htmlFor="sync-interval" className="block text-sm font-medium text-gray-700 mb-2">
                  Sync Interval (seconds)
                </label>
                <input
                  type="number"
                  id="sync-interval"
                  value={syncInterval}
                  onChange={(e) => setSyncInterval(parseInt(e.target.value))}
                  min="10"
                  max="300"
                  className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
                />
                <p className="mt-1 text-sm text-gray-500">
                  How often to automatically sync offline operations
                </p>
              </div>
            </div>
          </div>

          {/* Sync Status */}
          <div className="bg-white shadow rounded-lg">
            <div className="px-6 py-4 border-b border-gray-200">
              <h2 className="text-lg font-medium text-gray-900">Sync Status</h2>
            </div>
            <div className="px-6 py-4 space-y-4">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div>
                  <p className="text-sm font-medium text-gray-500">Connection Status</p>
                  <p className={`text-lg font-semibold ${isOnline ? 'text-green-600' : 'text-red-600'}`}>
                    {isOnline ? 'Online' : 'Offline'}
                  </p>
                </div>
                <div>
                  <p className="text-sm font-medium text-gray-500">Pending Operations</p>
                  <p className="text-lg font-semibold text-gray-900">{pendingOperations}</p>
                </div>
                <div>
                  <p className="text-sm font-medium text-gray-500">Last Sync</p>
                  <p className="text-lg font-semibold text-gray-900">
                    {syncStatus?.last_sync ? new Date(syncStatus.last_sync).toLocaleString() : 'Never'}
                  </p>
                </div>
                <div>
                  <p className="text-sm font-medium text-gray-500">Sync in Progress</p>
                  <p className={`text-lg font-semibold ${syncStatus?.sync_in_progress ? 'text-yellow-600' : 'text-gray-600'}`}>
                    {syncStatus?.sync_in_progress ? 'Yes' : 'No'}
                  </p>
                </div>
              </div>
              
              <div className="flex space-x-4">
                <button
                  onClick={handleForceSync}
                  disabled={!isOnline || pendingOperations === 0}
                  className="px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 disabled:bg-gray-300 disabled:cursor-not-allowed"
                >
                  Force Sync
                </button>
                <button
                  onClick={handleClearQueue}
                  disabled={pendingOperations === 0}
                  className="px-4 py-2 bg-red-600 text-white rounded-md hover:bg-red-700 disabled:bg-gray-300 disabled:cursor-not-allowed"
                >
                  Clear Queue
                </button>
              </div>
            </div>
          </div>

          {/* Application Info */}
          <div className="bg-white shadow rounded-lg">
            <div className="px-6 py-4 border-b border-gray-200">
              <h2 className="text-lg font-medium text-gray-900">Application Information</h2>
            </div>
            <div className="px-6 py-4 space-y-2">
              <div className="flex justify-between">
                <span className="text-sm font-medium text-gray-500">Version</span>
                <span className="text-sm text-gray-900">0.1.0</span>
              </div>
              <div className="flex justify-between">
                <span className="text-sm font-medium text-gray-500">Build</span>
                <span className="text-sm text-gray-900">Development</span>
              </div>
              <div className="flex justify-between">
                <span className="text-sm font-medium text-gray-500">Platform</span>
                <span className="text-sm text-gray-900">Tauri Desktop</span>
              </div>
            </div>
          </div>

          {/* Save Button */}
          <div className="flex justify-end">
            <button
              onClick={handleSaveSettings}
              className="px-6 py-2 bg-green-600 text-white rounded-md hover:bg-green-700"
            >
              Save Settings
            </button>
          </div>
        </div>
      </main>
    </div>
  );
};
