plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.sqldelight)
}

kotlin {
    // expect/actual classes (DatabaseDriverFactory) are still Beta as of
    // Kotlin 2.1 -- this is the documented way to opt in and drop the warning.
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    // jvm() until this builds on a machine with the Android SDK — see
    // DECISIONS.md, "KMP module structure and engine harness". The jvm target
    // doubles as the only target this repo can actually run repository tests
    // against right now (in-memory SQLite via the JDBC driver).
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
            implementation(libs.sqldelight.runtime)
            implementation(libs.kotlinx.datetime)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
        jvmMain.dependencies {
            implementation(libs.sqldelight.sqlite.driver)
        }
        iosMain.dependencies {
            implementation(libs.sqldelight.native.driver)
        }
    }
}

sqldelight {
    databases {
        create("RidgelineDatabase") {
            packageName.set("com.ridgeline.data.db")
        }
    }
}
