package com.truth.training.client.data.network.dto

import com.google.gson.annotations.SerializedName

/**
 * DTOs for Judgments API v1.0.0.
 */

data class Judgment(
    val id: String,
    @SerializedName("event_id") val eventId: String,
    val assessment: String,  // "true" | "false" | "uncertain"
    @SerializedName("confidence_level") val confidenceLevel: Double,  // 0.0-1.0
    val reasoning: String? = null,
    @SerializedName("submitted_at") val submittedAt: String
)

data class CreateJudgmentRequest(
    @SerializedName("event_id") val eventId: String,
    val assessment: String,  // "true" | "false" | "uncertain"
    @SerializedName("confidence_level") val confidenceLevel: Double,  // 0.0-1.0
    val reasoning: String? = null
)

data class JudgmentListResponse(
    val data: List<Judgment>,
    val total: Int
)

data class JudgmentStatsResponse(
    @SerializedName("true_count") val trueCount: Int,
    @SerializedName("false_count") val falseCount: Int,
    @SerializedName("uncertain_count") val uncertainCount: Int,
    @SerializedName("avg_confidence") val avgConfidence: Double,
    @SerializedName("last_submitted_at") val lastSubmittedAt: String? = null
)

