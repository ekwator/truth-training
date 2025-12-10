package com.truth.training.client.utils

/**
 * Utility for mapping technical node types to user-friendly types.
 * 
 * Mapping logic:
 * - Hub = RELAY or GLOBAL (network hubs)
 * - Leaf = LAN, WIFI, or CLIENT (end nodes)
 */
object NodeTypeMapper {
    
    /**
     * Maps technical node type to user-friendly type.
     * 
     * @param technicalType Technical node type (LAN, WIFI, GLOBAL, RELAY, CLIENT)
     * @return User-friendly type: "Hub" or "Leaf", or "Unknown" if invalid
     */
    fun mapToUserFriendly(technicalType: String?): String {
        return when (technicalType?.uppercase()) {
            "RELAY", "GLOBAL" -> "Hub"
            "LAN", "WIFI", "CLIENT" -> "Leaf"
            else -> "Unknown"
        }
    }
    
    /**
     * Checks if node type is a Hub.
     * 
     * @param technicalType Technical node type
     * @return true if type is RELAY or GLOBAL, false otherwise
     */
    fun isHub(technicalType: String?): Boolean {
        return technicalType?.uppercase() in listOf("RELAY", "GLOBAL")
    }
    
    /**
     * Checks if node type is a Leaf.
     * 
     * @param technicalType Technical node type
     * @return true if type is LAN, WIFI, or CLIENT, false otherwise
     */
    fun isLeaf(technicalType: String?): Boolean {
        return technicalType?.uppercase() in listOf("LAN", "WIFI", "CLIENT")
    }
    
    /**
     * Gets both user-friendly and technical type display.
     * 
     * @param technicalType Technical node type
     * @return Pair of (userFriendly, technical) types, or ("Unknown", original) if invalid
     */
    fun getBothTypes(technicalType: String?): Pair<String, String> {
        val userFriendly = mapToUserFriendly(technicalType)
        val technical = technicalType ?: "Unknown"
        return Pair(userFriendly, technical)
    }
}

