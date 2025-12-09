/**
 * Entity name resolution utility for Desktop UI.
 * Fetches entity names (category, forma, cause, develop, effect) from knowledge base
 * and merges them with event data for display.
 * 
 * This replaces the JOIN queries that were previously done in the Tauri backend.
 */

import { invoke } from '@tauri-apps/api/core';

/**
 * Entity name data structure
 */
export interface EntityName {
  id: number;
  name: string;
}

/**
 * Entity names cache
 */
export interface EntityNamesCache {
  categories: EntityName[];
  formas: EntityName[];
  causes: EntityName[];
  develops: EntityName[];
  effects: EntityName[];
  lastUpdated: number;
}

let entityNamesCache: EntityNamesCache | null = null;
const CACHE_TTL = 5 * 60 * 1000; // 5 minutes

/**
 * Fetch all entity names from knowledge base via Tauri command.
 * Results are cached to avoid repeated queries.
 * 
 * @returns Promise resolving to entity names cache
 */
export async function fetchEntityNames(): Promise<EntityNamesCache> {
  const now = Date.now();
  
  // Return cached data if still valid
  if (entityNamesCache && (now - entityNamesCache.lastUpdated) < CACHE_TTL) {
    return entityNamesCache;
  }
  
  // Fetch entity names from Tauri command
  // Note: This command needs to be created in knowledge_base.rs
  const categories = await invoke<EntityName[]>('get_entity_names', { entityType: 'category' });
  const formas = await invoke<EntityName[]>('get_entity_names', { entityType: 'forma' });
  const causes = await invoke<EntityName[]>('get_entity_names', { entityType: 'cause' });
  const develops = await invoke<EntityName[]>('get_entity_names', { entityType: 'develop' });
  const effects = await invoke<EntityName[]>('get_entity_names', { entityType: 'effect' });
  
  entityNamesCache = {
    categories,
    formas,
    causes,
    develops,
    effects,
    lastUpdated: now,
  };
  
  return entityNamesCache;
}

/**
 * Clear the entity names cache.
 * Call this after knowledge base reseeding to force refresh.
 */
export function clearEntityNamesCache(): void {
  entityNamesCache = null;
}

/**
 * Resolve entity name by ID from cache.
 * 
 * @param id - Entity ID (can be null)
 * @param entityType - Type of entity (category, forma, cause, develop, effect)
 * @param cache - Entity names cache (from fetchEntityNames)
 * @returns Entity name if found, null if id is null, ID as string if not found
 */
export function resolveEntityName(
  id: number | null,
  entityType: 'category' | 'forma' | 'cause' | 'develop' | 'effect',
  cache: EntityNamesCache
): string | null {
  if (id === null) {
    return null;
  }
  
  let entities: EntityName[];
  switch (entityType) {
    case 'category':
      entities = cache.categories;
      break;
    case 'forma':
      entities = cache.formas;
      break;
    case 'cause':
      entities = cache.causes;
      break;
    case 'develop':
      entities = cache.develops;
      break;
    case 'effect':
      entities = cache.effects;
      break;
  }
  
  const entity = entities.find((e) => e.id === id);
  return entity ? entity.name : id.toString();
}

/**
 * Resolve all entity names for an event.
 * 
 * @param event - Event with category_id, forma_id, cause_id, develop_id, effect_id
 * @param cache - Entity names cache (from fetchEntityNames)
 * @returns Object with resolved entity names
 */
export function resolveEventEntityNames(
  event: {
    category_id: number | null;
    forma_id: number | null;
    cause_id: number | null;
    develop_id: number | null;
    effect_id: number | null;
  },
  cache: EntityNamesCache
): {
  category_name: string | null;
  forma_name: string | null;
  cause_name: string | null;
  develop_name: string | null;
  effect_name: string | null;
} {
  return {
    category_name: resolveEntityName(event.category_id, 'category', cache),
    forma_name: resolveEntityName(event.forma_id, 'forma', cache),
    cause_name: resolveEntityName(event.cause_id, 'cause', cache),
    develop_name: resolveEntityName(event.develop_id, 'develop', cache),
    effect_name: resolveEntityName(event.effect_id, 'effect', cache),
  };
}

/**
 * Resolve entity names for multiple events.
 * More efficient than calling resolveEventEntityNames for each event individually.
 * 
 * @param events - Array of events
 * @param cache - Entity names cache (from fetchEntityNames)
 * @returns Array of events with resolved entity names
 */
export function resolveEventsEntityNames<T extends {
  category_id: number | null;
  forma_id: number | null;
  cause_id: number | null;
  develop_id: number | null;
  effect_id: number | null;
}>(
  events: T[],
  cache: EntityNamesCache
): Array<T & {
  category_name: string | null;
  forma_name: string | null;
  cause_name: string | null;
  develop_name: string | null;
  effect_name: string | null;
}> {
  return events.map((event) => ({
    ...event,
    ...resolveEventEntityNames(event, cache),
  }));
}

