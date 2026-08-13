// Only ever read via `apply(from = "android.gradle.kts")` when an Android
// SDK is present -- see build.gradle.kts. Unverified in this sandbox (no
// SDK to compile against); standard AGP + KMP boilerplate otherwise.
import com.android.build.gradle.LibraryExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

configure<KotlinMultiplatformExtension> {
    androidTarget()
}

configure<LibraryExtension> {
    namespace = "com.ridgeline.engine"
    compileSdk = 35
    defaultConfig {
        minSdk = 26
    }
}
