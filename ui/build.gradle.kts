plugins {
    alias(libs.plugins.kotlinMultiplatform)
}

kotlin {
    // jvm() only until this builds on a machine with the Android SDK / Xcode
    // — see DECISIONS.md, "KMP module structure and engine harness". Compose
    // Multiplatform is not applied yet; add it alongside androidTarget()/
    // iosArm64() when those toolchains are available.
    jvm()

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
