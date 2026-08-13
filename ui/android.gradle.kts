// Only ever read via `apply(from = "android.gradle.kts")` when an Android
// SDK is present -- see build.gradle.kts and engine/android.gradle.kts.
// Unverified in this sandbox (no SDK to compile against); standard AGP + KMP
// boilerplate otherwise. No Android-specific dependencies needed -- this
// module is plain Kotlin (screen state, design tokens), not Compose.
import com.android.build.gradle.LibraryExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

configure<KotlinMultiplatformExtension> {
    androidTarget()
}

configure<LibraryExtension> {
    namespace = "com.ridgeline.ui"
    compileSdk = 35
    defaultConfig {
        minSdk = 26
    }
}
