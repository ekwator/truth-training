/**
 * NodeTypeMapper utility
 * Maps technical node types to user-friendly types (Hub/Leaf)
 * Matching Android implementation pattern
 */

/**
 * Maps technical node type to user-friendly type
 * Hub = RELAY, GLOBAL
 * Leaf = LAN, WIFI, CLIENT
 * 
 * @param technicalType - Technical node type: "LAN", "WIFI", "GLOBAL", "RELAY", "CLIENT"
 * @returns User-friendly type: "Hub", "Leaf", or "Unknown"
 */
export function mapToUserFriendly(technicalType: string | null | undefined): string {
  if (!technicalType) {
    return 'Unknown';
  }
  
  const upper = technicalType.toUpperCase();
  if (upper === 'RELAY' || upper === 'GLOBAL') {
    return 'Hub';
  }
  if (upper === 'LAN' || upper === 'WIFI' || upper === 'CLIENT') {
    return 'Leaf';
  }
  return 'Unknown';
}

/**
 * Checks if node type is a Hub
 * 
 * @param technicalType - Technical node type
 * @returns True if type is RELAY or GLOBAL, false otherwise
 */
export function isHub(technicalType: string | null | undefined): boolean {
  if (!technicalType) {
    return false;
  }
  const upper = technicalType.toUpperCase();
  return upper === 'RELAY' || upper === 'GLOBAL';
}

/**
 * Checks if node type is a Leaf
 * 
 * @param technicalType - Technical node type
 * @returns True if type is LAN, WIFI, or CLIENT, false otherwise
 */
export function isLeaf(technicalType: string | null | undefined): boolean {
  if (!technicalType) {
    return false;
  }
  const upper = technicalType.toUpperCase();
  return upper === 'LAN' || upper === 'WIFI' || upper === 'CLIENT';
}

/**
 * Gets both user-friendly and technical types
 * 
 * @param technicalType - Technical node type
 * @returns Object with both userFriendly and technical types
 */
export function getBothTypes(technicalType: string | null | undefined): {
  userFriendly: string;
  technical: string;
} {
  const userFriendly = mapToUserFriendly(technicalType);
  const technical = technicalType || 'Unknown';
  return { userFriendly, technical };
}

