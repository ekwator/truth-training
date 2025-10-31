import { useState, useEffect } from 'react';
import { useSyncStore } from '@/stores/sync';
import { ApiService } from '@/services/api';
import { AppConfig, ConnectionTestResult } from '@/types/api';

export const Settings: React.FC = () => {
  const { isOnline, pendingOperations } = useSyncStore();
  const [config, setConfig] = useState<AppConfig>({
    mode: 'core',
    server_ip: '127.0.0.1',
    server_port: 8080,
    nearby_sync: false,
    nearby_interval_ms: 3000
  });
  const [loading, setLoading] = useState(false);
  const [testResult, setTestResult] = useState<ConnectionTestResult | null>(null);
  const [lastTestTime, setLastTestTime] = useState<string | null>(null);
  const [errors, setErrors] = useState<Record<string, string>>({});

  useEffect(() => {
    loadConfig();
  }, []);

  const loadConfig = async () => {
    try {
      const loadedConfig = await ApiService.getAppConfig();
      setConfig(loadedConfig);
    } catch (error) {
      console.error('Failed to load config:', error);
    }
  };

  const validateConfig = (): boolean => {
    const newErrors: Record<string, string> = {};

    // Validate IP address
    const ipRegex = /^\d{1,3}(\.\d{1,3}){3}$/;
    if (!ipRegex.test(config.server_ip)) {
      newErrors.server_ip = 'Invalid IP address format';
    }

    // Validate port
    if (config.server_port < 1 || config.server_port > 65535) {
      newErrors.server_port = 'Port must be between 1 and 65535';
    }

    // Validate nearby interval
    if (config.nearby_interval_ms !== undefined && (config.nearby_interval_ms < 500 || config.nearby_interval_ms > 60000)) {
      newErrors.nearby_interval_ms = 'Interval must be between 500 and 60000 ms';
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSaveSettings = async () => {
    if (!validateConfig()) {
      return;
    }

    setLoading(true);
    try {
      await ApiService.saveAppConfig(config);
      // Apply nearby sync runtime change (HTTP mode only)
      if (config.nearby_sync) {
        await ApiService.startNearbySync(config.nearby_interval_ms || 3000);
      } else {
        await ApiService.stopNearbySync();
      }
      setTestResult({
        ok: true,
        message: 'Settings saved successfully'
      });
    } catch (error) {
      setTestResult({
        ok: false,
        message: `Failed to save settings: ${error}`
      });
    } finally {
      setLoading(false);
    }
  };

  const handleTestConnection = async () => {
    if (!validateConfig()) {
      return;
    }

    setLoading(true);
    setTestResult(null);

    try {
      let result: ConnectionTestResult;
      
      if (config.mode === 'core') {
        result = await ApiService.testCoreConnection();
      } else {
        result = await ApiService.testHttpConnection(config.server_ip, config.server_port);
      }

      setTestResult(result);
      setLastTestTime(new Date().toLocaleString());
    } catch (error) {
      setTestResult({
        ok: false,
        message: `Connection test failed: ${error}`
      });
      setLastTestTime(new Date().toLocaleString());
    } finally {
      setLoading(false);
    }
  };

  const handleModeChange = (mode: 'core' | 'http') => {
    setConfig(prev => ({ ...prev, mode }));
    setTestResult(null);
    setLastTestTime(null);
  };

  const handleBack = () => {
    // This would be handled by the parent component's navigation
    window.history.back();
  };

  const handleInit = async () => {
    setLoading(true);
    setTestResult(null);
    try {
      const result = await ApiService.initApp();
      setTestResult(result);
      await loadConfig();
    } catch (error) {
      setTestResult({ ok: false, message: `Init failed: ${error}` });
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-gray-50">
      {/* Header */}
      <header className="bg-white shadow-sm border-b">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between items-center py-4">
            <div>
              <h1 className="text-2xl font-bold text-gray-900">Truth Training — App Settings</h1>
              <p className="text-sm text-gray-600">Configure connection and application preferences</p>
            </div>
          </div>
        </div>
      </header>

      {/* Main Content */}
      <main className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <div className="space-y-8">
          {/* Connection Mode */}
          <div className="bg-white shadow rounded-lg">
            <div className="px-6 py-4 border-b border-gray-200">
              <h2 className="text-lg font-medium text-gray-900">Connection Mode:</h2>
            </div>
            <div className="px-6 py-4 space-y-4">
              <div className="space-y-2">
                <label className="flex items-center">
                  <input
                    type="radio"
                    name="connection-mode"
                    value="core"
                    checked={config.mode === 'core'}
                    onChange={() => handleModeChange('core')}
                    className="h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300"
                  />
                  <span className="ml-2 text-sm text-gray-900">Connect to Core (Local)</span>
                </label>
                <label className="flex items-center">
                  <input
                    type="radio"
                    name="connection-mode"
                    value="http"
                    checked={config.mode === 'http'}
                    onChange={() => handleModeChange('http')}
                    className="h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300"
                  />
                  <span className="ml-2 text-sm text-gray-900">Connect via HTTP API</span>
                </label>
              </div>
            </div>
          </div>

          {/* Server Configuration */}
          {config.mode === 'http' && (
            <div className="bg-white shadow rounded-lg">
              <div className="px-6 py-4 border-b border-gray-200">
                <h2 className="text-lg font-medium text-gray-900">Server Configuration:</h2>
              </div>
              <div className="px-6 py-4 space-y-4">
                <div>
                  <label htmlFor="server-ip" className="block text-sm font-medium text-gray-700 mb-1">
                    Server IP Address:
                  </label>
                  <input
                    type="text"
                    id="server-ip"
                    value={config.server_ip}
                    onChange={(e) => setConfig(prev => ({ ...prev, server_ip: e.target.value }))}
                    className={`w-full px-3 py-2 border rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500 ${
                      errors.server_ip ? 'border-red-300' : 'border-gray-300'
                    }`}
                    placeholder="127.0.0.1"
                  />
                  {errors.server_ip && (
                    <p className="mt-1 text-sm text-red-600">{errors.server_ip}</p>
                  )}
                </div>
                
                <div>
                  <label htmlFor="server-port" className="block text-sm font-medium text-gray-700 mb-1">
                    Port:
                  </label>
                  <input
                    type="number"
                    id="server-port"
                    value={config.server_port}
                    onChange={(e) => setConfig(prev => ({ ...prev, server_port: parseInt(e.target.value) || 0 }))}
                    min="1"
                    max="65535"
                    className={`w-full px-3 py-2 border rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500 ${
                      errors.server_port ? 'border-red-300' : 'border-gray-300'
                    }`}
                    placeholder="8080"
                  />
                  {errors.server_port && (
                    <p className="mt-1 text-sm text-red-600">{errors.server_port}</p>
                  )}
                </div>
              </div>
            </div>
          )}

          {/* Nearby Sync */}
          <div className="bg-white shadow rounded-lg">
            <div className="px-6 py-4 border-b border-gray-200">
              <h2 className="text-lg font-medium text-gray-900">Nearby Sync (LAN Broadcast):</h2>
            </div>
            <div className="px-6 py-4 space-y-4">
              <label className="inline-flex items-center space-x-2">
                <input
                  type="checkbox"
                  checked={!!config.nearby_sync}
                  onChange={(e) => setConfig(prev => ({ ...prev, nearby_sync: e.target.checked }))}
                />
                <span className="text-sm">Enable nearby sync</span>
              </label>

              <div className="sm:w-64">
                <label htmlFor="nearby-interval" className="block text-sm font-medium text-gray-700 mb-1">
                  Broadcast interval (ms)
                </label>
                <input
                  id="nearby-interval"
                  type="number"
                  min={500}
                  max={60000}
                  value={config.nearby_interval_ms || 3000}
                  onChange={(e) => setConfig(prev => ({ ...prev, nearby_interval_ms: parseInt(e.target.value) || 3000 }))}
                  className={`w-full px-3 py-2 border rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500 ${
                    errors.nearby_interval_ms ? 'border-red-300' : 'border-gray-300'
                  }`}
                />
                {errors.nearby_interval_ms && (
                  <p className="mt-1 text-sm text-red-600">{errors.nearby_interval_ms}</p>
                )}
              </div>
              <p className="text-xs text-gray-500">When enabled, the node uses UDP broadcast discovery and HTTP sync with peers on the local network.</p>
            </div>
          </div>

          {/* Connection Test Result */}
          {testResult && (
            <div className="bg-white shadow rounded-lg">
              <div className="px-6 py-4 border-b border-gray-200">
                <h2 className="text-lg font-medium text-gray-900">Connection Test Result:</h2>
              </div>
              <div className="px-6 py-4">
                <div className={`flex items-center ${testResult.ok ? 'text-green-600' : 'text-red-600'}`}>
                  <span className="text-lg mr-2">{testResult.ok ? '✅' : '❌'}</span>
                  <span className="text-sm">{testResult.message}</span>
                </div>
                {lastTestTime && (
                  <p className="mt-2 text-xs text-gray-500">Last test: {lastTestTime}</p>
                )}
              </div>
            </div>
          )}

          {/* Current Status */}
          <div className="bg-white shadow rounded-lg">
            <div className="px-6 py-4 border-b border-gray-200">
              <h2 className="text-lg font-medium text-gray-900">Current Status:</h2>
            </div>
            <div className="px-6 py-4 space-y-2">
              <div className="flex justify-between">
                <span className="text-sm font-medium text-gray-500">Current Mode:</span>
                <span className="text-sm text-gray-900 capitalize">{config.mode}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-sm font-medium text-gray-500">Connection Status:</span>
                <span className={`text-sm font-semibold ${isOnline ? 'text-green-600' : 'text-red-600'}`}>
                  {isOnline ? 'Online' : 'Offline'}
                </span>
              </div>
              <div className="flex justify-between">
                <span className="text-sm font-medium text-gray-500">Pending Operations:</span>
                <span className="text-sm text-gray-900">{pendingOperations}</span>
              </div>
              {lastTestTime && (
                <div className="flex justify-between">
                  <span className="text-sm font-medium text-gray-500">Last Test:</span>
                  <span className="text-sm text-gray-900">{lastTestTime}</span>
                </div>
              )}
            </div>
          </div>

          {/* Actions */}
          <div className="bg-white shadow rounded-lg">
            <div className="px-6 py-4 border-b border-gray-200">
              <h2 className="text-lg font-medium text-gray-900">Actions:</h2>
            </div>
            <div className="px-6 py-4">
              <div className="flex space-x-4">
                <button
                  onClick={handleSaveSettings}
                  disabled={loading}
                  className="px-4 py-2 bg-green-600 text-white rounded-md hover:bg-green-700 disabled:bg-gray-300 disabled:cursor-not-allowed"
                >
                  {loading ? 'Saving...' : 'Save Settings'}
                </button>
                <button
                  onClick={handleTestConnection}
                  disabled={loading}
                  className="px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 disabled:bg-gray-300 disabled:cursor-not-allowed"
                >
                  {loading ? 'Testing...' : 'Test Connection'}
                </button>
                <button
                  onClick={handleInit}
                  disabled={loading}
                  className="px-4 py-2 bg-red-600 text-white rounded-md hover:bg-red-700 disabled:bg-gray-300 disabled:cursor-not-allowed"
                >
                  {loading ? 'Initializing...' : 'Init'}
                </button>
                <button
                  onClick={handleBack}
                  className="px-4 py-2 bg-gray-600 text-white rounded-md hover:bg-gray-700"
                >
                  Back
                </button>
              </div>
            </div>
          </div>
        </div>
      </main>
    </div>
  );
};
