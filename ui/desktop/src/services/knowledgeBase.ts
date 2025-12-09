/**
 * Knowledge Base Service
 * Handles knowledge base reseeding and refresh events.
 */

import { invoke } from '@tauri-apps/api/core';
import { listen } from '@tauri-apps/api/event';
import { ReseedResult } from '@/types/knowledgeBase';

/**
 * Reseed knowledge base using safe temporary tables approach.
 * @returns Promise resolving to reseeding result
 */
export async function reseedKnowledgeBase(): Promise<ReseedResult> {
  try {
    const result = await invoke<ReseedResult>('reseed_knowledge_base');
    return result;
  } catch (error: any) {
    return {
      success: false,
      message: error.message || 'Failed to reseed knowledge base',
      tablesUpdated: [],
    };
  }
}

/**
 * Listen for knowledge base refresh events.
 * @param callback Function to call when knowledge base is refreshed
 * @returns Unlisten function
 */
export async function listenForKnowledgeBaseRefresh(
  callback: () => void
): Promise<() => void> {
  const unlisten = await listen('knowledge-base-refreshed', () => {
    callback();
  });

  return unlisten;
}

