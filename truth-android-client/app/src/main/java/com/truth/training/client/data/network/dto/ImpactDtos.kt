package com.truth.training.client.data.network.dto

import com.google.gson.annotations.SerializedName

/**
 * DTOs for Impacts API v1.0.0.
 */

data class Impact(
    val id: String,
    @SerializedName("event_id") val eventId: String,
    @SerializedName("impact_level") val impactLevel: Int,  // 1-5
    val notes: String? = null,
    @SerializedName("created_at") val createdAt: String
)

data class CreateImpactRequest(
    @SerializedName("event_id") val eventId: String,
    @SerializedName("impact_level") val impactLevel: Int,  // 1-5
    val notes: String? = null
)

/**
 * Summary DTO (used in EventDetailsResponse).
 */
data class Summary(
    val id: String,
    @SerializedName("event_id") val eventId: String,
    @SerializedName("summary_text") val summaryText: String? = null,
    val recommendations: String? = null,
    @SerializedName("updated_at") val updatedAt: String
)

/**
 * Consensus DTO (used in EventDetailsResponse).
 */
data class Consensus(
    @SerializedName("dominant_assessment") val dominantAssessment: String,
    val confidence: Double,
    @SerializedName("total_judgments") val totalJudgments: Int,
    @SerializedName("weighted_confidence") val weightedConfidence: Double
)

