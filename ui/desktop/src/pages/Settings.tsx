/**
 * Settings Screen
 * Matches Android Settings screen layout and behavior.
 * Implements connection mode toggle, server configuration, discovery settings, and app initialization.
 * Localization toggle removed (English-only interface).
 * Route: settings
 * Reference: spec/23-function_desktop.md lines 262-295
 */

import React, { useEffect, useState } from 'react';
import { ApiService } from '@/services/api';
import type { AppConfig, ConnectionTestResult, DiscoverySettings } from '@/types/api';
import { getEmoji } from '@/utils/emojiMapping';

export const Settings: React.FC = () => {
  const [config, setConfig] = useState<AppConfig>({
    mode: 'core',
    server_ip: '127.0.0.1',
    server_port: 8080,
    nearby_sync: false,
    nearby_interval_ms: 3000,
  });
  const [discoverySettings, setDiscoverySettings] = useState<DiscoverySettings>({
    enable_background: false,
    lan_interval_secs: 30,
    wifi_interval_secs: 45,
    global_interval_secs: 3600,
    cleanup_interval_secs: 60,
    lan_ttl_secs: 120,
    wifi_ttl_secs: 300,
    global_ttl_secs: 3600,
    registry_urls: [],
    db_path: '/',
  });
  const [connectionStatus, setConnectionStatus] = useState<ConnectionTestResult | null>(null);
  const [connectionStatusTime, setConnectionStatusTime] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [errors, setErrors] = useState<Record<string, string>>({});

  useEffect(() => {
    loadSettings();
  }, []);

  const loadSettings = async () => {
    setLoading(true);
    try {
      const [appConfig, discovery] = await Promise.all([
        ApiService.getAppConfig(),
        ApiService.getDiscoverySettings(),
      ]);
      setConfig(appConfig);
      setDiscoverySettings(discovery);
    } catch (error) {
      console.error('Failed to load settings:', error);
    } finally {
      setLoading(false);
    }
  };

  const validateIP = (ip: string): boolean => {
    const regex = /^\d{1,3}(\.\d{1,3}){3}$/;
    if (!regex.test(ip)) {
      return false;
    }
    const parts = ip.split('.').map(Number);
    return parts.every(part => part >= 0 && part <= 255);
  };

  const validatePort = (port: number): boolean => {
    return port >= 1 && port <= 65535;
  };

  const validateNearbyInterval = (interval: number): boolean => {
    return interval >= 500 && interval <= 60000;
  };

  const handleConfigChange = (field: keyof AppConfig, value: any) => {
    setConfig(prev => ({ ...prev, [field]: value }));
    // Clear error for this field
    if (errors[field]) {
      setErrors(prev => {
        const newErrors = { ...prev };
        delete newErrors[field];
        return newErrors;
      });
    }
  };

  const handleDiscoveryChange = (field: keyof DiscoverySettings, value: any) => {
    setDiscoverySettings(prev => ({ ...prev, [field]: value }));
  };

  const handleTestConnection = async () => {
    setLoading(true);
    try {
      let result: ConnectionTestResult;
      if (config.mode === 'core') {
        result = await ApiService.testCoreConnection();
      } else {
        result = await ApiService.testHttpConnection(config.server_ip, config.server_port);
      }
      setConnectionStatus(result);
      setConnectionStatusTime(new Date().toISOString());
    } catch (error) {
      setConnectionStatus({
        ok: false,
        message: `Connection test failed: ${error}`,
      });
      setConnectionStatusTime(new Date().toISOString());
    } finally {
      setLoading(false);
    }
  };

  const handleInitApp = async () => {
    if (!confirm('This will reset the application configuration and rebuild the database schema. Continue?')) {
      return;
    }

    setLoading(true);
    try {
      const result = await ApiService.initApp();
      if (result.ok) {
        alert('Application initialized successfully');
        await loadSettings();
      } else {
        alert(`Initialization failed: ${result.message}`);
      }
    } catch (error) {
      alert(`Initialization error: ${error}`);
    } finally {
      setLoading(false);
    }
  };

  const handleSaveConnection = async () => {
    // Validate inputs
    const newErrors: Record<string, string> = {};
    
    if (config.mode === 'http') {
      if (!validateIP(config.server_ip)) {
        newErrors.server_ip = 'Invalid IP address format';
      }
      if (!validatePort(config.server_port)) {
        newErrors.server_port = 'Port must be between 1 and 65535';
      }
    }

    if (config.nearby_sync && config.nearby_interval_ms) {
      if (!validateNearbyInterval(config.nearby_interval_ms)) {
        newErrors.nearby_interval_ms = 'Interval must be between 500 and 60000 ms';
      }
    }

    if (Object.keys(newErrors).length > 0) {
      setErrors(newErrors);
      return;
    }

    setSaving(true);
    try {
      await ApiService.saveAppConfig(config);
      alert('Connection settings saved successfully');
    } catch (error) {
      alert(`Failed to save connection settings: ${error}`);
    } finally {
      setSaving(false);
    }
  };

  const handleSaveDiscovery = async () => {
    setSaving(true);
    try {
      await ApiService.saveDiscoverySettings(discoverySettings);
      alert('Discovery settings saved successfully');
    } catch (error) {
      alert(`Failed to save discovery settings: ${error}`);
    } finally {
      setSaving(false);
    }
  };

  if (loading && !connectionStatus) {
    return (
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <div className="text-center py-8 text-gray-700 dark:text-gray-300">
          {getEmoji('status', 'syncing')} Loading settings...
        </div>
      </div>
    );
  }

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <h1 className="text-3xl font-bold text-gray-900 dark:text-gray-100 mb-6">
        {getEmoji('screens', 'settings')} Settings
      </h1>

      {/* Connection Mode Toggle */}
      <div className="bg-white dark:bg-gray-800 rounded-lg shadow dark:shadow-gray-700 p-6 mb-6">
        <h2 className="text-lg font-semibold text-gray-900 dark:text-gray-100 mb-4">
          {getEmoji('fields', 'category')} Connection Mode
        </h2>
        <div className="flex items-center space-x-4">
          <label className="flex items-center">
            <input
              type="radio"
              name="mode"
              value="core"
              checked={config.mode === 'core'}
              onChange={(e) => handleConfigChange('mode', e.target.value)}
              className="mr-2"
            />
            <span className="text-gray-700 dark:text-gray-300">Core (Local)</span>
          </label>
          <label className="flex items-center">
            <input
              type="radio"
              name="mode"
              value="http"
              checked={config.mode === 'http'}
              onChange={(e) => handleConfigChange('mode', e.target.value)}
              className="mr-2"
            />
            <span className="text-gray-700 dark:text-gray-300">HTTP API</span>
          </label>
        </div>
      </div>

      {/* Server Configuration */}
      {config.mode === 'http' && (
        <div className="bg-white dark:bg-gray-800 rounded-lg shadow dark:shadow-gray-700 p-6 mb-6">
          <h2 className="text-lg font-semibold text-gray-900 dark:text-gray-100 mb-4">
            {getEmoji('fields', 'name')} Server Configuration
          </h2>
          <div className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                IP Address
              </label>
              <input
                type="text"
                value={config.server_ip}
                onChange={(e) => handleConfigChange('server_ip', e.target.value)}
                className={`w-full px-3 py-2 border rounded-md dark:bg-gray-700 dark:text-gray-100 ${
                  errors.server_ip ? 'border-red-500' : 'border-gray-300 dark:border-gray-600'
                }`}
                placeholder="127.0.0.1"
              />
              {errors.server_ip && (
                <p className="mt-1 text-sm text-red-600 dark:text-red-400">{errors.server_ip}</p>
              )}
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                Port
              </label>
              <input
                type="number"
                value={config.server_port}
                onChange={(e) => handleConfigChange('server_port', parseInt(e.target.value) || 8080)}
                min="1"
                max="65535"
                className={`w-full px-3 py-2 border rounded-md dark:bg-gray-700 dark:text-gray-100 ${
                  errors.server_port ? 'border-red-500' : 'border-gray-300 dark:border-gray-600'
                }`}
                placeholder="8080"
              />
              {errors.server_port && (
                <p className="mt-1 text-sm text-red-600 dark:text-red-400">{errors.server_port}</p>
              )}
            </div>
          </div>
        </div>
      )}

      {/* Nearby Sync Toggle */}
      <div className="bg-white dark:bg-gray-800 rounded-lg shadow dark:shadow-gray-700 p-6 mb-6">
        <h2 className="text-lg font-semibold text-gray-900 dark:text-gray-100 mb-4">
          {getEmoji('actions', 'sync')} Nearby Sync
        </h2>
        <div className="space-y-4">
          <label className="flex items-center">
            <input
              type="checkbox"
              checked={config.nearby_sync || false}
              onChange={(e) => handleConfigChange('nearby_sync', e.target.checked)}
              className="mr-2"
            />
            <span className="text-gray-700 dark:text-gray-300">Enable UDP broadcast discovery</span>
          </label>
          {config.nearby_sync && (
            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                Interval (ms) - 500 to 60000
              </label>
              <input
                type="number"
                value={config.nearby_interval_ms || 3000}
                onChange={(e) => handleConfigChange('nearby_interval_ms', parseInt(e.target.value) || 3000)}
                min="500"
                max="60000"
                className={`w-full px-3 py-2 border rounded-md dark:bg-gray-700 dark:text-gray-100 ${
                  errors.nearby_interval_ms ? 'border-red-500' : 'border-gray-300 dark:border-gray-600'
                }`}
              />
              {errors.nearby_interval_ms && (
                <p className="mt-1 text-sm text-red-600 dark:text-red-400">{errors.nearby_interval_ms}</p>
              )}
            </div>
          )}
        </div>
      </div>

      {/* Discovery Worker Settings */}
      <div className="bg-white dark:bg-gray-800 rounded-lg shadow dark:shadow-gray-700 p-6 mb-6">
        <h2 className="text-lg font-semibold text-gray-900 dark:text-gray-100 mb-4">
          {getEmoji('status', 'online')} Discovery Worker Settings
        </h2>
        <div className="space-y-4">
          <label className="flex items-center">
            <input
              type="checkbox"
              checked={discoverySettings.enable_background}
              onChange={(e) => handleDiscoveryChange('enable_background', e.target.checked)}
              className="mr-2"
            />
            <span className="text-gray-700 dark:text-gray-300">Enable background discovery</span>
          </label>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                LAN Interval (secs)
              </label>
              <input
                type="number"
                value={discoverySettings.lan_interval_secs}
                onChange={(e) => handleDiscoveryChange('lan_interval_secs', parseInt(e.target.value) || 30)}
                className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-md dark:bg-gray-700 dark:text-gray-100"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                LAN TTL (secs)
              </label>
              <input
                type="number"
                value={discoverySettings.lan_ttl_secs}
                onChange={(e) => handleDiscoveryChange('lan_ttl_secs', parseInt(e.target.value) || 120)}
                className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-md dark:bg-gray-700 dark:text-gray-100"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                Wi-Fi Interval (secs)
              </label>
              <input
                type="number"
                value={discoverySettings.wifi_interval_secs}
                onChange={(e) => handleDiscoveryChange('wifi_interval_secs', parseInt(e.target.value) || 45)}
                className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-md dark:bg-gray-700 dark:text-gray-100"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                Wi-Fi TTL (secs)
              </label>
              <input
                type="number"
                value={discoverySettings.wifi_ttl_secs}
                onChange={(e) => handleDiscoveryChange('wifi_ttl_secs', parseInt(e.target.value) || 300)}
                className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-md dark:bg-gray-700 dark:text-gray-100"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                Global Interval (secs)
              </label>
              <input
                type="number"
                value={discoverySettings.global_interval_secs}
                onChange={(e) => handleDiscoveryChange('global_interval_secs', parseInt(e.target.value) || 3600)}
                className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-md dark:bg-gray-700 dark:text-gray-100"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                Global TTL (secs)
              </label>
              <input
                type="number"
                value={discoverySettings.global_ttl_secs}
                onChange={(e) => handleDiscoveryChange('global_ttl_secs', parseInt(e.target.value) || 3600)}
                className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-md dark:bg-gray-700 dark:text-gray-100"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                Cleanup Interval (secs)
              </label>
              <input
                type="number"
                value={discoverySettings.cleanup_interval_secs}
                onChange={(e) => handleDiscoveryChange('cleanup_interval_secs', parseInt(e.target.value) || 60)}
                className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-md dark:bg-gray-700 dark:text-gray-100"
              />
            </div>
          </div>
        </div>
      </div>

      {/* Test Connection Button */}
      <div className="bg-white dark:bg-gray-800 rounded-lg shadow dark:shadow-gray-700 p-6 mb-6">
        <h2 className="text-lg font-semibold text-gray-900 dark:text-gray-100 mb-4">
          {getEmoji('actions', 'refresh')} Connection Test
        </h2>
        <button
          onClick={handleTestConnection}
          disabled={loading}
          className="px-4 py-2 bg-blue-600 dark:bg-blue-500 text-white rounded hover:bg-blue-700 dark:hover:bg-blue-600 disabled:opacity-50 disabled:cursor-not-allowed"
        >
          {getEmoji('actions', 'refresh')} Test Connection
        </button>
      </div>

      {/* Init Button */}
      <div className="bg-white dark:bg-gray-800 rounded-lg shadow dark:shadow-gray-700 p-6 mb-6">
        <h2 className="text-lg font-semibold text-gray-900 dark:text-gray-100 mb-4">
          {getEmoji('actions', 'refresh')} Initialize App
        </h2>
        <p className="text-sm text-gray-600 dark:text-gray-400 mb-4">
          Reset configuration and rebuild database schema using canonical Truth schema from core library.
        </p>
        <button
          onClick={handleInitApp}
          disabled={loading}
          className="px-4 py-2 bg-orange-600 dark:bg-orange-500 text-white rounded hover:bg-orange-700 dark:hover:bg-orange-600 disabled:opacity-50 disabled:cursor-not-allowed"
        >
          {getEmoji('actions', 'refresh')} Initialize App
        </button>
      </div>

      {/* Connection Status Panel */}
      {connectionStatus && (
        <div className="bg-white dark:bg-gray-800 rounded-lg shadow dark:shadow-gray-700 p-6 mb-6">
          <h2 className="text-lg font-semibold text-gray-900 dark:text-gray-100 mb-4">
            {getEmoji('status', connectionStatus.ok ? 'success' : 'error')} Connection Status
          </h2>
          <div className="space-y-2">
            <div className={`flex items-center space-x-2 ${connectionStatus.ok ? 'text-green-600 dark:text-green-400' : 'text-red-600 dark:text-red-400'}`}>
              <span>{connectionStatus.ok ? getEmoji('status', 'success') : getEmoji('status', 'error')}</span>
              <span className="font-medium">{connectionStatus.ok ? 'Connected' : 'Connection Failed'}</span>
            </div>
            <p className="text-sm text-gray-600 dark:text-gray-400">{connectionStatus.message}</p>
            {connectionStatusTime && (
              <p className="text-xs text-gray-500 dark:text-gray-500">
                Tested at: {new Date(connectionStatusTime).toLocaleString()}
              </p>
            )}
          </div>
        </div>
      )}

      {/* Save Buttons */}
      <div className="flex space-x-4">
        <button
          onClick={handleSaveConnection}
          disabled={saving}
          className="px-6 py-2 bg-green-600 dark:bg-green-500 text-white rounded hover:bg-green-700 dark:hover:bg-green-600 disabled:opacity-50 disabled:cursor-not-allowed"
        >
          {getEmoji('actions', 'save')} Save Connection Settings
        </button>
        <button
          onClick={handleSaveDiscovery}
          disabled={saving}
          className="px-6 py-2 bg-green-600 dark:bg-green-500 text-white rounded hover:bg-green-700 dark:hover:bg-green-600 disabled:opacity-50 disabled:cursor-not-allowed"
        >
          {getEmoji('actions', 'save')} Save Discovery Settings
        </button>
      </div>
    </div>
  );
};
