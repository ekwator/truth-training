/**
 * Entity name resolution utility.
 * Matches Android getEntityNameById algorithm with fallback to ID.
 * Used for displaying human-readable entity names instead of IDs.
 */

/**
 * Gets entity name by ID from a list of entities.
 * Falls back to ID string if name not found.
 * 
 * @param id - Entity ID to look up (can be null)
 * @param entities - List of entities to search
 * @param getId - Function to extract ID from entity
 * @param getName - Function to extract name from entity
 * @returns Entity name if found, ID as string if not found, null if id is null
 * 
 * @example
 * ```typescript
 * const categoryName = getEntityNameById(
 *   event.categoryId,
 *   categories,
 *   (c) => c.id,
 *   (c) => c.name
 * );
 * ```
 */
export function getEntityNameById<T>(
  id: number | null,
  entities: T[],
  getId: (entity: T) => number,
  getName: (entity: T) => string
): string | null {
  if (id === null) {
    return null;
  }
  
  const entity = entities.find((e) => getId(e) === id);
  
  if (entity) {
    return getName(entity);
  }
  
  // Fallback to ID if name not found (e.g., knowledge base not loaded yet)
  return id.toString();
}

/**
 * Resolve context field ID to name, fallback to ID if not found.
 * Matches Android algorithm exactly.
 * 
 * @param fieldType - Type of context field (category, forma, cause, develop, effect)
 * @param id - Context field ID to resolve
 * @param entities - Array of entities with id and name properties
 * @returns Entity name if found, ID as string if not found
 * 
 * @example
 * ```typescript
 * const categoryName = resolveContextFieldName(
 *   'category',
 *   event.categoryId,
 *   categories
 * );
 * ```
 */
export function resolveContextFieldName(
  _fieldType: 'category' | 'forma' | 'cause' | 'develop' | 'effect',
  id: number,
  entities: Array<{ id: number; name: string }>
): string {
  const entity = entities.find((e) => e.id === id);
  return entity ? entity.name : `${id}`;
}

