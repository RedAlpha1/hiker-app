plugins {
    alias(libs.plugins.kotlinMultiplatform)
    // AGP needs network access to resolve (via google()) even with `apply
    // false` -- see DECISIONS.md, "Camera preview and map view actuals" for
    // why that broke this sandbox's build and why this is gated the way it
    // is. The plugins{} DSL only supports literal/System.getenv-style
    // conditions here, not a script-level val (that fails to resolve) --
    // this exact expression is duplicated below because of that.
    if (System.getenv("ANDROID_HOME") != null || System.getenv("ANDROID_SDK_ROOT") != null || File("local.properties").exists()) {
        id("com.android.library") version "8.7.3" // keep in sync with gradle/libs.versions.toml's "agp" entry
    }
}

// Recomputed here (rootDir-relative, more robust than the plugins{} block's
// cwd-relative check) because that block can't see script-level vals.
val hasAndroidSdk = System.getenv("ANDROID_HOME") != null ||
    System.getenv("ANDROID_SDK_ROOT") != null ||
    File(rootDir, "local.properties").exists()

if (hasAndroidSdk) {
    // A separate file, not inline: AGP's LibraryExtension type can't be
    // referenced anywhere in *this* script when the plugin above didn't
    // apply (unresolved reference at script-compile time, not just a
    // runtime no-op). apply(from=) only compiles its target when actually
    // invoked, so the reference only exists in a script that's otherwise
    // never read.
    apply(from = "android.gradle.kts")
}

kotlin {
    // Pure Kotlin, no platform imports (see CLAUDE.md) — fully testable on
    // the JVM without any platform SDK.
    jvm()

    // iOS targets compile only on a macOS host; Kotlin disables them (with a
    // warning) rather than failing the build elsewhere.
    listOf(iosX64(), iosArm64(), iosSimulatorArm64()).forEach { target ->
        target.binaries.framework {
            baseName = "Engine"
            isStatic = true
        }
    }

    sourceSets {
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
