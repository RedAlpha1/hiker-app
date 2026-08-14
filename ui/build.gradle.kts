plugins {
    alias(libs.plugins.kotlinMultiplatform)
    // See engine/build.gradle.kts for why this is gated this exact way.
    if (System.getenv("ANDROID_HOME") != null || System.getenv("ANDROID_SDK_ROOT") != null || File("local.properties").exists()) {
        id("com.android.library") version "8.7.3" // keep in sync with gradle/libs.versions.toml's "agp" entry
    }
}

val hasAndroidSdk = System.getenv("ANDROID_HOME") != null ||
    System.getenv("ANDROID_SDK_ROOT") != null ||
    File(rootDir, "local.properties").exists()

if (hasAndroidSdk) {
    apply(from = "android.gradle.kts")
}

kotlin {
    // jvm() -- Android's target is added conditionally just above, mirroring
    // :engine/:data (see DECISIONS.md, "KMP module structure and engine
    // harness"). Compose Multiplatform is still not applied here -- see
    // DECISIONS.md, "Camera preview and map view actuals": its common
    // runtime has a hard transitive dependency on androidx artifacts that
    // only exist on Google's Maven repo, which this sandbox's network
    // policy blocks for every target, not just Android. Kept this module
    // Compose-free and fully verifiable rather than risk breaking its
    // already-tested state classes -- androidTarget() alone carries none of
    // that risk, it's the same plain Kotlin compiled for one more platform.
    jvm()

    listOf(iosX64(), iosArm64(), iosSimulatorArm64()).forEach { target ->
        target.binaries.framework {
            baseName = "UI"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":engine"))
            implementation(project(":data"))
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
