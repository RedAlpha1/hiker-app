plugins {
    alias(libs.plugins.kotlinMultiplatform)
}

kotlin {
    // jvm() until this builds on a machine with the Android SDK — see
    // DECISIONS.md, "KMP module structure and engine harness". Compose
    // Multiplatform is not applied yet — there are no screens to justify it;
    // add it alongside androidTarget() once real UI code lands here.
    jvm()

    // Disabled (with a warning) on non-macOS hosts, same as :engine.
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
