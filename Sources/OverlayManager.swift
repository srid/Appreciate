import Cocoa
import SwiftUI

/// Manages creating and displaying transparent overlay windows with reminder text.
final class OverlayManager {
    private var overlayWindow: NSWindow?
    private var fadeOutWorkItem: DispatchWorkItem?
    private var dismissWorkItem: DispatchWorkItem?

    func showOverlay(text: String, displayDuration: Double) {
        // Cancel any existing overlay
        dismiss()

        guard let screen = NSScreen.main else { return }
        let screenFrame = screen.frame

        // Create a full-screen transparent window
        let window = NSWindow(
            contentRect: screenFrame,
            styleMask: .borderless,
            backing: .buffered,
            defer: false
        )
        window.isOpaque = false
        window.backgroundColor = .clear
        window.level = .screenSaver
        window.ignoresMouseEvents = true
        window.collectionBehavior = [.canJoinAllSpaces, .fullScreenAuxiliary, .stationary]
        window.hasShadow = false

        // Random position offset within screen (text is centered by default, offset randomly)
        let xOffset = CGFloat.random(in: -screenFrame.width * 0.25 ... screenFrame.width * 0.25)
        let yOffset = CGFloat.random(in: -screenFrame.height * 0.25 ... screenFrame.height * 0.25)

        let animatingView = AnimatingOverlayView(
            text: text,
            displayDuration: displayDuration,
            xOffset: xOffset,
            yOffset: yOffset
        ) { [weak self] in
            self?.dismiss()
        }
        window.contentView = NSHostingView(rootView: animatingView)

        window.orderFrontRegardless()
        self.overlayWindow = window
    }


    func dismiss() {
        fadeOutWorkItem?.cancel()
        dismissWorkItem?.cancel()
        overlayWindow?.orderOut(nil)
        overlayWindow = nil
    }
}

/// Wrapper view that handles fade-in, hold, and fade-out animation lifecycle.
private struct AnimatingOverlayView: View {
    let text: String
    let displayDuration: Double
    let xOffset: CGFloat
    let yOffset: CGFloat
    let onDismiss: () -> Void

    @State private var opacity: Double = 0.0

    // Randomized style
    private let fontSize: CGFloat
    private let fontWeight: Font.Weight
    private let fontDesign: Font.Design
    private let textColor: Color
    private let shadowOpacity: Double
    private let rotation: Double

    private static let vibrantColors: [Color] = [
        Color(hue: 0.05, saturation: 0.85, brightness: 0.95),
        Color(hue: 0.08, saturation: 0.90, brightness: 1.0),
        Color(hue: 0.12, saturation: 0.80, brightness: 1.0),
        Color(hue: 0.95, saturation: 0.70, brightness: 1.0),
        Color(hue: 0.55, saturation: 0.75, brightness: 0.95),
        Color(hue: 0.50, saturation: 0.60, brightness: 1.0),
        Color(hue: 0.45, saturation: 0.70, brightness: 0.90),
        Color(hue: 0.75, saturation: 0.60, brightness: 0.95),
        Color(hue: 0.30, saturation: 0.80, brightness: 0.90),
        Color(hue: 0.85, saturation: 0.65, brightness: 1.0),
        Color(hue: 0.65, saturation: 0.70, brightness: 1.0),
        Color(hue: 0.15, saturation: 0.90, brightness: 1.0),
        Color(hue: 0.35, saturation: 0.95, brightness: 1.0),
        Color(hue: 0.80, saturation: 0.80, brightness: 1.0),
        Color(hue: 0.00, saturation: 0.80, brightness: 1.0),
        Color(hue: 0.60, saturation: 0.35, brightness: 1.0),
        Color(hue: 0.85, saturation: 0.30, brightness: 1.0),
        Color(hue: 0.40, saturation: 0.35, brightness: 0.95),
    ]

    private static let fontWeights: [Font.Weight] = [
        .light, .regular, .medium, .semibold, .bold, .heavy
    ]

    private static let fontDesigns: [Font.Design] = [
        .default, .rounded, .serif, .monospaced
    ]

    init(text: String, displayDuration: Double, xOffset: CGFloat, yOffset: CGFloat, onDismiss: @escaping () -> Void) {
        self.text = text
        self.displayDuration = displayDuration
        self.xOffset = xOffset
        self.yOffset = yOffset
        self.onDismiss = onDismiss
        self.fontSize = CGFloat.random(in: 36...72)
        self.fontWeight = Self.fontWeights.randomElement()!
        self.fontDesign = Self.fontDesigns.randomElement()!
        self.textColor = Self.vibrantColors.randomElement()!
        self.shadowOpacity = Double.random(in: 0.4...0.7)
        self.rotation = Double.random(in: -3...3)
    }

    var body: some View {
        Text(text)
            .font(.system(size: fontSize, weight: fontWeight, design: fontDesign))
            .foregroundColor(textColor)
            .shadow(color: .black.opacity(shadowOpacity), radius: 8, x: 2, y: 2)
            .shadow(color: .black.opacity(0.3), radius: 16, x: 0, y: 0)
            .rotationEffect(.degrees(rotation))
            .multilineTextAlignment(.center)
            .fixedSize()
            .padding(40)
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .offset(x: xOffset, y: yOffset)
            .opacity(opacity)
            .onAppear {
                // Fade in
                withAnimation(.easeIn(duration: 0.5)) {
                    opacity = 1.0
                }
                // Schedule fade out
                let holdDuration = max(0, displayDuration - 2.0)
                DispatchQueue.main.asyncAfter(deadline: .now() + 0.5 + holdDuration) {
                    withAnimation(.easeOut(duration: 1.5)) {
                        opacity = 0.0
                    }
                    // Dismiss after fade out completes
                    DispatchQueue.main.asyncAfter(deadline: .now() + 1.5) {
                        onDismiss()
                    }
                }
            }
    }
}
