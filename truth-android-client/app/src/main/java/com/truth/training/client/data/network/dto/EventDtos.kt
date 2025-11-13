package com.truth.training.client.data.network.dto

import com.google.gson.annotations.SerializedName

/**
 * DTOs for Events API v1.0.0 aligned with embedded context schema.
 */

data class CreateEventRequest(
    val description: String,
    @SerializedName("category_id") val categoryId: Int? = null,
    @SerializedName("forma_id") val formaId: Int? = null,
    @SerializedName("cause_id") val causeId: Int? = null,
    @SerializedName("develop_id") val developId: Int? = null,
    @SerializedName("effect_id") val effectId: Int? = null,
    val vector: Boolean = true,
    @SerializedName("timestamp_start") val timestampStart: Long,
    @SerializedName("timestamp_end") val timestampEnd: Long? = null,
    val code: Int = 1,
    @SerializedName("collective_score") val collectiveScore: Double? = null
)

data class UpdateEventRequest(
    val description: String? = null,
    @SerializedName("category_id") val categoryId: Int? = null,
    @SerializedName("forma_id") val formaId: Int? = null,
    @SerializedName("cause_id") val causeId: Int? = null,
    @SerializedName("develop_id") val developId: Int? = null,
    @SerializedName("effect_id") val effectId: Int? = null,
    val vector: Boolean? = null,
    @SerializedName("timestamp_start") val timestampStart: Long? = null,
    @SerializedName("timestamp_end") val timestampEnd: Long? = null,
    val code: Int? = null,
    @SerializedName("collective_score") val collectiveScore: Double? = null,
    val detected: Boolean? = null,
    val corrected: Boolean? = null
)

data class EventResponse(
    val id: Long,
    val description: String,
    @SerializedName("category_id") val categoryId: Int? = null,
    @SerializedName("forma_id") val formaId: Int? = null,
    @SerializedName("cause_id") val causeId: Int? = null,
    @SerializedName("develop_id") val developId: Int? = null,
    @SerializedName("effect_id") val effectId: Int? = null,
    val vector: Boolean,
    val detected: Boolean?,
    val corrected: Boolean,
    @SerializedName("timestamp_start") val timestampStart: Long,
    @SerializedName("timestamp_end") val timestampEnd: Long? = null,
    val code: Int,
    @SerializedName("collective_score") val collectiveScore: Double?
)

data class EventDetailsResponse(
    val id: Long,
    val description: String,
    @SerializedName("category_id") val categoryId: Int? = null,
    @SerializedName("forma_id") val formaId: Int? = null,
    @SerializedName("cause_id") val causeId: Int? = null,
    @SerializedName("develop_id") val developId: Int? = null,
    @SerializedName("effect_id") val effectId: Int? = null,
    val vector: Boolean,
    val detected: Boolean?,
    val corrected: Boolean,
    @SerializedName("timestamp_start") val timestampStart: Long,
    @SerializedName("timestamp_end") val timestampEnd: Long? = null,
    val code: Int,
    @SerializedName("collective_score") val collectiveScore: Double?,
    val impacts: List<Impact> = emptyList(),
    val judgments: List<Judgment> = emptyList(),
    val summary: Summary? = null,
    val consensus: Consensus? = null
)

data class EventListResponse(
    val data: List<EventResponse>,
    val total: Int
)

