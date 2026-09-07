import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.firebase.crashlytics)
    alias(libs.plugins.google.services)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.baselineprofile)
}

// Hoisted out of defaultConfig so signingConfigs can read it too.
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

android {
    namespace = "com.example.message.recovery"
    compileSdk = 37

    signingConfigs {
        // Release signing material is read from local.properties, never committed. When those
        // keys are absent it falls back to the debug keystore so that `assembleRelease` and — the
        // reason this block exists — baseline profile generation work on a fresh clone. A build
        // signed with the debug key is for local measurement only; Play will reject it.
        create("release") {
            val storePath = localProperties.getProperty("RELEASE_STORE_FILE")
            val configured = !storePath.isNullOrBlank() && rootProject.file(storePath).exists()
            if (configured) {
                storeFile = rootProject.file(storePath!!)
                storePassword = localProperties.getProperty("RELEASE_STORE_PASSWORD")
                keyAlias = localProperties.getProperty("RELEASE_KEY_ALIAS")
                keyPassword = localProperties.getProperty("RELEASE_KEY_PASSWORD")
            } else {
                val debugKeystore = File(System.getProperty("user.home"), ".android/debug.keystore")
                if (debugKeystore.exists()) {
                    storeFile = debugKeystore
                    storePassword = "android"
                    keyAlias = "androiddebugkey"
                    keyPassword = "android"
                }
            }
        }
    }

    defaultConfig {
        applicationId = "com.example.message.recovery"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Set BuildConfig fields
        buildConfigField("String", "API_KEY", "\"${localProperties.getProperty("API_KEY", "")}\"")
        buildConfigField(
            "String",
            "BASE_URL",
            "\"${localProperties.getProperty("BASE_URL", "")}\""
        )

    }

    androidResources {
        localeFilters += listOf("en", "ar", "es", "in", "fa", "hi", "ru", "pt", "bn", "tr")
    }

    bundle {
        language {
            enableSplit = false
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }
}

base {
    archivesName.set("Message-Recovery_vCode_${android.defaultConfig.versionCode}_vName${android.defaultConfig.versionName}")
}

composeCompiler {
    // Off by default so ordinary builds stay fast. Turn on with:
    //   ./gradlew :app:assembleRelease -PcomposeMetrics
    // then read build/compose_reports/*-composables.txt. Any composable on a scrolling or
    // frequently-updated screen that reports `skippable = false` is a jank suspect: it rebuilds
    // even when none of its inputs changed. That report is how the fixes in this commit were
    // verified, and how to check a regression has not crept back in.
    if (project.hasProperty("composeMetrics")) {
        metricsDestination = layout.buildDirectory.dir("compose_metrics")
        reportsDestination = layout.buildDirectory.dir("compose_reports")
    }
}

kotlin {
    jvmToolchain(17)
}

configurations.all {
    exclude(group = "com.google.android.gms", module = "play-services-ads")
    exclude(group = "com.google.android.gms", module = "play-services-ads-lite")
    resolutionStrategy {
        force("org.jetbrains.kotlin:kotlin-metadata-jvm:${libs.versions.kotlin.get()}")
    }
}


dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.sdp.android)
    implementation(libs.ssp.android)

    // Jetpack Compose
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material3.windowsize)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.kotlinx.serialization.core)
    implementation(libs.kotlinx.serialization.json)
    debugImplementation(libs.compose.ui.tooling)

    // DataStore
    implementation(libs.datastore.preferences)

    // Lifecycle + ViewModel
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.work.runtime.ktx)

    implementation(libs.gson)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)

        // Hilt DI
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    ksp(libs.kotlin.metadata.jvm)

    // Shimmer
    implementation(libs.shimmer)

    // Networking

    implementation(libs.okhttp)
    implementation(libs.logging.interceptor)

    // Firebase (BOM)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.config)
    implementation(libs.firebase.messaging)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.billing.ktx)
    implementation(libs.ads)

    // Mediation Adapters
    implementation(libs.mediation.applovin)
    implementation(libs.mediation.facebook)
    implementation(libs.mediation.mintegral)
    implementation(libs.mediation.pangle)
    implementation(libs.mediation.fyber)
    implementation(libs.mediation.ironsource)
    implementation(libs.mediation.unity)
    implementation(libs.mediation.unity3d.ads )
    implementation(libs.mediation.inmobi)
    implementation(libs.mediation.vungle)


}
