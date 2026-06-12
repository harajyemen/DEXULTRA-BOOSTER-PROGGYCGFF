# DexUltra Booster Pro – ProGuard Rules

# Keep app classes
-keep class com.dex.ultra.booster.pro.** { *; }

# Shizuku
-keep class rikka.shizuku.** { *; }
-keep class moe.shizuku.** { *; }

# libsu
-keep class com.topjohnwu.superuser.** { *; }

# Kotlin coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# AndroidX
-keep class androidx.** { *; }
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable

# Material
-keep class com.google.android.material.** { *; }

# VPN Service
-keep class com.dex.ultra.booster.pro.DnsVpnService { *; }

# Remove logging in release
-assumenosideeffects class android.util.Log {
    public static *** v(...);
    public static *** d(...);
    public static *** i(...);
}
