# ═══════════════════════════════════════════════════════════════
#  DexUltra Booster Pro — ProGuard / R8 rules
# ═══════════════════════════════════════════════════════════════

# ─── App classes ────────────────────────────────────────────────
-keep class com.dex.ultra.booster.pro.** { *; }

# ─── Shizuku — keep ALL + reflection target ─────────────────────
-keep class rikka.shizuku.** { *; }
-keep class moe.shizuku.** { *; }
-keepclassmembers class rikka.shizuku.** { *; }
# Reflection: Shizuku.newProcess used by ShizukuHelper
-keepclassmembers class rikka.shizuku.Shizuku {
    public static java.lang.Process newProcess(java.lang.String[], java.lang.String[], java.lang.String);
}

# ─── libsu ──────────────────────────────────────────────────────
-keep class com.topjohnwu.superuser.** { *; }
-keepclassmembers class com.topjohnwu.superuser.** { *; }

# ─── Kotlin coroutines ──────────────────────────────────────────
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** { volatile <fields>; }
-dontwarn kotlinx.coroutines.**

# ─── AndroidX / Material ────────────────────────────────────────
-keep class androidx.** { *; }
-keep class com.google.android.material.** { *; }
-dontwarn androidx.**

# ─── Data classes ───────────────────────────────────────────────
-keep class com.dex.ultra.booster.pro.SensPreset { *; }
-keep class com.dex.ultra.booster.pro.DnsServer { *; }

# ─── VPN Service ────────────────────────────────────────────────
-keep class com.dex.ultra.booster.pro.DnsVpnService { *; }
-keep class android.net.VpnService { *; }

# ─── Lottie ─────────────────────────────────────────────────────
-keep class com.airbnb.lottie.** { *; }
-dontwarn com.airbnb.lottie.**

# ─── Attributes ─────────────────────────────────────────────────
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ─── Serialization ──────────────────────────────────────────────
-keepclassmembers class * implements java.io.Serializable {
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# ─── Strip verbose logging in release ───────────────────────────
-assumenosideeffects class android.util.Log {
    public static *** v(...);
    public static *** d(...);
}

-dontwarn sun.misc.**
-dontwarn java.lang.invoke.**
