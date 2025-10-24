package com.truth.training.client.data.network

import android.content.Context
import com.google.gson.Gson
import com.truth.training.client.data.network.dto.AuthRequest
import com.truth.training.client.data.network.dto.AuthResponse
import com.truth.training.client.data.network.dto.InfoResponse
import com.truth.training.client.data.network.dto.StatsResponse
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
}


