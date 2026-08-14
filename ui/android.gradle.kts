// Only ever read via `apply(from = "android.gradle.kts")` when an Android
// SDK is present -- see build.gradle.kts and engine/android.gradle.kts.
// :ui stays Compose-free even with this target added (see DECISIONS.md,
// "Camera preview and map view actuals") -- this only makes its existing
// plain-Kotlin screen state (RecordingState, Colors, Typography, etc.)
// consumable from :android, which was the point of adding it.
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
