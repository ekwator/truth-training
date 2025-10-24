package com.truth.training.client.data.network.dto

data class AuthRequest(
    val email: String,
    val password: String
)

data class AuthResponse(
    val accessToken: String,
    val refreshToken: String? = null,
    val tokenType: String? = "Bearer"
)

data class InfoResponse(
    val version: String?,
    val uptime: String?,
    val nodeId: String?,
    val network: String?
)

data class StatsResponse(
    val peers: Int?,
    val edges: Int?,
    val avgTrust: Double?,
    val updatedAt: String?
)


