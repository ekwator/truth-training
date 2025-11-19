import { useEffect, useMemo, useState } from 'react';
import ApiService from '@/services/api';
import type { NodeRecord } from '@/types/api';
import { useToast } from '@/components/system/Toaster';

const NODE_TYPES = ['ALL', 'LAN', 'WIFI', 'GLOBAL', 'RELAY', 'CLIENT'];

export const NodesPanel: React.FC = () => {
  const [nodes, setNodes] = useState<NodeRecord[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [lastUpdated, setLastUpdated] = useState<number | null>(null);
  const [filterReachable, setFilterReachable] = useState<'all' | 'online' | 'offline'>('all');
  const [typeFilter, setTypeFilter] = useState<string>('ALL');
  const [now, setNow] = useState(Date.now());
  const { addToast } = useToast();

  useEffect(() => {
    refreshNodes();
  }, [typeFilter, filterReachable]);

  useEffect(() => {
    const timer = setInterval(() => setNow(Date.now()), 1000);
    return () => clearInterval(timer);
  }, []);

  const refreshNodes = async () => {
    setLoading(true);
    setError(null);
    try {
      const filter = {
        nodeType: typeFilter === 'ALL' ? undefined : typeFilter,
        reachable:
          filterReachable === 'all'
            ? undefined
            : filterReachable === 'online'
            ? true
            : false,
      };
      const data = await ApiService.listNodes(filter);
      setNodes(data);
      setLastUpdated(Date.now());
    } catch (err) {
      console.error('Failed to load nodes', err);
      setError('Unable to load nodes. Check logs for details.');
    } finally {
      setLoading(false);
    }
  };

  const handleDiscover = async () => {
    try {
      setLoading(true);
      const result = await ApiService.manualDiscover();
      addToast({
        type: 'success',
        title: 'Discovery completed',
        message: `New: ${result.discovered}, updated: ${result.updated}`,
      });
      await refreshNodes();
    } catch (err) {
      console.error('Discovery failed', err);
      addToast({
        type: 'error',
        title: 'Discovery failed',
        message: 'Unable to run discovery cycle.',
      });
    }
  };

  const handleCleanup = async () => {
    try {
      const removed = await ApiService.cleanupNodes();
      addToast({
        type: 'success',
        title: 'Cleanup completed',
        message: `Removed ${removed} stale nodes`,
      });
      await refreshNodes();
    } catch (err) {
      console.error('Cleanup failed', err);
      addToast({
        type: 'error',
        title: 'Cleanup failed',
        message: 'Unable to run TTL cleanup.',
      });
    }
  };

  const handleHealth = async () => {
    try {
      const checked = await ApiService.runNodesHealthCheck();
      addToast({
        type: 'info',
        title: 'Health check finished',
        message: `Checked ${checked} nodes`,
      });
      await refreshNodes();
    } catch (err) {
      console.error('Health check failed', err);
      addToast({
        type: 'error',
        title: 'Health check failed',
        message: 'Unable to run reachability check.',
      });
    }
  };

  const rows = useMemo(() => {
    if (!lastUpdated) return nodes;
    const ageSeconds = (Date.now() - lastUpdated) / 1000;
    return nodes.map((node) => ({
      ...node,
      expires_in: Math.max(0, node.expires_in - ageSeconds),
    }));
  }, [nodes, lastUpdated, now]);

  return (
    <div className="bg-white shadow rounded-lg">
      <div className="flex flex-wrap items-center justify-between px-6 py-4 border-b border-gray-200">
        <div>
          <h2 className="text-lg font-medium text-gray-900">Node Discovery</h2>
          <p className="text-sm text-gray-500">
            LAN/Wi-Fi/Global nodes with TTL countdown and reachability status
          </p>
        </div>
        <div className="flex flex-wrap gap-2 mt-3 sm:mt-0">
          <select
            value={typeFilter}
            onChange={(e) => setTypeFilter(e.target.value)}
            className="border rounded px-2 py-1 text-sm"
          >
            {NODE_TYPES.map((type) => (
              <option key={type} value={type}>
                {type}
              </option>
            ))}
          </select>
          <select
            value={filterReachable}
            onChange={(e) => setFilterReachable(e.target.value as any)}
            className="border rounded px-2 py-1 text-sm"
          >
            <option value="all">All</option>
            <option value="online">Reachable</option>
            <option value="offline">Unreachable</option>
          </select>
          <button
            onClick={refreshNodes}
            className="px-3 py-1 bg-gray-100 rounded text-sm hover:bg-gray-200"
            disabled={loading}
          >
            Refresh
          </button>
          <button
            onClick={handleDiscover}
            className="px-3 py-1 bg-blue-600 text-white rounded text-sm hover:bg-blue-700 disabled:bg-gray-300"
            disabled={loading}
          >
            Discover
          </button>
          <button
            onClick={handleCleanup}
            className="px-3 py-1 bg-emerald-600 text-white rounded text-sm hover:bg-emerald-700 disabled:bg-gray-300"
            disabled={loading}
          >
            Cleanup
          </button>
          <button
            onClick={handleHealth}
            className="px-3 py-1 bg-indigo-600 text-white rounded text-sm hover:bg-indigo-700 disabled:bg-gray-300"
            disabled={loading}
          >
            Health Check
          </button>
        </div>
      </div>
      <div className="px-6 py-4">
        {error && (
          <div className="text-sm text-red-600 mb-3">
            {error}
          </div>
        )}
        {loading ? (
          <div className="text-sm text-gray-500">Loading nodes…</div>
        ) : rows.length === 0 ? (
          <div className="text-sm text-gray-500">No nodes discovered yet.</div>
        ) : (
          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-gray-200 text-sm">
              <thead className="bg-gray-50 text-xs font-medium uppercase tracking-wider text-gray-500">
                <tr>
                  <th className="px-3 py-2 text-left">Address</th>
                  <th className="px-3 py-2">Type</th>
                  <th className="px-3 py-2">Reachable</th>
                  <th className="px-3 py-2">TTL (s)</th>
                  <th className="px-3 py-2">Expires In</th>
                  <th className="px-3 py-2">Source</th>
                  <th className="px-3 py-2">Last Seen</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-200 text-gray-900">
                {rows.map((node) => (
                  <tr key={node.id}>
                    <td className="px-3 py-2 font-mono text-xs">{node.address}</td>
                    <td className="px-3 py-2">{node.node_type}</td>
                    <td className="px-3 py-2">
                      <span
                        className={`inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium ${
                          node.reachable ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800'
                        }`}
                      >
                        {node.reachable ? 'Online' : 'Offline'}
                      </span>
                    </td>
                    <td className="px-3 py-2 text-center">{node.ttl}</td>
                    <td className="px-3 py-2 text-center">
                      {formatDuration(node.expires_in)}
                    </td>
                    <td className="px-3 py-2 text-xs">{node.source ?? '—'}</td>
                    <td className="px-3 py-2 text-xs">
                      {new Date(node.last_seen * 1000).toLocaleString()}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
        {lastUpdated && (
          <p className="text-xs text-gray-500 mt-3">
            Last updated: {new Date(lastUpdated).toLocaleTimeString()}
          </p>
        )}
      </div>
    </div>
  );
};

function formatDuration(seconds?: number) {
  if (seconds === undefined) return '—';
  const secs = Math.max(0, Math.floor(seconds));
  if (secs === 0) return 'Expired';
  if (secs < 60) return `${secs}s`;
  const minutes = Math.floor(secs / 60);
  const remainingSeconds = secs % 60;
  if (minutes < 60) {
    return `${minutes}m ${remainingSeconds}s`;
  }
  const hours = Math.floor(minutes / 60);
  const remainingMinutes = minutes % 60;
  return `${hours}h ${remainingMinutes}m`;
}

