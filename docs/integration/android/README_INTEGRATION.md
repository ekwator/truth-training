## Android Integration Guide (Truth Core v1.0.0)

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

### Building the Android JNI library (cargo)

```bash
rustup target add aarch64-linux-android x86_64-linux-android

# Build shared libraries
cargo build --release --target aarch64-linux-android --features mobile --lib -p truth_core
cargo build --release --target x86_64-linux-android --features mobile --lib -p truth_core

# Outputs:
# target/aarch64-linux-android/release/libtruth_core.so
# target/x86_64-linux-android/release/libtruth_core.so
```

Copy the resulting libraries into your Android client's `app/src/main/jniLibs/<abi>/` folders.

### Overview

- Base URL: your node (e.g., `http://10.0.2.2:8080` for Android emulator, `https://truth-core.example.com` for production)
- Content-Type: `application/json; charset=utf-8`
- Authentication: JWT (`Authorization: Bearer <jwt>`) for protected endpoints
- Full API reference: see [docs/api_reference/API_REFERENCE.md](../../api_reference/API_REFERENCE.md)
- Collective Intelligence API: `POST /api/v1/recalc_collective` for consensus recalculation

### v1.0.0 API Endpoints

**Events:**
- `POST /api/v1/events` - Create event (with embedded context fields: `category_id`, `forma_id`, `cause_id`, `develop_id`, `effect_id`)
- `GET /api/v1/events` - List events (with pagination)
- `GET /api/v1/events/{id}` - Get event by ID
- `PUT /api/v1/events/{id}` - Update event
- `DELETE /api/v1/events/{id}` - Delete event

**Context Templates:**
- `GET /api/v1/contexts` - List all context templates
- `POST /api/v1/contexts` - Create context template (with duplicate detection)
- `GET /api/v1/contexts/{id}` - Get template by ID
- `GET /api/v1/contexts/by-name/{name}` - Get template by name
- `POST /api/v1/contexts/match` - Match context by embedded fields
- `POST /api/v1/contexts/from-event` - Create template from event
- `PUT /api/v1/contexts/{id}` - Update template
- `DELETE /api/v1/contexts/{id}` - Delete template

**Judgments:**
- `POST /api/v1/judgments` - Submit judgment (assessment: 'true' | 'false' | 'uncertain', confidence_level: 0.0-1.0)
- `GET /api/v1/judgments` - List judgments (optionally filtered by event_id)

**Impacts:**
- `POST /api/v1/impacts` - Add impact (impact_level: 1-5)
- `GET /api/v1/impacts` - List impacts (optionally filtered by event_id)

**Node Discovery:**
- `GET /api/v1/nodes` - List discovered nodes
- `POST /api/v1/nodes` - Add/update node
- `GET /api/v1/nodes/{id}` - Get node by ID
- `POST /api/v1/nodes/sync` - Incremental sync with peer

**Note:** v1.0.0 uses **embedded context fields** instead of `context_id`. Events store `category_id`, `forma_id`, `cause_id`, `develop_id`, `effect_id` directly (all nullable FK references).

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

### API Models (v1.0.0 samples)
```kotlin
// Info & Stats
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

// Events (v1.0.0: embedded context fields)
data class Event(
    val id: Long? = null,
    val description: String,
    val category_id: Long? = null,  // Embedded context field
    val forma_id: Long? = null,       // Embedded context field
    val cause_id: Long? = null,      // Embedded context field
    val develop_id: Long? = null,    // Embedded context field
    val effect_id: Long? = null,     // Embedded context field
    val vector: Boolean = false,
    val detected: Boolean? = null,
    val corrected: Boolean = false,
    val timestamp_start: Long,
    val timestamp_end: Long? = null,
    val code: Int = 1,
    val collective_score: Double? = null
)

// Context Templates (v1.0.0)
data class ContextTemplate(
    val id: Long? = null,
    val name: String,
    val description: String? = null,
    val category_id: Long? = null,
    val forma_id: Long? = null,
    val cause_id: Long? = null,
    val develop_id: Long? = null,
    val effect_id: Long? = null
)

// Judgments
data class Judgment(
    val id: Long? = null,
    val event_id: Long,
    val assessment: String,  // 'true' | 'false' | 'uncertain'
    val confidence_level: Double,  // 0.0-1.0
    val reasoning: String? = null,
    val submitted_at: Long
)

// Impacts
data class Impact(
    val id: Long? = null,
    val event_id: Long,
    val type_id: Long,
    val value: Boolean,  // true = positive, false = negative
    val notes: String? = null
)

// Graph
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

### Retrofit Service (v1.0.0)
```kotlin
interface TruthCoreApi {
    // Info & Stats
    @GET("/api/v1/info")
    suspend fun info(): InfoResponse

    @GET("/api/v1/stats")
    suspend fun stats(): StatsResponse

    // Events (v1.0.0: embedded context fields)
    @GET("/api/v1/events")
    suspend fun listEvents(
        @Query("limit") limit: Int? = null,
        @Query("offset") offset: Int? = null
    ): List<Event>

