// This module is only ever included (see the root settings.gradle.kts) when
// an Android SDK is present, so unlike every other build.gradle.kts in this
// repo, nothing here needs the conditional-plugin gymnastics documented in
// engine/build.gradle.kts -- by the time Gradle evaluates this file at all,
// real network + SDK access is assumed. Unverified in this sandbox (no SDK
// to build against) -- see DECISIONS.md, "Camera preview and map view
// actuals".
plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinAndroid)
}

android {
    namespace = "com.ridgeline.android"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.ridgeline.android"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1"
    }

    buildFeatures {
        compose = true
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
    // :engine and :data are Kotlin Multiplatform libraries with an
    // androidTarget() (conditionally added -- see their own build.gradle.kts);
    // this resolves to their Android variant automatically.
    implementation(project(":engine"))
    implementation(project(":data"))
    // Screen state (OnboardingState, ...) and design tokens (Colors,
    // Typography) -- see DECISIONS.md, ":ui gets androidTarget()".
    implementation(project(":ui"))

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)

    implementation(libs.camerax.core)
    implementation(libs.camerax.camera2)
    implementation(libs.camerax.lifecycle)
    implementation(libs.camerax.view)

    implementation(libs.maplibre.android)
}
