import { useState, useEffect } from 'react';
import { useSyncStore } from '@/stores/sync';
import { useEventsStore } from '@/stores/events';
import { ApiService } from '@/services/api';
import { AppConfig, ConnectionTestResult, DiscoverySettings } from '@/types/api';
import { useToast } from '@/components/system/Toaster';
// import { LocaleToggle } from '@/components/layout/LocaleToggle'; // Not used - using explicit buttons instead
import { getCurrentLocale, setLocale } from '@/i18n';
import { t } from '@/i18n';

export const Settings: React.FC = () => {
  const { isOnline, pendingOperations } = useSyncStore();
  const { fetchEvents } = useEventsStore();
  const { addToast } = useToast();
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
  const [discoverySettings, setDiscoverySettings] = useState<DiscoverySettings | null>(null);
  const [discoveryLoading, setDiscoveryLoading] = useState(false);
  const [discoveryErrors, setDiscoveryErrors] = useState<Record<string, string>>({});
  const [currentLocale, setCurrentLocale] = useState<string>(getCurrentLocale());
  const [showLanguageConfirmation, setShowLanguageConfirmation] = useState(false);
  const [pendingLocale, setPendingLocale] = useState<string | null>(null);
  const [showClearEventsConfirmation, setShowClearEventsConfirmation] = useState(false);

  useEffect(() => {
    loadConfig();
    loadDiscoverySettings();
  }, []);

  const loadConfig = async () => {
    try {
      const loadedConfig = await ApiService.getAppConfig();
      setConfig(loadedConfig);
    } catch (error) {
      console.error('Failed to load config:', error);
    }
  };

  const loadDiscoverySettings = async () => {
    try {
      const settings = await ApiService.getDiscoverySettings();
      setDiscoverySettings(settings);
    } catch (error) {
      console.error('Failed to load discovery settings:', error);
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

  const validateDiscoverySettings = (): boolean => {
    if (!discoverySettings) return false;
    const next: Record<string, string> = {};
    if (discoverySettings.lan_interval_secs < 5) {
      next.lan_interval_secs = 'Must be at least 5 seconds';
    }
    if (discoverySettings.wifi_interval_secs < 5) {
      next.wifi_interval_secs = 'Must be at least 5 seconds';
    }
    if (discoverySettings.global_interval_secs < 30) {
      next.global_interval_secs = 'Should be at least 30 seconds';
    }
    if (discoverySettings.cleanup_interval_secs < 10) {
      next.cleanup_interval_secs = 'Must be at least 10 seconds';
    }
    if (discoverySettings.lan_ttl_secs < 60) {
      next.lan_ttl_secs = 'Must be >= 60';
    }
    if (discoverySettings.wifi_ttl_secs < 60) {
      next.wifi_ttl_secs = 'Must be >= 60';
    }
    if (discoverySettings.global_ttl_secs < 300) {
      next.global_ttl_secs = 'Must be >= 300';
    }
    setDiscoveryErrors(next);
    return Object.keys(next).length === 0;
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

  const handleSaveDiscoverySettings = async () => {
    if (!discoverySettings || !validateDiscoverySettings()) {
      return;
    }
    setDiscoveryLoading(true);
    try {
      await ApiService.saveDiscoverySettings(discoverySettings);
      addToast({
        type: 'success',
        title: 'Discovery settings saved',
        message: 'Background worker updated successfully',
      });
    } catch (error) {
      console.error('Failed to save discovery settings', error);
      addToast({
        type: 'error',
        title: 'Save failed',
        message: 'Unable to persist discovery settings',
      });
    } finally {
      setDiscoveryLoading(false);
    }
  };

  const updateDiscovery = <K extends keyof DiscoverySettings>(field: K, value: DiscoverySettings[K]) => {
    setDiscoverySettings(prev => (prev ? { ...prev, [field]: value } : prev));
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

  const handleLanguageSelect = (locale: string) => {
    if (locale === currentLocale) return;
    setPendingLocale(locale);
    setShowLanguageConfirmation(true);
  };

  const handleLanguageChangeConfirm = async () => {
    if (!pendingLocale) return;
    setLoading(true);
    
    // Performance logging: Start timing
    const startTime = performance.now();
    console.log(`[Performance] Language change started: ${pendingLocale} at ${new Date().toISOString()}`);
    
    try {
      // Step 1: Clear context templates
      const clearStart = performance.now();
      await ApiService.clearContextTemplates();
      const clearDuration = performance.now() - clearStart;
      console.log(`[Performance] Clear context templates: ${clearDuration.toFixed(2)}ms`);
      
      // Step 2: Change locale (this will trigger reseed_knowledge_base via setLocale)
      const localeStart = performance.now();
      await setLocale(pendingLocale, true);
      setCurrentLocale(pendingLocale);
      const localeDuration = performance.now() - localeStart;
      console.log(`[Performance] Set locale: ${localeDuration.toFixed(2)}ms`);
      
      // Step 3: Explicitly reseed knowledge base (setLocale may have already done this, but ensure it)
      const reseedStart = performance.now();
      await ApiService.reseedKnowledgeBase();
      const reseedDuration = performance.now() - reseedStart;
      console.log(`[Performance] Reseed knowledge base: ${reseedDuration.toFixed(2)}ms`);
      
      const totalDuration = performance.now() - startTime;
      console.log(`[Performance] Language change completed: ${totalDuration.toFixed(2)}ms (target: <5000ms)`);
      
      if (totalDuration > 5000) {
        console.warn(`[Performance] Language change exceeded 5 second target: ${totalDuration.toFixed(2)}ms`);
      }
      
      setShowLanguageConfirmation(false);
      setPendingLocale(null);
      addToast({
        type: 'success',
        title: t('settings.languageChanged'),
        message: t('settings.languageChangedMessage')
      });
      // Reload page to apply locale changes
      window.location.reload();
    } catch (error) {
      const totalDuration = performance.now() - startTime;
      console.error(`[Performance] Language change failed after ${totalDuration.toFixed(2)}ms:`, error);
      addToast({
        type: 'error',
        title: t('settings.errorChangingLanguage'),
        message: error instanceof Error ? error.message : t('settings.errorChangingLanguageMessage')
      });
    } finally {
      setLoading(false);
    }
  };

  const handleClearEvents = async () => {
    setLoading(true);
    try {
      // TODO: Implement clear_all_events Tauri command
      // For now, show a message
      addToast({
        type: 'info',
        title: t('settings.clearEvents'),
        message: t('settings.clearEventsNotImplemented')
      });
      setShowClearEventsConfirmation(false);
      // Refresh events list
      await fetchEvents();
    } catch (error) {
      addToast({
        type: 'error',
        title: t('settings.errorClearingEvents'),
        message: error instanceof Error ? error.message : t('settings.errorClearingEventsMessage')
      });
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

        {/* Discovery Worker Settings */}
        {discoverySettings && (
          <div className="bg-white shadow rounded-lg">
            <div className="px-6 py-4 border-b border-gray-200">
              <h2 className="text-lg font-medium text-gray-900">Discovery Worker Settings</h2>
              <p className="text-sm text-gray-500">Background discovery cadence and TTL overrides</p>
            </div>
            <div className="px-6 py-4 space-y-6">
              <label className="inline-flex items-center space-x-2">
                <input
                  type="checkbox"
                  checked={discoverySettings.enable_background}
                  onChange={(e) => updateDiscovery('enable_background', e.target.checked)}
                />
                <span className="text-sm">Enable background discovery worker</span>
              </label>

              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    LAN Interval (seconds)
                  </label>
                  <input
                    type="number"
                    min={5}
                    value={discoverySettings.lan_interval_secs}
                    onChange={(e) =>
                      updateDiscovery('lan_interval_secs', Number(e.target.value) || 5)
                    }
                    className={`w-full px-3 py-2 border rounded-md ${
                      discoveryErrors.lan_interval_secs ? 'border-red-300' : 'border-gray-300'
                    }`}
                  />
                  {discoveryErrors.lan_interval_secs && (
                    <p className="text-xs text-red-600">{discoveryErrors.lan_interval_secs}</p>
                  )}
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    Wi-Fi Interval (seconds)
                  </label>
                  <input
                    type="number"
                    min={5}
                    value={discoverySettings.wifi_interval_secs}
                    onChange={(e) =>
                      updateDiscovery('wifi_interval_secs', Number(e.target.value) || 5)
                    }
                    className={`w-full px-3 py-2 border rounded-md ${
                      discoveryErrors.wifi_interval_secs ? 'border-red-300' : 'border-gray-300'
                    }`}
                  />
                  {discoveryErrors.wifi_interval_secs && (
                    <p className="text-xs text-red-600">{discoveryErrors.wifi_interval_secs}</p>
                  )}
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    Global Poll Interval (seconds)
                  </label>
                  <input
                    type="number"
                    min={30}
                    value={discoverySettings.global_interval_secs}
                    onChange={(e) =>
                      updateDiscovery('global_interval_secs', Number(e.target.value) || 30)
                    }
                    className={`w-full px-3 py-2 border rounded-md ${
                      discoveryErrors.global_interval_secs ? 'border-red-300' : 'border-gray-300'
                    }`}
                  />
                  {discoveryErrors.global_interval_secs && (
                    <p className="text-xs text-red-600">{discoveryErrors.global_interval_secs}</p>
                  )}
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    Cleanup Interval (seconds)
                  </label>
                  <input
                    type="number"
                    min={10}
                    value={discoverySettings.cleanup_interval_secs}
                    onChange={(e) =>
                      updateDiscovery('cleanup_interval_secs', Number(e.target.value) || 10)
                    }
                    className={`w-full px-3 py-2 border rounded-md ${
                      discoveryErrors.cleanup_interval_secs ? 'border-red-300' : 'border-gray-300'
                    }`}
                  />
                  {discoveryErrors.cleanup_interval_secs && (
                    <p className="text-xs text-red-600">{discoveryErrors.cleanup_interval_secs}</p>
                  )}
                </div>
              </div>

              <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    LAN TTL (seconds)
                  </label>
                  <input
                    type="number"
                    min={60}
                    value={discoverySettings.lan_ttl_secs}
                    onChange={(e) =>
                      updateDiscovery('lan_ttl_secs', Number(e.target.value) || 60)
                    }
                    className={`w-full px-3 py-2 border rounded-md ${
                      discoveryErrors.lan_ttl_secs ? 'border-red-300' : 'border-gray-300'
                    }`}
                  />
                  {discoveryErrors.lan_ttl_secs && (
                    <p className="text-xs text-red-600">{discoveryErrors.lan_ttl_secs}</p>
                  )}
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    Wi-Fi TTL (seconds)
                  </label>
                  <input
                    type="number"
                    min={60}
                    value={discoverySettings.wifi_ttl_secs}
                    onChange={(e) =>
                      updateDiscovery('wifi_ttl_secs', Number(e.target.value) || 120)
                    }
                    className={`w-full px-3 py-2 border rounded-md ${
                      discoveryErrors.wifi_ttl_secs ? 'border-red-300' : 'border-gray-300'
                    }`}
                  />
                  {discoveryErrors.wifi_ttl_secs && (
                    <p className="text-xs text-red-600">{discoveryErrors.wifi_ttl_secs}</p>
                  )}
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    Global TTL (seconds)
                  </label>
                  <input
                    type="number"
                    min={300}
                    value={discoverySettings.global_ttl_secs}
                    onChange={(e) =>
                      updateDiscovery('global_ttl_secs', Number(e.target.value) || 3600)
                    }
                    className={`w-full px-3 py-2 border rounded-md ${
                      discoveryErrors.global_ttl_secs ? 'border-red-300' : 'border-gray-300'
                    }`}
                  />
                  {discoveryErrors.global_ttl_secs && (
                    <p className="text-xs text-red-600">{discoveryErrors.global_ttl_secs}</p>
                  )}
                </div>
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Registry URLs (one per line)
                </label>
                <textarea
                  rows={3}
                  value={discoverySettings.registry_urls.join('\n')}
                  onChange={(e) =>
                    updateDiscovery(
                      'registry_urls',
                      e.target.value
                        .split('\n')
                        .map((line) => line.trim())
                        .filter((line) => line.length > 0)
                    )
                  }
                  className="w-full px-3 py-2 border rounded-md border-gray-300"
                  placeholder="https://registry.example.com/api/v1/nodes"
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Nodes Database Path
                </label>
                <input
                  type="text"
                  value={discoverySettings.db_path}
                  onChange={(e) => updateDiscovery('db_path', e.target.value)}
                  className="w-full px-3 py-2 border rounded-md border-gray-300"
                />
                <p className="text-xs text-gray-500 mt-1">
                  The desktop worker writes discovery results to this SQLite database.
                </p>
              </div>

              <div className="flex justify-end">
                <button
                  onClick={handleSaveDiscoverySettings}
                  disabled={discoveryLoading}
                  className="px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 disabled:bg-gray-300"
                >
                  {discoveryLoading ? 'Saving...' : 'Save Discovery Settings'}
                </button>
              </div>
            </div>
          </div>
        )}

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

          {/* Language Settings */}
          <div className="bg-white shadow rounded-lg">
            <div className="px-6 py-4 border-b border-gray-200">
              <h2 className="text-lg font-medium text-gray-900">{t('settings.language')}</h2>
            </div>
            <div className="px-6 py-4 space-y-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  {t('settings.languageDescription')}
                </label>
                <div className="flex space-x-2">
                  <button
                    onClick={() => handleLanguageSelect('en')}
                    className={`px-4 py-2 rounded-lg text-sm font-medium transition-colors ${
                      currentLocale === 'en'
                        ? 'bg-blue-600 text-white'
                        : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
                    }`}
                  >
                    English
                  </button>
                  <button
                    onClick={() => handleLanguageSelect('ru')}
                    className={`px-4 py-2 rounded-lg text-sm font-medium transition-colors ${
                      currentLocale === 'ru'
                        ? 'bg-blue-600 text-white'
                        : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
                    }`}
                  >
                    Русский
                  </button>
                </div>
              </div>
            </div>
          </div>

          {/* Clear Events */}
          <div className="bg-white shadow rounded-lg">
            <div className="px-6 py-4 border-b border-gray-200">
              <h2 className="text-lg font-medium text-gray-900">{t('settings.actions')}</h2>
            </div>
            <div className="px-6 py-4">
              <button
                onClick={() => setShowClearEventsConfirmation(true)}
                disabled={loading}
                className="px-4 py-2 bg-red-600 text-white rounded-lg hover:bg-red-700 disabled:opacity-50"
              >
                {t('settings.clearEvents')}
              </button>
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

      {/* Language Change Confirmation Dialog */}
      {showLanguageConfirmation && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg p-6 max-w-md">
            <h3 className="text-lg font-semibold text-gray-900 mb-4">{t('settings.confirmLanguageChange')}</h3>
            <p className="text-sm text-gray-600 mb-4">{t('settings.confirmLanguageChangeMessage')}</p>
            <div className="flex space-x-2 justify-end">
              <button
                onClick={() => {
                  setShowLanguageConfirmation(false);
                  setPendingLocale(null);
                }}
                className="px-4 py-2 bg-gray-200 text-gray-700 rounded-lg hover:bg-gray-300"
              >
                {t('common.cancel')}
              </button>
              <button
                onClick={handleLanguageChangeConfirm}
                disabled={loading}
                className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:opacity-50"
              >
                {t('common.confirm')}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Clear Events Confirmation Dialog */}
      {showClearEventsConfirmation && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg p-6 max-w-md">
            <h3 className="text-lg font-semibold text-gray-900 mb-4">{t('settings.confirmClearEvents')}</h3>
            <p className="text-sm text-gray-600 mb-4">{t('settings.confirmClearEventsMessage')}</p>
            <div className="flex space-x-2 justify-end">
              <button
                onClick={() => setShowClearEventsConfirmation(false)}
                className="px-4 py-2 bg-gray-200 text-gray-700 rounded-lg hover:bg-gray-300"
              >
                {t('common.cancel')}
              </button>
              <button
                onClick={handleClearEvents}
                disabled={loading}
                className="px-4 py-2 bg-red-600 text-white rounded-lg hover:bg-red-700 disabled:opacity-50"
              >
                {t('common.confirm')}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
