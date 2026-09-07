# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# ----------------------------------------------------------------------------
# Strip verbose/debug/info logging from release
# ----------------------------------------------------------------------------
# This codebase logs heavily on hot paths (splash navigation, every ad callback, every activity
# lifecycle event), and each call builds its interpolated String *before* Log decides to drop it.
# R8 removes the calls and the string building with them. Warnings and errors are kept so
# Crashlytics breadcrumbs survive.
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static boolean isLoggable(...);
}

# ----------------------------------------------------------------------------
# Android & Kotlin
# ----------------------------------------------------------------------------
-dontwarn kotlin.**
-dontwarn kotlinx.coroutines.**
-keep class kotlinx.coroutines.** { *; }
-keep class kotlin.Metadata { *; }

# ----------------------------------------------------------------------------
# Hilt / Dagger
# ----------------------------------------------------------------------------
-keep class com.example.message.recovery.di.** { *; }
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class javax.annotation.** { *; }
-keep @dagger.hilt.EntryPoint class *
-keep @dagger.hilt.InstallIn class *
-keep @dagger.hilt.DefineComponent class *
-keep @dagger.Module class *
-keep @dagger.Provides class *
-keep @javax.inject.Inject class *
-keep @javax.inject.Singleton class *


# ----------------------------------------------------------------------------
# Gson
# ----------------------------------------------------------------------------
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes InnerClasses,EnclosingMethod,AnnotationDefault

-keep class com.google.gson.stream.** { *; }
-keep class com.google.gson.examples.android.model.** { *; }
-keep class com.google.gson.** { *; }

# TypeToken generic signatures (required with R8 full mode, used in Extensions.kt)
-keep,allowobfuscation,allowshrinking class com.google.gson.reflect.TypeToken
-keep,allowobfuscation,allowshrinking class * extends com.google.gson.reflect.TypeToken

# Enums serialized/deserialized by Gson
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ----------------------------------------------------------------------------
# Models (Keep all data classes to prevent serialization issues)
# ----------------------------------------------------------------------------
-keep class com.example.message.recovery.model.** { *; }
-keep class com.inc.vidwave.videodownloader.data.source.api.** { *; }
-keep class com.inc.vidwave.videodownloader.data.model.** { *; }
# Remote Config JSON models (parsed via Gson in RemoteConfigManager)
-keep class com.example.message.recovery.remoteconfig.data.** { *; }
# Weather API response models (Timestamp feature)
-keep class com.example.message.recovery.ui.timeStamp.model.** { *; }
# Gallery models (Serializable, passed via Intents)
-keep class com.example.message.recovery.ui.timeStamp.gallery.model.** { *; }

# Parcelable
-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

# Serializable
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# ----------------------------------------------------------------------------
# Downloader API DTOs / Ktor
# ----------------------------------------------------------------------------
-keep class com.inc.vidwave.videodownloader.data.downloader.dto.** { *; }
-keep class com.inc.vidwave.videodownloader.domain.downloader.model.** { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-dontwarn io.ktor.**

# ----------------------------------------------------------------------------
# Retrofit / OkHttp (R8 full mode safety)
# ----------------------------------------------------------------------------
# Keep annotated API interface methods and their generic signatures
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-if interface * { @retrofit2.http.* <methods>; }
-keep,allowobfuscation interface <1>
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation
-dontwarn retrofit2.**
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement

# ----------------------------------------------------------------------------
# ViewBinding / DataBinding
# ----------------------------------------------------------------------------

-keep class com.inc.vidwave.videodownloader.databinding.** { *; }

# ----------------------------------------------------------------------------
# AdMob / Google Play Services
# ----------------------------------------------------------------------------
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**
-keep class com.google.ads.** { *; }

# ----------------------------------------------------------------------------
# Internal ads library (com.umer_tf.ads — ships NO consumer rules)
# ----------------------------------------------------------------------------
-keep class com.umer_tf.ads.** { *; }
-dontwarn com.umer_tf.ads.**

# ----------------------------------------------------------------------------
# Mediation adapters / partner ad SDKs
# ----------------------------------------------------------------------------
# AppLovin
-keep class com.applovin.** { *; }
-dontwarn com.applovin.**
# Meta (Facebook Audience Network + Facebook SDK)
-keep class com.facebook.** { *; }
-dontwarn com.facebook.**
# Mintegral
-keep class com.mbridge.** { *; }
-dontwarn com.mbridge.**
# Pangle
-keep class com.bytedance.sdk.** { *; }
-dontwarn com.bytedance.sdk.**
# Fyber / DT Exchange
-keep class com.fyber.** { *; }
-dontwarn com.fyber.**
# ironSource
-keep class com.ironsource.** { *; }
-dontwarn com.ironsource.**
# Unity Ads
-keep class com.unity3d.ads.** { *; }
-keep class com.unity3d.services.** { *; }
-dontwarn com.unity3d.**
# InMobi
-keep class com.inmobi.** { *; }
-dontwarn com.inmobi.**
# Vungle / Liftoff
-keep class com.vungle.** { *; }
-dontwarn com.vungle.**

# ----------------------------------------------------------------------------
# Google Play Billing
# ----------------------------------------------------------------------------
-keep class com.android.billingclient.** { *; }
-dontwarn com.android.billingclient.**

# ----------------------------------------------------------------------------
# YouTube Player (pierfrancescosoffritti — WebView + JS bridge)
# ----------------------------------------------------------------------------
-keep class com.pierfrancescosoffritti.androidyoutubeplayer.** { *; }
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# ----------------------------------------------------------------------------
# PanoramaGL (OpenGL, reflection-based)
# ----------------------------------------------------------------------------
-keep class com.panoramagl.** { *; }
-dontwarn com.panoramagl.**

# ----------------------------------------------------------------------------
# DocViewer / Pdf-Viewer
# ----------------------------------------------------------------------------
-keep class com.cherry.lib.doc.** { *; }
-dontwarn com.cherry.lib.doc.**
-keep class com.rajat.pdfviewer.** { *; }
-dontwarn com.rajat.pdfviewer.**

# ----------------------------------------------------------------------------
# Apache POI (Fix for missing java.awt classes)
# ----------------------------------------------------------------------------
-dontwarn java.awt.**
-dontwarn org.apache.poi.**
-keep class org.apache.poi.** { *; }
-dontwarn javax.xml.stream.**

# ----------------------------------------------------------------------------
# Navigation Compose type-safe routes
# ----------------------------------------------------------------------------
-keep class com.example.message.recovery.ui.navigation.** { *; }

# ----------------------------------------------------------------------------
# Firebase & FCM
# ----------------------------------------------------------------------------
-keep class com.google.firebase.** { *; }
-keep class com.example.message.recovery.fcm.** { *; }
-keep class com.example.message.recovery.notification.** { *; }