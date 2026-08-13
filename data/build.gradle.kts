plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.sqldelight)
    // See engine/build.gradle.kts for why this is gated this exact way.
    if (System.getenv("ANDROID_HOME") != null || System.getenv("ANDROID_SDK_ROOT") != null || File("local.properties").exists()) {
        id("com.android.library") version "8.7.3" // keep in sync with gradle/libs.versions.toml's "agp" entry
    }
}

val hasAndroidSdk = System.getenv("ANDROID_HOME") != null ||
    System.getenv("ANDROID_SDK_ROOT") != null ||
    File(rootDir, "local.properties").exists()

if (hasAndroidSdk) {
    apply(from = "android.gradle.kts")
}

kotlin {
    // expect/actual classes (DatabaseDriverFactory) are still Beta as of
    // Kotlin 2.1 -- this is the documented way to opt in and drop the warning.
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    // jvm() doubles as the only target this repo can actually run repository
    // tests against right now (in-memory SQLite via the JDBC driver).
    jvm()

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
        if (hasAndroidSdk) {
            val androidMain by getting {
                dependencies {
                    implementation(libs.sqldelight.android.driver)
                }
            }
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
