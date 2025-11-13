package com.truth.training.client.data.network

import android.content.Context
import com.google.gson.Gson
import com.truth.training.client.data.network.dto.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.Response

class MockTruthApi(private val context: Context) : TruthApi {
    private fun load(path: String): String = context.assets.open(path).bufferedReader().use { it.readText() }

    override suspend fun authenticate(body: AuthRequest): Response<AuthResponse> {
        val obj = Gson().fromJson(load("api/auth.json"), AuthResponse::class.java)
        return Response.success(obj)
    }

    override suspend fun getInfo(): Response<InfoResponse> {
        val obj = Gson().fromJson(load("api/info.json"), InfoResponse::class.java)
        return Response.success(obj)
    }

    override suspend fun getStats(): Response<StatsResponse> {
        val obj = Gson().fromJson(load("api/stats.json"), StatsResponse::class.java)
        return Response.success(obj)
    }

    override suspend fun getGraphJson(): Response<ResponseBody> {
        return Response.success(load("api/graph.json").toResponseBody("application/json".toMediaType()))
    }

    override suspend fun refreshToken(): Response<AuthResponse> {
        val obj = Gson().fromJson(load("api/auth.json"), AuthResponse::class.java)
        return Response.success(obj)
    }

    override suspend fun listEvents(limit: Int, offset: Int): Response<EventListResponse> {
        val obj = Gson().fromJson(load("api/events.json"), EventListResponse::class.java)
        return Response.success(obj)
    }

    override suspend fun createEvent(body: CreateEventRequest): Response<EventResponse> {
        val obj = Gson().fromJson(load("api/event_create.json"), EventResponse::class.java)
        return Response.success(obj)
    }

    override suspend fun getEvent(id: Long): Response<EventDetailsResponse> {
        val obj = Gson().fromJson(load("api/event_detail.json"), EventDetailsResponse::class.java)
        return Response.success(obj)
    }

    override suspend fun updateEvent(id: Long, body: UpdateEventRequest): Response<EventResponse> {
        val obj = Gson().fromJson(load("api/event_update.json"), EventResponse::class.java)
        return Response.success(obj)
    }

    override suspend fun deleteEvent(id: Long): Response<Unit> = Response.success(Unit)

    override suspend fun listContexts(): Response<ContextListResponse> {
        val obj = Gson().fromJson(load("api/contexts.json"), ContextListResponse::class.java)
        return Response.success(obj)
    }

    override suspend fun createContext(body: CreateContextRequest): Response<ContextTemplate> {
        val obj = Gson().fromJson(load("api/context_create.json"), ContextTemplate::class.java)
        return Response.success(obj)
    }

    override suspend fun getContextByName(name: String): Response<ContextTemplate> {
        val obj = Gson().fromJson(load("api/context_detail.json"), ContextTemplate::class.java)
        return Response.success(obj)
    }

    override suspend fun matchContext(body: MatchContextRequest): Response<MatchContextResponse> {
        val obj = Gson().fromJson(load("api/context_match.json"), MatchContextResponse::class.java)
        return Response.success(obj)
    }

    override suspend fun createContextFromEvent(body: CreateContextFromEventRequest): Response<ContextTemplate> {
        val obj = Gson().fromJson(load("api/context_create.json"), ContextTemplate::class.java)
        return Response.success(obj)
    }

    override suspend fun listJudgments(eventId: Long, limit: Int, offset: Int): Response<JudgmentListResponse> {
        val obj = Gson().fromJson(load("api/judgments.json"), JudgmentListResponse::class.java)
        return Response.success(obj)
    }

    override suspend fun submitJudgment(body: CreateJudgmentRequest): Response<Judgment> {
        val obj = Gson().fromJson(load("api/judgment_submit.json"), Judgment::class.java)
        return Response.success(obj)
    }

    override suspend fun getJudgmentStats(eventId: Long): Response<JudgmentStatsResponse> {
        val obj = Gson().fromJson(load("api/judgment_stats.json"), JudgmentStatsResponse::class.java)
        return Response.success(obj)
    }

    override suspend fun addImpact(body: CreateImpactRequest): Response<Impact> {
        val obj = Gson().fromJson(load("api/impact_create.json"), Impact::class.java)
        return Response.success(obj)
    }
}


