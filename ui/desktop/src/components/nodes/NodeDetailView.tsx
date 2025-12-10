/**
 * NodeDetailView Component
 * Modal dialog for viewing detailed information about a network node
 * 
 * Features:
 * - Display all node fields (address, type, status, timestamps, etc.)
 * - Calculate expires_in and age
 * - Use NodeTypeMapper for user-friendly type display
 * - Refresh button to reload node data
 * - Emoji support (Rule 8)
 * - Localization (EN/RU)
 */

import React, { useState, useEffect } from 'react';
import { Dialog } from '@headlessui/react';
import { XMarkIcon, ArrowPathIcon } from '@heroicons/react/24/outline';
import { ApiService } from '@/services/api';
import type { NodeRecord } from '@/types/api';
import { getBothTypes } from '@/utils/nodeTypeMapper';
import { getEmoji } from '@/utils/emojiMapping';
import { useToast } from '@/components/system/Toaster';

interface NodeDetailViewProps {
  isOpen: boolean;
  onClose: () => void;
  nodeId: number;
  initialNode?: NodeRecord;
}

export const NodeDetailView: React.FC<NodeDetailViewProps> = ({
  isOpen,
  onClose,
  nodeId,
  initialNode
}) => {
  const [node, setNode] = useState<NodeRecord | null>(initialNode || null);
  const [loading, setLoading] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);
  const [now, setNow] = useState<number>(Math.floor(Date.now() / 1000));
  const { addToast } = useToast();

  // Update current time every second for live calculations
  useEffect(() => {
    if (!isOpen) return;
    const timer = setInterval(() => {
      setNow(Math.floor(Date.now() / 1000));
    }, 1000);
    return () => clearInterval(timer);
  }, [isOpen]);

  // Load node data when modal opens
  useEffect(() => {
    if (isOpen && nodeId) {
      loadNode();
    }
  }, [isOpen, nodeId]);

  const loadNode = async () => {
    setLoading(true);
    setError(null);
    try {
      // Get all nodes and find the one with matching id
      const allNodes = await ApiService.listNodes();
      const foundNode = allNodes.find(n => n.id === nodeId);
      
      if (!foundNode) {
        setError('Node not found');
        return;
      }
      
      setNode(foundNode);
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : 'Failed to load node details';
      setError(errorMessage);
      addToast({
        type: 'error',
        title: 'Error',
        message: errorMessage
      });
    } finally {
      setLoading(false);
    }
  };

  const formatTimestamp = (timestamp: number) => {
    return new Date(timestamp * 1000).toLocaleString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit'
    });
  };

  const formatDuration = (seconds: number) => {
    if (seconds < 0) return 'Expired';
    if (seconds < 60) return `${Math.floor(seconds)}s`;
    if (seconds < 3600) return `${Math.floor(seconds / 60)}m`;
    if (seconds < 86400) return `${Math.floor(seconds / 3600)}h`;
    return `${Math.floor(seconds / 86400)}d`;
  };

  if (!node && !loading && !error) {
    return null;
  }

  // Calculate expires_in and age
  const expiresIn = node ? Math.max(0, node.last_seen + node.ttl - now) : 0;
  const age = node ? now - node.last_seen : 0;
  const { userFriendly, technical } = node ? getBothTypes(node.node_type) : { userFriendly: 'Unknown', technical: 'Unknown' };

  return (
    <Dialog open={isOpen} onClose={onClose} className="relative z-50">
      {/* Backdrop */}
      <div className="fixed inset-0 bg-gray-600 bg-opacity-50 dark:bg-gray-900 dark:bg-opacity-75" aria-hidden="true" />

      {/* Modal */}
      <div className="fixed inset-0 flex items-center justify-center p-4">
        <Dialog.Panel className="w-full max-w-2xl bg-white dark:bg-gray-800 rounded-lg shadow-xl max-h-[90vh] overflow-y-auto">
          <div className="p-6">
            {/* Header */}
            <div className="flex items-center justify-between mb-4">
              <Dialog.Title className="text-xl font-semibold text-gray-900 dark:text-gray-100">
                {getEmoji('navigation', 'nodes')} Node Details
              </Dialog.Title>
              <div className="flex items-center space-x-2">
                <button
                  onClick={loadNode}
                  disabled={loading}
                  className="p-2 text-gray-400 dark:text-gray-500 hover:text-gray-600 dark:hover:text-gray-300 disabled:opacity-50"
                  aria-label="Refresh"
                >
                  <ArrowPathIcon className={`h-5 w-5 ${loading ? 'animate-spin' : ''}`} />
                </button>
                <button
                  onClick={onClose}
                  className="text-gray-400 dark:text-gray-500 hover:text-gray-600 dark:hover:text-gray-300"
                  aria-label="Close"
                >
                  <XMarkIcon className="h-6 w-6" />
                </button>
              </div>
            </div>

            {/* Content */}
            {loading && !node ? (
              <div className="text-center py-8">
                <p className="text-gray-500 dark:text-gray-400">Loading node details...</p>
              </div>
            ) : error ? (
              <div className="p-4 bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 rounded-md">
                <p className="text-sm text-red-800 dark:text-red-200">{error}</p>
              </div>
            ) : node ? (
              <div className="space-y-4">
                {/* Address */}
                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <label className="block text-sm font-medium text-gray-500 dark:text-gray-400 mb-1">
                      Address
                    </label>
                    <p className="text-sm font-mono text-gray-900 dark:text-gray-100">{node.address}</p>
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-gray-500 dark:text-gray-400 mb-1">
                      Node ID
                    </label>
                    <p className="text-sm text-gray-900 dark:text-gray-100">{node.node_id || 'N/A'}</p>
                  </div>
                </div>

                {/* Type */}
                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <label className="block text-sm font-medium text-gray-500 dark:text-gray-400 mb-1">
                      Type (User-Friendly)
                    </label>
                    <p className="text-sm font-semibold text-gray-900 dark:text-gray-100">{userFriendly}</p>
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-gray-500 dark:text-gray-400 mb-1">
                      Type (Technical)
                    </label>
                    <p className="text-sm text-gray-900 dark:text-gray-100">{technical}</p>
                  </div>
                </div>

                {/* Status */}
                <div>
                  <label className="block text-sm font-medium text-gray-500 dark:text-gray-400 mb-1">
                    Status
                  </label>
                  <span className={`inline-flex items-center px-3 py-1 rounded-full text-sm font-medium ${
                    node.reachable
                      ? 'bg-green-100 dark:bg-green-900 text-green-800 dark:text-green-200'
                      : 'bg-red-100 dark:bg-red-900 text-red-800 dark:text-red-200'
                  }`}>
                    {node.reachable ? 'Reachable' : 'Unreachable'}
                  </span>
                </div>

                {/* TTL and Expiration */}
                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <label className="block text-sm font-medium text-gray-500 dark:text-gray-400 mb-1">
                      TTL
                    </label>
                    <p className="text-sm text-gray-900 dark:text-gray-100">{node.ttl} seconds</p>
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-gray-500 dark:text-gray-400 mb-1">
                      Expires In
                    </label>
                    <p className={`text-sm font-medium ${
                      expiresIn > 0
                        ? 'text-gray-900 dark:text-gray-100'
                        : 'text-red-600 dark:text-red-400'
                    }`}>
                      {expiresIn > 0 ? formatDuration(expiresIn) : 'Expired'}
                    </p>
                  </div>
                </div>

                {/* Timestamps */}
                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <label className="block text-sm font-medium text-gray-500 dark:text-gray-400 mb-1">
                      Last Seen
                    </label>
                    <p className="text-sm text-gray-900 dark:text-gray-100">{formatTimestamp(node.last_seen)}</p>
                    <p className="text-xs text-gray-500 dark:text-gray-400 mt-1">
                      Age: {formatDuration(age)}
                    </p>
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-gray-500 dark:text-gray-400 mb-1">
                      Updated At
                    </label>
                    <p className="text-sm text-gray-900 dark:text-gray-100">{formatTimestamp(node.updated_at)}</p>
                  </div>
                </div>

                {/* Source */}
                {node.source && (
                  <div>
                    <label className="block text-sm font-medium text-gray-500 dark:text-gray-400 mb-1">
                      Source
                    </label>
                    <p className="text-sm text-gray-900 dark:text-gray-100">{node.source}</p>
                  </div>
                )}

                {/* Additional Info */}
                <div className="pt-4 border-t dark:border-gray-700">
                  <p className="text-xs text-gray-500 dark:text-gray-400">
                    Internal ID: {node.id}
                  </p>
                </div>
              </div>
            ) : null}

            {/* Actions */}
            <div className="flex justify-end pt-4 mt-4 border-t dark:border-gray-700">
              <button
                onClick={onClose}
                className="px-4 py-2 text-sm font-medium text-gray-700 dark:text-gray-300 bg-white dark:bg-gray-700 border border-gray-300 dark:border-gray-600 rounded-md hover:bg-gray-50 dark:hover:bg-gray-600 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500"
              >
                {getEmoji('actions', 'back')} Close
              </button>
            </div>
          </div>
        </Dialog.Panel>
      </div>
    </Dialog>
  );
};

