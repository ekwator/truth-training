/**
 * Knowledge base reseeding types.
 * Used for safe database reseeding with temporary tables.
 */

export interface ReseedResult {
  success: boolean;
  message: string;
  tablesUpdated: string[];
}

