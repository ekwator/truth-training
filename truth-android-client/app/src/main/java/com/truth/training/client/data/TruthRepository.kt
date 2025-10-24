package com.truth.training.client.data

import android.content.Context
import com.truth.training.client.BuildConfig
import com.truth.training.client.data.network.NetworkModule
import com.truth.training.client.data.network.MockTruthApi
import com.truth.training.client.data.network.TokenStorage
import com.truth.training.client.data.network.dto.AuthRequest
import com.truth.training.client.data.network.dto.InfoResponse
import com.truth.training.client.data.network.dto.StatsResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class TruthRepository(context: Context) {
    private val tokenStorage = TokenStorage(context)
    private val api = if (BuildConfig.FLAVOR == "mock") {
        MockTruthApi(context)
    } else {
        NetworkModule.provideApi(
            NetworkModule.provideRetrofit(
                NetworkModule.provideOkHttp(context, tokenStorage)
            )
        )
    }

    private val _lastSync = MutableStateFlow<Long?>(null)
    val lastSync: Flow<Long?> = _lastSync.asStateFlow()

    suspend fun login(email: String, password: String): Result<Unit> = runCatching {
        val resp = api.authenticate(AuthRequest(email, password))
        if (!resp.isSuccessful || resp.body() == null) error("Auth failed: ${resp.code()}")
        val body = resp.body()!!
        tokenStorage.saveTokens(body.accessToken, body.refreshToken)
    }

    suspend fun fetchInfo(): Result<InfoResponse> = runCatching {
        val resp = api.getInfo()
        if (!resp.isSuccessful || resp.body() == null) error("Info failed: ${resp.code()}")
        _lastSync.value = System.currentTimeMillis()
        resp.body()!!
    }

    suspend fun fetchStats(): Result<StatsResponse> = runCatching {
        val resp = api.getStats()
        if (!resp.isSuccessful || resp.body() == null) error("Stats failed: ${resp.code()}")
        _lastSync.value = System.currentTimeMillis()
        resp.body()!!
    }

    suspend fun fetchGraphJson(): Result<String> = runCatching {
        val resp = api.getGraphJson()
        if (!resp.isSuccessful || resp.body() == null) error("Graph failed: ${resp.code()}")
        _lastSync.value = System.currentTimeMillis()
        resp.body()!!.string()
    }
}


