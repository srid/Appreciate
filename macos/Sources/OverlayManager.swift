import Cocoa
import SwiftUI

/// Manages creating and displaying transparent overlay windows with reminder text.
final class OverlayManager {
    private var overlayWindows: [NSWindow] = []

    func showOverlay(text: String, displayDuration: Double) {
        // Cancel any existing overlays
        dismiss()

        let screens = NSScreen.screens
        guard !screens.isEmpty else { return }

        // Shared random style seed so all screens show the same style
        let style = OverlayStyle.random()

        for screen in screens {
            let screenFrame = screen.frame

            // Random position offset per screen
            let xOffset = CGFloat.random(in: -screenFrame.width * 0.25 ... screenFrame.width * 0.25)
            let yOffset = CGFloat.random(in: -screenFrame.height * 0.25 ... screenFrame.height * 0.25)

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

            let animatingView = AnimatingOverlayView(
                text: text,
                displayDuration: displayDuration,
                xOffset: xOffset,
                yOffset: yOffset,
                style: style
            ) { [weak self] in
                self?.dismiss()
            }
            window.contentView = NSHostingView(rootView: animatingView)
            window.orderFrontRegardless()
            overlayWindows.append(window)
        }
    }

    func dismiss() {
        for window in overlayWindows {
            window.orderOut(nil)
        }
        overlayWindows.removeAll()
    }
}

// MARK: - Animation Style

// SYNC: Animation kinds must match android/.../OverlayViewFactory.kt AnimationKind
enum AnimationKind: CaseIterable {
    case fade
    case slideFromTop
    case slideFromBottom
    case slideFromLeft
    case slideFromRight
    case scaleUp
    case blurIn
}

// MARK: - Overlay Style (randomized once per display, shared across screens)

struct OverlayStyle {
    let fontSize: CGFloat
    let fontWeight: Font.Weight
    let fontDesign: Font.Design
    let textColor: Color
    let shadowOpacity: Double
    let rotation: Double
    let animationKind: AnimationKind
    let showBackgroundPill: Bool
    let pillColor: Color
    let pillOpacity: Double
    let scaleEffect: Bool
    let targetScale: CGFloat

    static func random() -> OverlayStyle {
        let showPill = Bool.random()
        let textColor = vibrantColors.randomElement()!
        // Pill color is a desaturated version or contrasting dark/light
        let pillColor = Bool.random()
            ? Color.black.opacity(0.4)
            : Color.white.opacity(0.15)

        return OverlayStyle(
            fontSize: CGFloat.random(in: 36...72),
            fontWeight: fontWeights.randomElement()!,
            fontDesign: fontDesigns.randomElement()!,
            textColor: textColor,
            shadowOpacity: Double.random(in: 0.4...0.7),
            rotation: Double.random(in: -3...3),
            animationKind: AnimationKind.allCases.randomElement()!,
            showBackgroundPill: showPill,
            pillColor: pillColor,
            pillOpacity: Double.random(in: 0.5...0.85),
            scaleEffect: Bool.random(),
            targetScale: CGFloat.random(in: 1.0...1.08)
        )
    }

    // SYNC: Color palette must match android/.../OverlayViewFactory.kt vibrantColors
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
}

// MARK: - Animating Overlay View

private struct AnimatingOverlayView: View {
    let text: String
    let displayDuration: Double
    let xOffset: CGFloat
    let yOffset: CGFloat
    let style: OverlayStyle
    let onDismiss: () -> Void

    @State private var opacity: Double = 0.0
    @State private var slideOffset: CGSize = .zero
    @State private var currentScale: CGFloat = 1.0
    @State private var blur: CGFloat = 0.0

    init(text: String, displayDuration: Double, xOffset: CGFloat, yOffset: CGFloat, style: OverlayStyle, onDismiss: @escaping () -> Void) {
        self.text = text
        self.displayDuration = displayDuration
        self.xOffset = xOffset
        self.yOffset = yOffset
        self.style = style
        self.onDismiss = onDismiss
    }

    var body: some View {
        textContent
            .fixedSize()
            .padding(.horizontal, 40)
            .padding(.vertical, 24)
            .background(pillBackground)
            .rotationEffect(.degrees(style.rotation))
            .scaleEffect(currentScale)
            .blur(radius: blur)
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .offset(x: xOffset + slideOffset.width, y: yOffset + slideOffset.height)
            .opacity(opacity)
            .onAppear { animateIn() }
    }

    private var textContent: some View {
        Text(text)
            .font(.system(size: style.fontSize, weight: style.fontWeight, design: style.fontDesign))
            .foregroundColor(style.textColor)
            .shadow(color: .black.opacity(style.shadowOpacity), radius: 8, x: 2, y: 2)
            .shadow(color: .black.opacity(0.3), radius: 16, x: 0, y: 0)
            .multilineTextAlignment(.center)
    }

    @ViewBuilder
    private var pillBackground: some View {
        if style.showBackgroundPill {
            RoundedRectangle(cornerRadius: 20, style: .continuous)
                .fill(style.pillColor.opacity(style.pillOpacity))
                .blur(radius: 0.5)
        }
    }

    // MARK: - Animations

    private func animateIn() {
        // Set initial state based on animation kind
        switch style.animationKind {
        case .fade:
            break // just opacity
        case .slideFromTop:
            slideOffset = CGSize(width: 0, height: -200)
        case .slideFromBottom:
            slideOffset = CGSize(width: 0, height: 200)
        case .slideFromLeft:
            slideOffset = CGSize(width: -400, height: 0)
        case .slideFromRight:
            slideOffset = CGSize(width: 400, height: 0)
        case .scaleUp:
            currentScale = 0.3
        case .blurIn:
            blur = 20
        }

        // Scale breathing effect
        if style.scaleEffect {
            currentScale = style.animationKind == .scaleUp ? 0.3 : 0.95
        }

        // Animate in
        withAnimation(.spring(response: 0.6, dampingFraction: 0.8)) {
            opacity = 1.0
            slideOffset = .zero
            blur = 0
            if style.animationKind == .scaleUp {
                currentScale = style.scaleEffect ? style.targetScale : 1.0
            } else if style.scaleEffect {
                currentScale = style.targetScale
            }
        }

        // Subtle scale breathing during hold
        if style.scaleEffect && style.animationKind != .scaleUp {
            let breatheDuration = max(1.0, displayDuration - 2.0)
            withAnimation(.easeInOut(duration: breatheDuration).repeatForever(autoreverses: true)) {
                currentScale = style.targetScale
            }
        }

        // Schedule fade out
        let holdDuration = max(0, displayDuration - 2.0)
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.6 + holdDuration) {
            animateOut()
        }
    }

    private func animateOut() {
        withAnimation(.easeOut(duration: 1.5)) {
            opacity = 0.0
            if style.animationKind == .scaleUp {
                currentScale = 1.5
            }
            if style.animationKind == .blurIn {
                blur = 20
            }
        }
        DispatchQueue.main.asyncAfter(deadline: .now() + 1.5) {
            onDismiss()
        }
    }
}
