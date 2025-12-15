plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "it.antonio.glucosebridge"
    compileSdk = 35

    defaultConfig {
        applicationId = "it.antonio.glucosebridge"
        minSdk = 28
        targetSdk = 34

        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.activity:activity-ktx:1.9.0")

    // Health Connect
    implementation("androidx.health.connect:connect-client:1.1.0-alpha10")

    // HTTP client
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
}
