import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { t } from '@/i18n';

type OverallMetrics = {
  total_events: number;
  average_impact_level: number;
  last_updated: string | null;
};

type EventRow = {
  event: string;
  summary: string;
  impact: number | null;
  date: string;
};

function isTauri() {
  return typeof window !== 'undefined' && '__TAURI__' in window;
}

export const OverallSummary: React.FC = () => {
  const [metrics, setMetrics] = useState<OverallMetrics | null>(null);
  const [rows, setRows] = useState<EventRow[]>([]); // Reserved for future use
  const [loading, setLoading] = useState(false);
  void rows; // Suppress unused warning

  const refresh = useCallback(async () => {
    setLoading(true);
    try {
      if (isTauri()) {
        const { invoke } = await import('@tauri-apps/api/core');
        const m = (await invoke('get_overall_metrics')) as OverallMetrics;
        const r = (await invoke('list_event_rows')) as EventRow[];
        setMetrics(m);
        setRows(r);
      } else {
        setMetrics({ total_events: 0, average_impact_level: 0, last_updated: null });
        setRows([]);
      }
    } finally {
      setLoading(false);
    }
  }, []);

  const avgText = useMemo(() => {
    if (!metrics) return '-';
    return (Math.round(metrics.average_impact_level * 10) / 10).toFixed(1);
  }, [metrics]);

  useEffect(() => {
    refresh();
  }, [refresh]);

  const exportTxt = useCallback(async () => {
    if (isTauri()) {
      const { invoke } = await import('@tauri-apps/api/core');
      const p = (await invoke('export_overall_summary_txt')) as string;
      alert(`Exported to: ${p}`);
    }
  }, []);

  return (
    <div className="min-h-screen bg-gray-50">
      {/* Top App Bar */}
      <header className="bg-white shadow-sm border-b">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between items-center py-4">
            <h1 className="text-2xl font-bold text-gray-900">{t('summary.title')}</h1>
            <button
              onClick={refresh}
              disabled={loading}
              className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:opacity-50"
            >
              {t('summary.refresh')}
            </button>
          </div>
        </div>
      </header>

      {/* Main Content */}
      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {/* Metrics Card */}
        <div className="bg-white shadow rounded-lg mb-6">
          <div className="px-6 py-4 border-b border-gray-200">
            <h2 className="text-lg font-medium text-gray-900">{t('summary.metrics')}</h2>
          </div>
          <div className="px-6 py-4">
            {loading ? (
              <div className="flex items-center justify-center py-8">
                <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
              </div>
            ) : metrics ? (
              <div className="space-y-2 text-sm">
                <div>• {t('summary.totalEvents')}: {metrics.total_events}</div>
                <div>• {t('summary.detectedEvents')}: -</div>
                <div>• {t('summary.eventsWithConsensus')}: -</div>
                <div>• {t('summary.averageCollectiveScore')}: {avgText}</div>
                <div>• {t('summary.lastUpdated')}: {metrics.last_updated ?? t('summary.never')}</div>
              </div>
            ) : (
              <div className="text-sm text-gray-600">{t('common.loading')}</div>
            )}
          </div>
        </div>

        {/* Network Statistics */}
        <div className="bg-white shadow rounded-lg mb-6">
          <div className="px-6 py-4 border-b border-gray-200">
            <h2 className="text-lg font-medium text-gray-900">{t('summary.networkStatistics')}</h2>
          </div>
          <div className="px-6 py-4">
            <div className="space-y-2 text-sm">
              <div>• {t('summary.nodeCount')}: -</div>
              <div>• {t('summary.activeConnections')}: -</div>
              <div>• {t('summary.syncStatus')}: -</div>
            </div>
          </div>
        </div>

        {/* Export Button */}
        <div className="bg-white shadow rounded-lg">
          <div className="px-6 py-4">
            <button
              onClick={exportTxt}
              disabled={loading}
              className="px-4 py-2 bg-gray-600 text-white rounded-lg hover:bg-gray-700 disabled:opacity-50"
            >
              {t('summary.exportReport')}
            </button>
          </div>
        </div>
      </main>
    </div>
  );
};


