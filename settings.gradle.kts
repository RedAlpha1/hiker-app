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

// :android and :ios are scaffolded under their own directories (see the
// README in each) but not wired in here yet. `:android` needs the Android
// Gradle plugin, which requires ANDROID_HOME; `:ios` needs Kotlin/Native's
// iOS targets, which only compile on a macOS host. Neither is available in
// this environment. Uncomment on a machine with the relevant SDK/Xcode
// installed — see DECISIONS.md, "KMP module structure and engine harness".
// include(":android")
// include(":ios")
