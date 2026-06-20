@file:Suppress("UnstableApiUsage")

import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.firebase.crashlytics)
    alias(libs.plugins.google.services)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

configurations.all {
    resolutionStrategy.eachDependency {
        if (requested.group == "com.google.firebase" && requested.name == "firebase-crashlytics-ktx") {
            useVersion(libs.versions.firebaseCrashlyticsKtx.get())
        }
    }
}

android {
    namespace = "com.mzalogics.docuview"
    compileSdk = 37

    signingConfigs {
        create("release") {
            storeFile = file("../keystore/mzalogics_keystore.jks")
            storePassword = "123456"
            keyAlias = "key0"
            keyPassword = "123456"
        }
    }
    defaultConfig {
        applicationId = "com.tf.phonecleaner.booster"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        val localProperties = Properties()
        val localPropertiesFile = rootProject.file("local.properties")

        if (localPropertiesFile.exists()) {
            localProperties.load(localPropertiesFile.inputStream())
        }

        // Set BuildConfig fields
        buildConfigField("String", "API_KEY", "\"${localProperties.getProperty("API_KEY")}\"")
        buildConfigField(
            "String",
            "BASE_URL",
            "\"${localProperties.getProperty("BASE_URL", "https://your-default-api.com/")}\""
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
        viewBinding = true
        buildConfig = true
    }
}

base {
    archivesName.set("Document_Viewer_vCode_${android.defaultConfig.versionCode}_vName${android.defaultConfig.versionName}")
}

kotlin {
    jvmToolchain(17)
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.room.ktx)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)

    // Lifecycle + ViewModel
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.work.runtime.ktx)

    implementation(libs.gson)
    implementation(libs.glide)
    implementation(libs.docviewer)
    implementation(libs.pdf.viewer)

    // Navigation
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.navigation.ui.ktx)
    // Hilt DI
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    // Shimmer
    implementation(libs.shimmer)


    // Networking

    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.okhttp)
    implementation(libs.logging.interceptor)
// For URL manipulation
    implementation(libs.okhttp.urlconnection)


    // Firebase (BOM)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.config)
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
    implementation(libs.mediation.inmobi)
    implementation(libs.mediation.vungle)

    // Facebook SDK for AppEvents
    implementation(libs.facebook.android.sdk)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.work.testing)
}