// Only ever read via `apply(from = "android.gradle.kts")` when an Android
// SDK is present -- see build.gradle.kts and engine/android.gradle.kts.
import com.android.build.gradle.LibraryExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

configure<KotlinMultiplatformExtension> {
    androidTarget()
}

configure<LibraryExtension> {
    namespace = "com.ridgeline.data"
    compileSdk = 35
    defaultConfig {
        minSdk = 26
    }
}
