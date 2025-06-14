// app/build.gradle.kts

// This block is needed to read your local.properties file
import java.io.FileInputStream
import java.util.Properties

val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        load(FileInputStream(localPropertiesFile))
    }
}

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
    id("kotlin-parcelize")
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.virtualrealm.virtualrealmmusicplayer"
    compileSdk = 35
    ksp {
        arg("ksp.kotlin.1.9.compatibility", "true")
    }

    // Merged buildFeatures block
    buildFeatures {
        compose = true
        buildConfig = true
    }

    defaultConfig {
        applicationId = "com.virtualrealm.virtualrealmmusicplayer"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // API credentials from local.properties
        buildConfigField("String", "SPOTIFY_CLIENT_ID", "\"${localProperties.getProperty("spotify.client.id", "")}\"")
        buildConfigField("String", "SPOTIFY_CLIENT_SECRET", "\"${localProperties.getProperty("spotify.client.secret", "")}\"")
        buildConfigField("String", "SPOTIFY_REDIRECT_URI", "\"${localProperties.getProperty("spotify.redirect.uri", "")}\"")
        buildConfigField("String", "YOUTUBE_API_KEY", "\"${localProperties.getProperty("youtube.api.key", "")}\"")

        manifestPlaceholders["redirectSchemeName"] = "com.virtualrealm.virtualrealmmusicplayer"
        manifestPlaceholders["redirectHostName"] = "callback"
        manifestPlaceholders["appAuthRedirectScheme"] = "com.virtualrealm.virtualrealmmusicplayer"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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

    kotlinOptions {
        jvmTarget = "17"
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }
}

dependencies {
    // Core Android & Kotlin
    implementation(libs.androidx.core.ktx.v1120)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.lifecycle.runtime.ktx.v270)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)

    // Compose
    implementation(libs.androidx.activity.compose.v182)
    implementation(platform(libs.androidx.compose.bom.v20231001))
    implementation(libs.ui)
    implementation(libs.ui.graphics)
    implementation(libs.ui.tooling.preview)
    implementation(libs.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.runtime.livedata)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.lifecycle.service)
    debugImplementation(libs.ui.tooling)

    // Compose Navigation
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.navigation.compose)

    // Animated Bottom Navigation Bar
    implementation("com.exyte:animated-navigation-bar:1.0.0")

    // Accompanist (Compose utilities)
    implementation(libs.accompanist.permissions)
    implementation(libs.accompanist.systemuicontroller)
    implementation(libs.accompanist.webview)

    // Hilt Dependency Injection
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)

    // Room for local database
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Retrofit & OkHttp for API requests
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.okhttp)
    implementation(libs.logging.interceptor)

    // Coroutines for asynchronous programming
    implementation(libs.kotlinx.coroutines.android)

    // Coil for image loading with Compose
    implementation(libs.coil.compose)

    // Spotify SDK
    implementation(libs.auth)

    // YouTube Player API for Compose
    implementation(libs.core)
    implementation(libs.chromecast.sender)

    // DataStore Preferences
    implementation(libs.androidx.datastore.preferences)

    // Other libraries
    implementation(libs.glide)
    implementation(libs.androidx.media)
    implementation(libs.appauth)
    implementation(libs.androidx.browser)

    // For JSON processing
    implementation(libs.converter.gson)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit.v115)
    androidTestImplementation(libs.androidx.espresso.core.v351)
    androidTestImplementation(platform(libs.androidx.compose.bom.v20250500))
    androidTestImplementation(libs.ui.test.junit4)
    debugImplementation(libs.ui.test.manifest)

    // Spotify AAR
    implementation(files("libs/spotify-app-remote-release-0.8.0.aar"))
}
