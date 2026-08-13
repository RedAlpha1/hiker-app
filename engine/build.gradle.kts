plugins {
    alias(libs.plugins.kotlinMultiplatform)
}

kotlin {
    // Pure Kotlin, no platform imports (see CLAUDE.md) — fully testable on
    // the JVM without any platform SDK.
    jvm()

    // iOS targets compile only on a macOS host; Kotlin disables them (with a
    // warning) rather than failing the build elsewhere. androidTarget() isn't
    // added yet — it needs the Android SDK present just to configure, which
    // isn't a "skip on unsupported host" situation like iOS. See
    // DECISIONS.md, "KMP module structure and engine harness".
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
