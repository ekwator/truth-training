## Android Integration Guide (Truth Core v0.4.0)

### Android JSON signature verification (Ed25519)

Android client signs deterministic serialization of `payload` field (JSON) using Ed25519 and sends together with key:

```json
{
  "node_id": "device-1",
  "payload": { "action": "ping", "n": 1 },
  "signature": "<base64 Ed25519 signature>",
  "public_key": "<base64 Ed25519 public key>"
}
```

On Rust core side verification is performed before processing:
- Extract `signature` and `public_key`.
- Form canonical JSON string from `payload` (`serde_json::to_vec`).
- Verify signature against Ed25519 public key.

Responses:
- Success:
```json
{ "status": "ok", "verified": true }
```
- Signature error:
```json
{ "status": "error", "reason": "invalid_signature" }
```

Notes:
- Both `signature` and `public_key` are base64 of raw Ed25519 bytes (signature: 64 bytes, public key: 32 bytes).
- `payload` serialization must be deterministic and match what Android signed.

This guide helps Android developers consume the Truth Core REST API using Retrofit and JWT authentication.

### Overview

- Base URL: your node (e.g., `http://10.0.2.2:8080` for Android emulator)
- Content-Type: `application/json; charset=utf-8`
- Authentication: JWT (`Authorization: Bearer <jwt>`) for protected endpoints
- Full API reference: see `docs/api_reference/API_REFERENCE.md`
- Collective Intelligence API: `POST /api/v1/recalc_collective` for consensus recalculation

### Retrofit Setup (Kotlin)

Add dependencies in Gradle (Kotlin DSL):
```kotlin
implementation("com.squareup.retrofit2:retrofit:2.11.0")
implementation("com.squareup.retrofit2:converter-moshi:2.11.0")
implementation("com.squareup.okhttp3:okhttp:4.12.0")
implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
implementation("com.squareup.moshi:moshi-kotlin:1.15.1")
```

Create Retrofit instance with optional JWT header:
```kotlin
val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
val authInterceptor = Interceptor { chain ->
    val jwt = tokenProvider.currentJwt()
    val req = if (jwt != null) {
        chain.request().newBuilder()
            .addHeader("Authorization", "Bearer $jwt")
            .build()
    } else chain.request()
    chain.proceed(req)
}

val client = OkHttpClient.Builder()
    .addInterceptor(logging)
    .addInterceptor(authInterceptor)
    .build()

val retrofit = Retrofit.Builder()
    .baseUrl(BASE_URL)
    .client(client)
    .addConverterFactory(MoshiConverterFactory.create())
    .build()
```

### API Models (samples)
```kotlin
data class InfoResponse(
    val name: String,
    val version: String,
    val uptime_sec: Long,
    val started_at: Long,
    val features: List<String>,
    val peer_count: Int
)

data class StatsResponse(
    val events: Int,
    val statements: Int,
    val impacts: Int,
    val node_ratings: Int,
    val group_ratings: Int,
    val avg_trust_score: Double,
    val avg_propagation_priority: Double,
    val avg_relay_success_rate: Double,
    val active_nodes: Int
)

data class GraphNode(
    val id: String,
    val score: Double,
    val propagation_priority: Double,
    val last_seen: Long?,
    val relay_success_rate: Double?
)

data class GraphLink(
    val source: String,
    val target: String,
    val weight: Double,
    val latency_ms: Int?
)

data class GraphResponse(
    val nodes: List<GraphNode>,
    val links: List<GraphLink>
)
```

### Retrofit Service
```kotlin
interface TruthCoreApi {
    @GET("/api/v1/info")
    suspend fun info(): InfoResponse

    @GET("/api/v1/stats")
    suspend fun stats(): StatsResponse

    @GET("/graph/json")
    suspend fun graph(
        @Query("min_priority") minPriority: Double? = null,
        @Query("limit") limit: Int? = null
    ): GraphResponse
}
```

### Auth Notes

- Obtain JWT via `POST /api/v1/auth` and refresh via `POST /api/v1/refresh`.
- Store tokens securely (EncryptedSharedPreferences, Keystore-backed).
- Attach the token in `Authorization: Bearer <jwt>`.

### JSON & Content-Type

- Always send and expect `application/json; charset=utf-8`.
- Numbers are typically `Double` on the wire; map to Kotlin `Double`/`Int`/`Long` appropriately.

### Version

- This document targets truth_core v0.3.0.

### Samples

- See `integration/android/sample_responses/` for example payloads.