    @GET("/api/v1/events/{id}")
    suspend fun getEvent(@Path("id") id: Long): Event

    @POST("/api/v1/events")
    suspend fun createEvent(@Body event: Event): Event

    @PUT("/api/v1/events/{id}")
    suspend fun updateEvent(@Path("id") id: Long, @Body event: Event): Event

    @DELETE("/api/v1/events/{id}")
    suspend fun deleteEvent(@Path("id") id: Long)

    // Context Templates (v1.0.0)
    @GET("/api/v1/contexts")
    suspend fun listContexts(): List<ContextTemplate>

    @GET("/api/v1/contexts/{id}")
    suspend fun getContext(@Path("id") id: Long): ContextTemplate

    @GET("/api/v1/contexts/by-name/{name}")
    suspend fun getContextByName(@Path("name") name: String): ContextTemplate

    @POST("/api/v1/contexts")
    suspend fun createContext(@Body template: ContextTemplate): ContextTemplate

    @POST("/api/v1/contexts/match")
    suspend fun matchContext(
        @Query("category_id") categoryId: Long? = null,
        @Query("forma_id") formaId: Long? = null,
        @Query("cause_id") causeId: Long? = null,
        @Query("develop_id") developId: Long? = null,
        @Query("effect_id") effectId: Long? = null
    ): ContextTemplate?

    @POST("/api/v1/contexts/from-event")
    suspend fun createContextFromEvent(@Body event: Event): ContextTemplate

    @PUT("/api/v1/contexts/{id}")
    suspend fun updateContext(@Path("id") id: Long, @Body template: ContextTemplate): ContextTemplate

    @DELETE("/api/v1/contexts/{id}")
    suspend fun deleteContext(@Path("id") id: Long)

    // Judgments
    @POST("/api/v1/judgments")
    suspend fun submitJudgment(@Body judgment: Judgment): Judgment

    @GET("/api/v1/judgments")
    suspend fun listJudgments(@Query("event_id") eventId: Long? = null): List<Judgment>

    // Impacts
    @POST("/api/v1/impacts")
    suspend fun addImpact(@Body impact: Impact): Impact

    @GET("/api/v1/impacts")
    suspend fun listImpacts(@Query("event_id") eventId: Long? = null): List<Impact>

    // Graph
    @GET("/graph/json")
    suspend fun graph(
        @Query("min_priority") minPriority: Double? = null,
        @Query("limit") limit: Int? = null
    ): GraphResponse

    // Collective Intelligence
    @POST("/api/v1/recalc_collective")
    suspend fun recalcCollective()
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

- This document targets Truth Training v1.0.0 (Core, Server, Desktop, Android).
- Android Client: v1.0.0 (stable, full feature parity with Desktop UI)
- Core Library: v1.0.0
- API Compatibility: v1.0.0 (embedded context fields, context templates, judgments, consensus)

### Android Client Architecture (v1.0.0)

**Offline-First Design:**
- **Room Database**: Local SQLite persistence with reactive Flow-based queries
- **Sync Queue**: Pending operations queued for background sync
- **WorkManager**: Periodic background sync every 15 minutes
- **Local-Wins Conflict Resolution**: Local changes take precedence over remote

**Key Components:**
- `TruthDatabase.kt` - Room database with schema migrations
- `EventRepository`, `ContextTemplateRepository`, `JudgmentRepository` - Data access layer
- `SyncWorker` (WorkManager) - Background synchronization
- `P2PSyncManager` - Peer-to-peer event propagation
- `DiscoveryRepository` - Node discovery (UDP multicast, global registry)

**UI Framework:**
- **Jetpack Compose**: Modern Material 3 design
- **MVVM Architecture**: ViewModel + StateFlow for reactive UI
- **Navigation**: Jetpack Navigation Compose

**P2P Discovery:**
- **UDP Multicast**: Standard address `239.255.0.1:52525` for LAN discovery
- **Global Registry**: HTTPS polling for internet-accessible nodes
- **NSD Discovery**: Android Network Service Discovery for local peers

### Migration Notes (v0.3.0 → v1.0.0)

**Breaking Changes:**
- `context_id` removed from events; use embedded fields (`category_id`, `forma_id`, `cause_id`, `develop_id`, `effect_id`)
- Database schema migration required (Room migrations V1→V2)
- API endpoints updated for context templates

**See:** [Android Migration Guide](../../ANDROID_MIGRATION.md) for detailed migration instructions.

### Samples

Example JSON response payloads:
- [info.json](sample_responses/info.json) - `/api/v1/info` response format
- [stats.json](sample_responses/stats.json) - `/api/v1/stats` response format
- [graph.json](sample_responses/graph.json) - `/graph/json` response format

See also:
- [Android Quickstart Guide](../../quickstart_android.md) for complete setup instructions
- [Android Architecture Documentation](../../android_discovery_architecture.md) for discovery and sync details

_Version: v1.0.0_
