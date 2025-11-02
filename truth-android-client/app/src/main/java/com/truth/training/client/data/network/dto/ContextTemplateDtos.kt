package com.truth.training.client.data.network.dto

import com.google.gson.annotations.SerializedName

/**
 * DTOs for Context Templates API v1.0.0.
 */

data class ContextTemplate(
    val id: Int,
    val name: String,
    @SerializedName("category_id") val categoryId: Int? = null,
    @SerializedName("forma_id") val formaId: Int? = null,
    @SerializedName("cause_id") val causeId: Int? = null,
    @SerializedName("develop_id") val developId: Int? = null,
    @SerializedName("effect_id") val effectId: Int? = null,
    val description: String? = null
)

data class CreateContextRequest(
    val name: String,
    @SerializedName("category_id") val categoryId: Int? = null,
    @SerializedName("forma_id") val formaId: Int? = null,
    @SerializedName("cause_id") val causeId: Int? = null,
    @SerializedName("develop_id") val developId: Int? = null,
    @SerializedName("effect_id") val effectId: Int? = null,
    val description: String? = null
)

data class MatchContextRequest(
    @SerializedName("category_id") val categoryId: Int? = null,
    @SerializedName("forma_id") val formaId: Int? = null,
    @SerializedName("cause_id") val causeId: Int? = null,
    @SerializedName("develop_id") val developId: Int? = null,
    @SerializedName("effect_id") val effectId: Int? = null
)

data class MatchContextResponse(
    val matched: Boolean,
    val template: ContextTemplate? = null
)

data class CreateContextFromEventRequest(
    val name: String,
    @SerializedName("event_id") val eventId: String,
    val description: String? = null
)

data class ContextListResponse(
    val data: List<ContextTemplate>,
    val total: Int
)

