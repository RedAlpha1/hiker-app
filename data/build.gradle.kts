plugins {
    alias(libs.plugins.kotlinMultiplatform)
}

kotlin {
    // jvm() only until this builds on a machine with the Android SDK / Xcode
    // — see DECISIONS.md, "KMP module structure and engine harness".
    jvm()

    sourceSets {
        commonMain.dependencies {
            implementation(project(":engine"))
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
