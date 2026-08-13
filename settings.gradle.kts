rootProject.name = "ridgeline"

pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

include(":engine")
include(":data")
include(":ui")

// The Android Gradle plugin needs an SDK just to *configure* -- unlike
// Kotlin/Native's iOS targets, which disable themselves gracefully on a
// non-macOS host, AGP fails the build outright without ANDROID_HOME. So
// :android (and androidTarget() inside :data/:ui, guarded the same way in
// their own build.gradle.kts) is included only when an SDK is actually
// findable. In Android Studio this activates automatically -- opening the
// project writes local.properties with sdk.dir, no manual edit needed. See
// DECISIONS.md, "Camera preview and map view actuals".
val hasAndroidSdk = System.getenv("ANDROID_HOME") != null ||
    System.getenv("ANDROID_SDK_ROOT") != null ||
    file("local.properties").exists()

if (hasAndroidSdk) {
    include(":android")
}

// :ios has no Gradle module of its own -- the iosMain actuals live inside
// :engine/:data/:ui's own iOS targets (always declared; Kotlin/Native skips
// them, with a warning, on a non-macOS host). The Xcode project under
// ios/ consumes those modules' frameworks directly. See ios/README.md.
