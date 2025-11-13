package com.truth.training.client.data.network

import com.truth.training.client.data.network.dto.*
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

/**
 * Truth Training API v1.0.0 interface.
 */
interface TruthApi {
    @POST("/api/v1/auth")
    suspend fun authenticate(@Body body: AuthRequest): Response<AuthResponse>

    @GET("/api/v1/info")
    suspend fun getInfo(): Response<InfoResponse>

    @GET("/api/v1/stats")
    suspend fun getStats(): Response<StatsResponse>

    @GET("/graph/json")
    suspend fun getGraphJson(): Response<ResponseBody>

    @POST("/api/v1/refresh")
    suspend fun refreshToken(): Response<AuthResponse>

    // Events API (v1.0.0 with embedded fields)
    @GET("/api/v1/events")
    suspend fun listEvents(
        @Query("limit") limit: Int = 35,
        @Query("offset") offset: Int = 0
    ): Response<EventListResponse>

    @POST("/api/v1/events")
    suspend fun createEvent(@Body body: CreateEventRequest): Response<EventResponse>

    @GET("/api/v1/events/{id}")
    suspend fun getEvent(@Path("id") id: Long): Response<EventDetailsResponse>

    @PUT("/api/v1/events/{id}")
    suspend fun updateEvent(
        @Path("id") id: Long,
        @Body body: UpdateEventRequest
    ): Response<EventResponse>

    @DELETE("/api/v1/events/{id}")
    suspend fun deleteEvent(@Path("id") id: Long): Response<Unit>

    // Context API
    @GET("/api/v1/contexts")
    suspend fun listContexts(): Response<ContextListResponse>

    @POST("/api/v1/contexts")
    suspend fun createContext(@Body body: CreateContextRequest): Response<ContextTemplate>

    @GET("/api/v1/contexts/by-name/{name}")
    suspend fun getContextByName(@Path("name") name: String): Response<ContextTemplate>

    @POST("/api/v1/contexts/match")
    suspend fun matchContext(@Body body: MatchContextRequest): Response<MatchContextResponse>

    @POST("/api/v1/contexts/from-event")
    suspend fun createContextFromEvent(@Body body: CreateContextFromEventRequest): Response<ContextTemplate>

    // Judgments API
    @GET("/api/v1/judgments")
    suspend fun listJudgments(
        @Query("event_id") eventId: Long,
        @Query("limit") limit: Int = 35,
        @Query("offset") offset: Int = 0
    ): Response<JudgmentListResponse>

    @POST("/api/v1/judgments")
    suspend fun submitJudgment(@Body body: CreateJudgmentRequest): Response<Judgment>

    @GET("/api/v1/judgments/stats/{event_id}")
    suspend fun getJudgmentStats(@Path("event_id") eventId: Long): Response<JudgmentStatsResponse>

    // Impacts API
    @POST("/api/v1/impacts")
    suspend fun addImpact(@Body body: CreateImpactRequest): Response<Impact>
}


