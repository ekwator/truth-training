# Keep model classes for Gson
-keep class com.truth.training.client.** { *; }

# Room Database rules
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }

# Compose rules
-keep class androidx.compose.** { *; }
-keep class kotlin.coroutines.** { *; }
-dontwarn androidx.compose.**

# WorkManager rules
-keep class androidx.work.** { *; }
-dontwarn androidx.work.**


