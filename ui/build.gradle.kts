plugins {
    alias(libs.plugins.kotlinMultiplatform)
}

kotlin {
    // jvm() until this builds on a machine with the Android SDK — see
    // DECISIONS.md, "KMP module structure and engine harness". Compose
    // Multiplatform is not applied here -- see DECISIONS.md, "Camera preview
    // and map view actuals": its common runtime has a hard transitive
    // dependency on androidx artifacts that only exist on Google's Maven
    // repo, which this sandbox's network policy blocks for every target,
    // not just Android. Kept this module Compose-free and fully verifiable
    // rather than risk breaking its already-tested state classes.
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
