# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Keep Kotlin metadata
-keep class kotlin.Metadata { *; }

# Keep Retrofit
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# Keep Gson
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
-keep class * extends com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# ================================
# SECURITY: ProGuard Rules for FocusMother
# ================================
# These rules preserve classes that use reflection while obfuscating security-sensitive code.

# ================================
# Room Database - MUST keep entities and DAOs
# ================================
-keep class com.focusmother.android.data.entity.** { *; }
-keep class com.focusmother.android.data.dao.** { *; }
-keep @androidx.room.Entity class *
-keepclassmembers class * {
    @androidx.room.* <fields>;
    @androidx.room.* <methods>;
}

# ================================
# Retrofit API Models - MUST keep for JSON serialization
# ================================
-keep class com.focusmother.android.data.api.models.** { *; }

# ================================
# Monitor data classes - Keep for usage stats
# ================================
-keep class com.focusmother.android.monitor.** { *; }

# ================================
# Preferences data classes - Keep for DataStore
# ================================
-keep class com.focusmother.android.data.preferences.** { *; }

# ================================
# SECURITY: Obfuscate security-sensitive classes (name them but don't keep internals)
# ================================
# This makes reverse engineering harder while keeping functionality
-keepnames class com.focusmother.android.util.SecureApiKeyProvider
-keepnames class com.focusmother.android.util.BlocklistEncryption
-keepnames class com.focusmother.android.domain.AdultContentManager

# ================================
# Coroutines
# ================================
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-dontwarn kotlinx.coroutines.**

# ================================
# Jetpack Compose
# ================================
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# ================================
# CameraX
# ================================
-keep class androidx.camera.** { *; }
-dontwarn androidx.camera.**

# ================================
# SceneView (3D rendering)
# ================================
-keep class io.github.sceneview.** { *; }
-dontwarn io.github.sceneview.**

# ================================
# OkHttp
# ================================
-dontwarn okhttp3.**
-dontwarn okio.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# ================================
# Strip debug logging in release builds
# ================================
# SECURITY: Remove all Log.d, Log.v, Log.i calls to reduce information leakage
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}

# ================================
# General optimization rules
# ================================
-optimizationpasses 5
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-verbose

# ================================
# Preserve source file names and line numbers for stack traces
# ================================
# Keep for crash reporting (mapping file will deobfuscate)
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
