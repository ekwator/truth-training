package com.truth.training.client.data.network

import com.truth.training.client.data.network.dto.AuthRequest
import com.truth.training.client.data.network.dto.AuthResponse
import com.truth.training.client.data.network.dto.InfoResponse
import com.truth.training.client.data.network.dto.StatsResponse
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

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
}


