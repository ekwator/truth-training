package com.truth.training.client.data.network

import android.content.Context
import com.truth.training.client.BuildConfig
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking

class TokenStorage(context: Context) {
    private val prefs = context.getSharedPreferences("truth_tokens", Context.MODE_PRIVATE)

    fun getAccessToken(): String? = prefs.getString("access", null)
    fun getRefreshToken(): String? = prefs.getString("refresh", null)
    fun saveTokens(access: String?, refresh: String?) {
        prefs.edit().apply {
            if (access != null) putString("access", access) else remove("access")
            if (refresh != null) putString("refresh", refresh) else remove("refresh")
        }.apply()
    }
    fun clear() { saveTokens(null, null) }
}

class AuthInterceptor(private val tokenStorage: TokenStorage) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val token = tokenStorage.getAccessToken()
        val request: Request = if (!token.isNullOrEmpty()) {
            original.newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
        } else original
        return chain.proceed(request)
    }
}

class RefreshAuthenticator(
    private val context: Context,
    private val tokenStorage: TokenStorage
) : Interceptor {
    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        var request = chain.request()
        var response = chain.proceed(request)
        if (response.code == 401) {
            response.close()
            synchronized(this) {
                val refreshToken = tokenStorage.getRefreshToken()
                if (refreshToken.isNullOrEmpty()) return response
                val refreshClient = OkHttpClient.Builder()
                    .addInterceptor(HttpLoggingInterceptor().apply {
                        level = HttpLoggingInterceptor.Level.BASIC
                    })
                    .build()
                val retrofit = Retrofit.Builder()
                    .baseUrl(BuildConfig.BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(refreshClient)
                    .build()
                val api = retrofit.create(TruthApi::class.java)
                try {
                    val refreshResp = runBlocking { api.refreshToken() }
                    if (refreshResp.isSuccessful) {
                        val body = refreshResp.body()
                        if (body != null) {
                            tokenStorage.saveTokens(body.accessToken, body.refreshToken)
                            request = request.newBuilder()
                                .removeHeader("Authorization")
                                .addHeader("Authorization", "Bearer ${body.accessToken}")
                                .build()
                            response = chain.proceed(request)
                        }
                    }
                } catch (_: Exception) { }
            }
        }
        return response
    }
}

object NetworkModule {
    fun provideOkHttp(context: Context, tokenStorage: TokenStorage): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.BASIC
        }
        return OkHttpClient.Builder()
            .addInterceptor(logging)
            .addInterceptor(AuthInterceptor(tokenStorage))
            .addInterceptor(RefreshAuthenticator(context, tokenStorage))
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .client(okHttpClient)
        .build()

    fun provideApi(retrofit: Retrofit): TruthApi = retrofit.create(TruthApi::class.java)
}


