plugins {
    alias(libs.plugins.kotlinMultiplatform)
}

kotlin {
    // jvm() until this builds on a machine with the Android SDK — see
    // DECISIONS.md, "KMP module structure and engine harness".
    jvm()

    // Disabled (with a warning) on non-macOS hosts, same as :engine.
    listOf(iosX64(), iosArm64(), iosSimulatorArm64()).forEach { target ->
        target.binaries.framework {
            baseName = "Data"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":engine"))
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
