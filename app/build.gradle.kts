plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.10"
}

android {
    namespace = "com.example.fitnessquest"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.fitnessquest"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        vectorDrawables {
            useSupportLibrary = true
        }
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

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx.v1131)
    implementation(libs.androidx.lifecycle.runtime.ktx.v284)
    implementation(libs.androidx.activity.compose.v190)
    implementation(libs.androidx.navigation.compose)

    // ✅ Use Compose BOM (latest June 2024)
    implementation(platform(libs.androidx.compose.bom.v20240600))

    // Compose UI + Material3
    implementation(libs.ui)
    implementation(libs.ui.tooling.preview)
    implementation(libs.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.animation)
    implementation(libs.androidx.compose.material3.material3)

    // Debug tooling
    debugImplementation(libs.ui.tooling)
    debugImplementation(libs.ui.test.manifest)

    // ViewModel + Lifecycle
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // DataStore (for saving XP/quests)
    implementation(libs.androidx.datastore.preferences)

    // Kotlin serialization
    implementation(libs.jetbrains.kotlinx.serialization.json)

    // (Optional) Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit.v121)
    androidTestImplementation(libs.androidx.espresso.core.v361)
    androidTestImplementation(platform(libs.androidx.compose.bom.v20240600))
    androidTestImplementation(libs.ui.test.junit4)
}

