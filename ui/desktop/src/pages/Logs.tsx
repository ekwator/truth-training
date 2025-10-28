import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { LOGS_PAGE_SIZE } from '@/constants';

type LogItem = {
  id: string;
  timestamp: string;
  source: string;
  level: string;
  message: string;
};

type LogsPage = {
  items: LogItem[];
  page: number;
  total: number;
};

function isTauri() {
  return typeof window !== 'undefined' && '__TAURI__' in window;
}

export const Logs: React.FC = () => {
  const [page, setPage] = useState(1);
  const [total, setTotal] = useState(0);
  const [items, setItems] = useState<LogItem[]>([]);
  const [loading, setLoading] = useState(false);
  const totalPages = useMemo(() => Math.max(1, Math.ceil(total / LOGS_PAGE_SIZE)), [total]);

  const fetchLogs = useCallback(async (p: number) => {
    setLoading(true);
    try {
      if (isTauri()) {
        const { invoke } = await import('@tauri-apps/api/core');
        const resp = (await invoke('list_logs', { page: p })) as LogsPage;
        setItems(resp.items);
        setTotal(resp.total);
        setPage(p);
      } else {
        setItems([]);
        setTotal(0);
        setPage(1);
      }
    } finally {
      setLoading(false);
    }
  }, []);

  const clearLogs = useCallback(async () => {
    if (isTauri()) {
      const { invoke } = await import('@tauri-apps/api/core');
      await invoke('clear_logs');
      fetchLogs(1);
    }
  }, [fetchLogs]);

  useEffect(() => {
    fetchLogs(1);
  }, [fetchLogs]);

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <div className="bg-white shadow rounded-lg">
        <div className="px-6 py-4 border-b border-gray-200 flex justify-between items-center">
          <h2 className="text-lg font-medium text-gray-900">Logs</h2>
          <div className="space-x-2">
            <button className="px-3 py-1 bg-gray-100 border rounded" onClick={() => fetchLogs(page)} disabled={loading}>Refresh</button>
            <button className="px-3 py-1 bg-gray-100 border rounded" onClick={clearLogs} disabled={loading}>Clear Log</button>
          </div>
        </div>
        <div className="px-6 py-4 text-sm font-mono whitespace-pre-wrap">
          {loading ? (
            <div>Loading…</div>
          ) : items.length === 0 ? (
            <div>No log entries.</div>
          ) : (
            <div className="space-y-1">
              {items.map((it) => (
                <div key={it.id}>
                  [{it.timestamp}] {it.level} {it.source} — {it.message}
                </div>
              ))}
            </div>
          )}
        </div>
        <div className="px-6 py-4 border-t border-gray-200 flex items-center justify-between text-sm">
          <div>
            Page {page} of {totalPages} • {LOGS_PAGE_SIZE} lines/page • Total {total}
          </div>
          <div className="space-x-2">
            <button className="px-3 py-1 bg-gray-100 border rounded" onClick={() => fetchLogs(1)} disabled={page === 1 || loading}>First</button>
            <button className="px-3 py-1 bg-gray-100 border rounded" onClick={() => fetchLogs(Math.max(1, page - 1))} disabled={page === 1 || loading}>Prev</button>
            <button className="px-3 py-1 bg-gray-100 border rounded" onClick={() => fetchLogs(Math.min(totalPages, page + 1))} disabled={page >= totalPages || loading}>Next</button>
            <button className="px-3 py-1 bg-gray-100 border rounded" onClick={() => fetchLogs(totalPages)} disabled={page >= totalPages || loading}>Last</button>
          </div>
        </div>
      </div>
    </div>
  );
};


