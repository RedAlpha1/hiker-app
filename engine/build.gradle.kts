plugins {
    alias(libs.plugins.kotlinMultiplatform)
}

kotlin {
    // Pure Kotlin, no platform imports (see CLAUDE.md) — fully testable on
    // the JVM without any platform SDK. androidTarget()/iosArm64() etc. get
    // added once this builds on a machine with those toolchains; see
    // DECISIONS.md, "KMP module structure and engine harness".
    jvm()

    sourceSets {
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
