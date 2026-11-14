plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    // Keep the Kotlin namespace stable for the existing Activity package.
    namespace = "com.smart222tech.pasha18tv"
    compileSdk = 35

    defaultConfig {
        // New installation identity and launcher branding: PASHXXX.
        applicationId = "com.smart222tech.pashxxx"
        minSdk = 23
        targetSdk = 35
        versionCode = System.getenv("PASHXXX_VERSION_CODE")?.toIntOrNull() ?: 15
        versionName = System.getenv("PASHXXX_VERSION_NAME") ?: "0.5.7"
    }

    signingConfigs {
        create("release") {
            val storePath = System.getenv("PASHXXX_KEYSTORE_PATH")
            if (!storePath.isNullOrBlank()) {
                storeFile = file(storePath)
                storePassword = System.getenv("PASHXXX_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("PASHXXX_KEY_ALIAS")
                keyPassword = System.getenv("PASHXXX_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        getByName("release") {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = false
        }
    }

    buildFeatures { compose = true }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2025.02.00"))
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.media3:media3-exoplayer:1.5.1")
    implementation("androidx.media3:media3-ui:1.5.1")
    implementation("io.coil-kt:coil-compose:2.7.0")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
