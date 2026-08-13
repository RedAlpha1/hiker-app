plugins {
    alias(libs.plugins.kotlinMultiplatform)
    // Safe to add unconditionally-gated: unlike Compose Multiplatform's
    // common runtime (see the jvm() comment below), plain Kotlin has no
    // transitive dependency on Google's Maven repo, so androidTarget() alone
    // doesn't reintroduce the resolution failure that kept CMP out of this
    // module. Gated the same way as engine/build.gradle.kts anyway, purely
    // because this environment still has no SDK to build the android.gradle.kts
    // half against -- see DECISIONS.md, "KMP module structure and engine
    // harness" for why script-level vals can't be used inside plugins{}.
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
    // jvm() is what this module builds and tests against in this sandbox.
    // androidTarget() is added above, conditionally, once an SDK is
    // findable -- see DECISIONS.md, ":ui gets androidTarget()". Compose
    // Multiplatform is still not applied here -- see DECISIONS.md, "Camera
    // preview and map view actuals": its common runtime has a hard
    // transitive dependency on androidx artifacts that only exist on
    // Google's Maven repo, which this sandbox's network policy blocks for
    // every target, not just Android. Kept this module Compose-free and
    // fully verifiable rather than risk breaking its already-tested state
    // classes -- androidTarget() alone doesn't have that problem, only the
    // CMP plugin/runtime does.
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
