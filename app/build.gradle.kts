@file:Suppress("UnstableApiUsage")

import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.firebase.crashlytics)
    alias(libs.plugins.google.services)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.professor.baseproject"
    compileSdk = 37

    signingConfigs {
        create("release") {
            storeFile = file("../keystore/AppKeyStore.jks")
            storePassword = "123456"
            keyAlias = "key0"
            keyPassword = "123456"
        }
    }
    defaultConfig {
        applicationId = "com.professor.baseproject"
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
        // NOTE: every build type keeps the same applicationId (com.professor.baseproject).
        //
        // Do NOT add applicationIdSuffix here unless you also add a matching Android app
        // to the Firebase project. app/google-services.json declares exactly one client,
        // for com.professor.baseproject, and the Google Services plugin hard-fails the
        // build with "No matching client found for package name ..." on any other id.
        //
        // Consequence: debug and release overwrite each other on a device. If you want
        // them side by side, register com.professor.baseproject.debug in Firebase, re-
        // download google-services.json, then set applicationIdSuffix = ".debug".
        debug {
            versionNameSuffix = "-debug"
            isMinifyEnabled = false
        }

        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }

        /**
         * R8-processed like release, but debug-signed so it can actually be installed and
         * run locally. The release variant is minified AND unsigned, so the shrunk code
         * path was effectively never executed before upload — which is exactly where the
         * package-name-hardcoded ProGuard keeps fail silently.
         *
         * Use: ./gradlew :app:assembleMinifiedDebug
         */
        create("minifiedDebug") {
            initWith(getByName("release"))
            versionNameSuffix = "-minified"
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += listOf("release")
        }
    }

    lint {
        // Fail the build on real problems, and check the libraries too. None of this was
        // configured before, so the unused-resource and unused-permission findings that
        // lint reports natively were never surfaced.
        warningsAsErrors = false
        abortOnError = true
        checkDependencies = true
        // Regenerate with: ./gradlew :app:updateLintBaseline
        baseline = file("lint-baseline.xml")
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }

    // Room's exported schema JSON must be on the instrumentation test classpath for
    // MigrationTestHelper to find it. Without this, a migration test can compile and
    // then fail at runtime with "Cannot find the schema file in the assets folder".
    sourceSets {
        getByName("androidTest") {
            assets.srcDirs("$projectDir/schemas")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
        compose = true
    }
}

base {
    // Was the literal placeholder "AppName_", which no fork ever substituted — every
    // build shipped artefacts called AppName_vCode_1_vName1.0.1. Derived from the
    // applicationId so it is correct automatically after a rename.
    val appLabel = (android.defaultConfig.applicationId ?: "app").substringAfterLast('.')
    archivesName.set(
        "${appLabel}_vCode${android.defaultConfig.versionCode}_vName${android.defaultConfig.versionName}"
    )
}

kotlin {
    jvmToolchain(17)
}

// Room schema export — required by @Database(exportSchema = true). The generated
// JSON under app/schemas should be committed; it is what makes migration tests possible.
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    // Room — room-ktx alone does NOT generate AppDatabase_Impl; the KSP compiler is required.
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)

    // Jetpack Compose
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.activity.compose)
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
    implementation(libs.glide)



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
    implementation(libs.play.app.update.ktx)
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

    // ---------------------------------------------------------------------------
    // Testing
    // ---------------------------------------------------------------------------
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.androidx.room.testing)
    androidTestImplementation(libs.kotlinx.coroutines.test) // runTest in AppDatabaseTest



}