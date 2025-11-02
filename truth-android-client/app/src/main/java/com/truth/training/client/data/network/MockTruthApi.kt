package com.truth.training.client.data.network

import android.content.Context
import com.google.gson.Gson
import com.truth.training.client.data.network.dto.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.Response

class MockTruthApi(private val context: Context) : TruthApi {
    private fun load(path: String): String {
        return context.assets.open(path).bufferedReader().use { it.readText() }
    }

    override suspend fun authenticate(body: AuthRequest): Response<AuthResponse> {
        val json = load("api/auth.json")
        val obj = Gson().fromJson(json, AuthResponse::class.java)
        return Response.success(obj)
    }

    override suspend fun getInfo(): Response<InfoResponse> {
        val json = load("api/info.json")
        val obj = Gson().fromJson(json, InfoResponse::class.java)
        return Response.success(obj)
    }

    override suspend fun getStats(): Response<StatsResponse> {
        val json = load("api/stats.json")
        val obj = Gson().fromJson(json, StatsResponse::class.java)
        return Response.success(obj)
    }

    override suspend fun getGraphJson(): Response<ResponseBody> {
        val json = load("api/graph.json")
        return Response.success(json.toResponseBody("application/json".toMediaType()))
    }

    override suspend fun refreshToken(): Response<AuthResponse> {
        val json = load("api/auth.json")
        val obj = Gson().fromJson(json, AuthResponse::class.java)
        return Response.success(obj)
    }

    override suspend fun listEvents(limit: Int, offset: Int, status: String?): Response<EventListResponse> {
        val json = load("api/events.json")
        val obj = Gson().fromJson(json, EventListResponse::class.java)
        return Response.success(obj)
    }

    override suspend fun createEvent(body: CreateEventRequest): Response<EventResponse> {
        val json = load("api/event_create.json")
        val obj = Gson().fromJson(json, EventResponse::class.java)
        return Response.success(obj)
    }

    override suspend fun getEvent(id: String): Response<EventDetailsResponse> {
        val json = load("api/event_detail.json")
        val obj = Gson().fromJson(json, EventDetailsResponse::class.java)
        return Response.success(obj)
    }

    override suspend fun updateEvent(id: String, body: UpdateEventRequest): Response<EventResponse> {
        val json = load("api/event_update.json")
        val obj = Gson().fromJson(json, EventResponse::class.java)
        return Response.success(obj)
    }

    override suspend fun deleteEvent(id: String): Response<Unit> {
        return Response.success(Unit)
    }

    override suspend fun listContexts(): Response<ContextListResponse> {
        val json = load("api/contexts.json")
        val obj = Gson().fromJson(json, ContextListResponse::class.java)
        return Response.success(obj)
    }

    override suspend fun createContext(body: CreateContextRequest): Response<ContextTemplate> {
        val json = load("api/context_create.json")
        val obj = Gson().fromJson(json, ContextTemplate::class.java)
        return Response.success(obj)
    }

    override suspend fun getContextByName(name: String): Response<ContextTemplate> {
        val json = load("api/context_detail.json")
        val obj = Gson().fromJson(json, ContextTemplate::class.java)
        return Response.success(obj)
    }

    override suspend fun matchContext(body: MatchContextRequest): Response<MatchContextResponse> {
        val json = load("api/context_match.json")
        val obj = Gson().fromJson(json, MatchContextResponse::class.java)
        return Response.success(obj)
    }

    override suspend fun createContextFromEvent(body: CreateContextFromEventRequest): Response<ContextTemplate> {
        val json = load("api/context_create.json")
        val obj = Gson().fromJson(json, ContextTemplate::class.java)
        return Response.success(obj)
    }

    override suspend fun listJudgments(eventId: String, limit: Int, offset: Int): Response<JudgmentListResponse> {
        val json = load("api/judgments.json")
        val obj = Gson().fromJson(json, JudgmentListResponse::class.java)
        return Response.success(obj)
    }

    override suspend fun submitJudgment(body: CreateJudgmentRequest): Response<Judgment> {
        val json = load("api/judgment_submit.json")
        val obj = Gson().fromJson(json, Judgment::class.java)
        return Response.success(obj)
    }

    override suspend fun getJudgmentStats(eventId: String): Response<JudgmentStatsResponse> {
        val json = load("api/judgment_stats.json")
        val obj = Gson().fromJson(json, JudgmentStatsResponse::class.java)
        return Response.success(obj)
    }

    override suspend fun addImpact(body: CreateImpactRequest): Response<Impact> {
        val json = load("api/impact_create.json")
        val obj = Gson().fromJson(json, Impact::class.java)
        return Response.success(obj)
    }
}


