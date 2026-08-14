import SwiftUI
import UI // Kotlin/Native framework built from :ui

/// `UI.Colors`' tokens are ARGB `Int64` (0xAARRGGBB, see ui/theme/Colors.kt's
/// own doc comment: "Convert at the call site once composables land"). This
/// is that call site for SwiftUI, mirroring :android's `toComposeColor()`.
extension Color {
    init(argb: Int64) {
        let a = Double((argb >> 24) & 0xFF) / 255.0
        let r = Double((argb >> 16) & 0xFF) / 255.0
        let g = Double((argb >> 8) & 0xFF) / 255.0
        let b = Double(argb & 0xFF) / 255.0
        self.init(.sRGB, red: r, green: g, blue: b, opacity: a)
    }
}
