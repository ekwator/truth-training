package com.truth.training.client.data.network.dto

import com.google.gson.annotations.SerializedName

/**
 * DTOs for Events API v1.0.0 with embedded context fields.
 * Replaces legacy context_id with category_id, forma_id, cause_id, develop_id, effect_id.
 */

data class CreateEventRequest(
    val title: String,
    val description: String? = null,
    @SerializedName("category_id") val categoryId: Int? = null,
    @SerializedName("forma_id") val formaId: Int? = null,
    @SerializedName("cause_id") val causeId: Int? = null,
    @SerializedName("develop_id") val developId: Int? = null,
    @SerializedName("effect_id") val effectId: Int? = null,
    @SerializedName("start_date") val startDate: String? = null,
    @SerializedName("end_date") val endDate: String? = null
)

data class UpdateEventRequest(
    val title: String? = null,
    val description: String? = null,
    @SerializedName("category_id") val categoryId: Int? = null,
    @SerializedName("forma_id") val formaId: Int? = null,
    @SerializedName("cause_id") val causeId: Int? = null,
    @SerializedName("develop_id") val developId: Int? = null,
    @SerializedName("effect_id") val effectId: Int? = null,
    @SerializedName("start_date") val startDate: String? = null,
    @SerializedName("end_date") val endDate: String? = null,
    val status: String? = null
)

data class EventResponse(
    val id: String,
    val title: String,
    val description: String? = null,
    @SerializedName("category_id") val categoryId: Int? = null,
    @SerializedName("forma_id") val formaId: Int? = null,
    @SerializedName("cause_id") val causeId: Int? = null,
    @SerializedName("develop_id") val developId: Int? = null,
    @SerializedName("effect_id") val effectId: Int? = null,
    @SerializedName("start_date") val startDate: String? = null,
    @SerializedName("end_date") val endDate: String? = null,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("updated_at") val updatedAt: String? = null,
    val status: String
)

data class EventDetailsResponse(
    val id: String,
    val title: String,
    val description: String? = null,
    @SerializedName("category_id") val categoryId: Int? = null,
    @SerializedName("forma_id") val formaId: Int? = null,
    @SerializedName("cause_id") val causeId: Int? = null,
    @SerializedName("develop_id") val developId: Int? = null,
    @SerializedName("effect_id") val effectId: Int? = null,
    @SerializedName("start_date") val startDate: String? = null,
    @SerializedName("end_date") val endDate: String? = null,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("updated_at") val updatedAt: String? = null,
    val status: String,
    val impacts: List<Impact> = emptyList(),
    val judgments: List<Judgment> = emptyList(),
    val summary: Summary? = null,
    val consensus: Consensus? = null
)

data class EventListResponse(
    val data: List<EventResponse>,
    val total: Int
)

