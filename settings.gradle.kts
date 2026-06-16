import java.util.Properties

pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }

        // Firebase Maven repository
        maven { url = uri("https://maven.google.com") }

        maven {
            url = uri("https://maven.pkg.github.com/umerrjaved1/AdsManager")
            credentials {
                val localProps = Properties()
                val localPropsFile = settingsDir.resolve("local.properties")
                if (localPropsFile.exists()) {
                    localProps.load(localPropsFile.inputStream())
                }
                username = localProps.getProperty("gpr.user") ?: providers.gradleProperty("gpr.user").orNull
                password = localProps.getProperty("gpr.key") ?: providers.gradleProperty("gpr.key").orNull
            }
            content {
                includeGroup("com.umer_tf.ads")
            }
        }
    }
}

rootProject.name = "Docuview"
include(":app")
