import SwiftUI

/// A curated palette of vibrant colors for the reminder text.
private let vibrantColors: [Color] = [
    // Warm
    Color(hue: 0.05, saturation: 0.85, brightness: 0.95),  // coral
    Color(hue: 0.08, saturation: 0.90, brightness: 1.0),   // tangerine
    Color(hue: 0.12, saturation: 0.80, brightness: 1.0),   // amber
    Color(hue: 0.95, saturation: 0.70, brightness: 1.0),   // rose
    // Cool
    Color(hue: 0.55, saturation: 0.75, brightness: 0.95),  // ocean blue
    Color(hue: 0.50, saturation: 0.60, brightness: 1.0),   // sky
    Color(hue: 0.45, saturation: 0.70, brightness: 0.90),  // teal
    Color(hue: 0.75, saturation: 0.60, brightness: 0.95),  // lavender
    // Vivid
    Color(hue: 0.30, saturation: 0.80, brightness: 0.90),  // emerald
    Color(hue: 0.85, saturation: 0.65, brightness: 1.0),   // magenta
    Color(hue: 0.65, saturation: 0.70, brightness: 1.0),   // periwinkle
    Color(hue: 0.15, saturation: 0.90, brightness: 1.0),   // golden
    // Neon
    Color(hue: 0.35, saturation: 0.95, brightness: 1.0),   // neon green
    Color(hue: 0.80, saturation: 0.80, brightness: 1.0),   // electric purple
    Color(hue: 0.00, saturation: 0.80, brightness: 1.0),   // neon red-pink
    // Pastels
    Color(hue: 0.60, saturation: 0.35, brightness: 1.0),   // pastel blue
    Color(hue: 0.85, saturation: 0.30, brightness: 1.0),   // pastel pink
    Color(hue: 0.40, saturation: 0.35, brightness: 0.95),  // pastel mint
]

private let fontWeights: [Font.Weight] = [
    .light, .regular, .medium, .semibold, .bold, .heavy
]

private let fontDesigns: [Font.Design] = [
    .default, .rounded, .serif, .monospaced
]

struct OverlayContentView: View {
    let text: String
    @State private var opacity: Double = 0

    // Randomized style (computed once at init)
    private let fontSize: CGFloat
    private let fontWeight: Font.Weight
    private let fontDesign: Font.Design
    private let textColor: Color
    private let shadowColor: Color
    private let rotation: Double

    init(text: String) {
        self.text = text
        self.fontSize = CGFloat.random(in: 36...72)
        self.fontWeight = fontWeights.randomElement()!
        self.fontDesign = fontDesigns.randomElement()!
        self.textColor = vibrantColors.randomElement()!
        self.shadowColor = Color.black.opacity(Double.random(in: 0.4...0.7))
        self.rotation = Double.random(in: -3...3)
    }

    var body: some View {
        Text(text)
            .font(.system(size: fontSize, weight: fontWeight, design: fontDesign))
            .foregroundColor(textColor)
            .shadow(color: shadowColor, radius: 8, x: 2, y: 2)
            .shadow(color: .black.opacity(0.3), radius: 16, x: 0, y: 0)
            .rotationEffect(.degrees(rotation))
            .multilineTextAlignment(.center)
            .padding(40)
            .opacity(opacity)
    }

    func fadeIn() {
        withAnimation(.easeIn(duration: 0.5)) {
            opacity = 1.0
        }
    }

    func fadeOut(duration: Double) {
        withAnimation(.easeOut(duration: duration)) {
            opacity = 0.0
        }
    }
}
