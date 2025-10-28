import React, { useCallback, useEffect, useMemo, useState } from 'react';

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
  const [rows, setRows] = useState<EventRow[]>([]);
  const [loading, setLoading] = useState(false);

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
    // TODO: hook export command when available
    // Placeholder: copy to clipboard-like text (Tauri save impl later)
    alert('Export to .txt will be implemented via Tauri command.');
  }, []);

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <div className="bg-white shadow rounded-lg">
        <div className="px-6 py-4 border-b border-gray-200 flex justify-between items-center">
          <h2 className="text-lg font-medium text-gray-900">Overall Summary of Training Sessions</h2>
          <div className="space-x-2">
            <button className="px-3 py-1 bg-gray-100 border rounded" onClick={refresh} disabled={loading}>Refresh Data</button>
            <button className="px-3 py-1 bg-gray-100 border rounded" onClick={exportTxt} disabled={loading}>Export Report (.txt)</button>
          </div>
        </div>
        <div className="px-6 py-4 text-sm">
          {metrics ? (
            <div className="space-y-1">
              <div>• Total Events: {metrics.total_events}</div>
              <div>• Average Impact Level: {avgText}</div>
              <div>• Last Updated: {metrics.last_updated ?? '-'}</div>
            </div>
          ) : (
            <div>Loading…</div>
          )}
        </div>
        <div className="px-6 py-4 border-t border-gray-200">
          <div className="text-sm font-semibold mb-2">Event | Summary | Impact | Date</div>
          {rows.length === 0 ? (
            <div className="text-sm text-gray-600">No rows.</div>
          ) : (
            <div className="space-y-1 text-sm">
              {rows.map((r, idx) => (
                <div key={idx}>
                  {r.event} | {r.summary} | {r.impact == null ? '-' : (Math.round(r.impact * 10) / 10).toFixed(1)} | {r.date}
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  );
};


