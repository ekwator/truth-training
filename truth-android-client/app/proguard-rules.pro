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

# Retrofit and OkHttp rules (required for Remote flavor HTTPS)
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepattributes AnnotationDefault
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
-dontwarn javax.annotation.**
-dontwarn kotlin.Unit
-dontwarn retrofit2.KotlinExtensions
-dontwarn retrofit2.KotlinExtensions$*
-dontwarn retrofit2.-KotlinExtensions
-keep class retrofit2.** { *; }
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**

# Gson rules (required for JSON serialization)
-keepattributes Signature
-keepattributes *Annotation*
-keep class sun.misc.Unsafe { *; }
-keep class com.google.gson.** { *; }
-keep class com.google.gson.examples.android.model.** { *; }
-keepclassmembers,allowobfuscation class * {
  @com.google.gson.annotations.SerializedName <fields>;
}
-keepclassmembers class * implements com.google.gson.TypeAdapter {
    public <init>(com.google.gson.Gson);
}
-keepclassmembers class * implements com.google.gson.TypeAdapterFactory {
    public <init>(com.google.gson.Gson);
}
-keepclassmembers class * implements com.google.gson.JsonSerializer {
    public <methods>;
}
-keepclassmembers class * implements com.google.gson.JsonDeserializer {
    public <methods>;
}

# HTTPS/SSL rules (required for Remote flavor)
-keep class javax.net.ssl.** { *; }
-keep class java.security.** { *; }
-keep class javax.crypto.** { *; }
-dontwarn javax.net.ssl.**
-dontwarn java.security.**
-dontwarn javax.crypto.**

# BouncyCastle rules (for Ed25519 crypto)
-keep class org.bouncycastle.** { *; }
-dontwarn org.bouncycastle.**


