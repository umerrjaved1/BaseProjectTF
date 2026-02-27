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
# Android & Kotlin
# ----------------------------------------------------------------------------
-dontwarn kotlin.**
-dontwarn kotlinx.coroutines.**
-keep class kotlinx.coroutines.** { *; }
-keep class kotlin.Metadata { *; }

# ----------------------------------------------------------------------------
# Hilt / Dagger
# ----------------------------------------------------------------------------
-keep class com.professor.baseproject.di.** { *; }
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
# Retrofit
# ----------------------------------------------------------------------------
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepattributes Signature
-keepattributes Exceptions
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# ----------------------------------------------------------------------------
# OkHttp
# ----------------------------------------------------------------------------
-dontwarn okhttp3.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# ----------------------------------------------------------------------------
# Gson
# ----------------------------------------------------------------------------
-keepattributes Signature
-keepattributes *Annotation*

-keep class com.google.gson.stream.** { *; }
-keep class com.google.gson.examples.android.model.** { *; }
-keep class com.google.gson.** { *; }

# ----------------------------------------------------------------------------
# Models (Keep all data classes to prevent serialization issues)
# ----------------------------------------------------------------------------
-keep class com.professor.baseproject.model.** { *; }
-keep class com.professor.baseproject.data.source.api.** { *; }
-keep class com.professor.baseproject.data.model.** { *; }

# ----------------------------------------------------------------------------
# ViewBinding / DataBinding
# ----------------------------------------------------------------------------

-keep class com.professor.baseproject.databinding.** { *; }

# ----------------------------------------------------------------------------
# AdMob / Google Play Services
# ----------------------------------------------------------------------------
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**
-keep class com.google.ads.** { *; }

# ----------------------------------------------------------------------------
# Apache POI (Fix for missing java.awt classes)
# ----------------------------------------------------------------------------
-dontwarn java.awt.**
-dontwarn org.apache.poi.**
-keep class org.apache.poi.** { *; }
-dontwarn javax.xml.stream.**

# ----------------------------------------------------------------------------
# Firebase
# ----------------------------------------------------------------------------
-keep class com.google.firebase.** { *; }