package com.truth.training.client.core.network

import com.truth.training.client.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

data class PushEnvelope(
    val payload: Map<String, Any>,
    val signature: String,
    val public_key: String
)

interface TruthPushApi {
    @POST("/api/v1/push")
    suspend fun sendEvent(
        @Header("Authorization") bearer: String,
        @Body body: PushEnvelope
    ): Response<Unit>
}

object TruthPushClient {
    private fun client(): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.BASIC
        })
        .build()

    private fun retrofit(): Retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .client(client())
        .build()

    val api: TruthPushApi by lazy { retrofit().create(TruthPushApi::class.java) }
}


