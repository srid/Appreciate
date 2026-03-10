package ca.srid.appreciate

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.TextView

/**
 * Creates the overlay view with randomized styling, matching the macOS app's anti-habituation design.
 */
class OverlayViewFactory(private val context: Context) {

    // SYNC: Color palette must match macos/Sources/OverlayManager.swift vibrantColors
    private val vibrantColors = listOf(
        floatArrayOf(0.05f, 0.85f, 0.95f),
        floatArrayOf(0.08f, 0.90f, 1.0f),
        floatArrayOf(0.12f, 0.80f, 1.0f),
        floatArrayOf(0.95f, 0.70f, 1.0f),
        floatArrayOf(0.55f, 0.75f, 0.95f),
        floatArrayOf(0.50f, 0.60f, 1.0f),
        floatArrayOf(0.45f, 0.70f, 0.90f),
        floatArrayOf(0.75f, 0.60f, 0.95f),
        floatArrayOf(0.30f, 0.80f, 0.90f),
        floatArrayOf(0.85f, 0.65f, 1.0f),
        floatArrayOf(0.65f, 0.70f, 1.0f),
        floatArrayOf(0.15f, 0.90f, 1.0f),
        floatArrayOf(0.35f, 0.95f, 1.0f),
        floatArrayOf(0.80f, 0.80f, 1.0f),
        floatArrayOf(0.00f, 0.80f, 1.0f),
        floatArrayOf(0.60f, 0.35f, 1.0f),
        floatArrayOf(0.85f, 0.30f, 1.0f),
        floatArrayOf(0.40f, 0.35f, 0.95f),
    )

    private val fontFamilies = listOf(
        Typeface.DEFAULT,
        Typeface.SERIF,
        Typeface.SANS_SERIF,
        Typeface.MONOSPACE
    )

    // SYNC: Animation kinds must match macos/Sources/OverlayManager.swift AnimationKind
    enum class AnimationKind {
        FADE, SLIDE_TOP, SLIDE_BOTTOM, SLIDE_LEFT, SLIDE_RIGHT, SCALE_UP, BLUR_FADE
    }

    data class OverlayStyle(
        val fontSize: Float,
        val textColor: Int,
        val typeface: Typeface,
        val isBold: Boolean,
        val rotation: Float,
        val animationKind: AnimationKind,
        val showPill: Boolean,
        val pillColor: Int,
        val scaleBreathing: Boolean,
        val targetScale: Float,
        val xOffsetFraction: Float,
        val yOffsetFraction: Float
    )

    private fun randomStyle(): OverlayStyle {
        val hsb = vibrantColors.random()
        val textColor = Color.HSVToColor(floatArrayOf(hsb[0] * 360f, hsb[1], hsb[2]))

        val showPill = Math.random() < 0.5
        val pillColor = if (Math.random() < 0.5)
            Color.argb(100, 0, 0, 0)
        else
            Color.argb(40, 255, 255, 255)

        return OverlayStyle(
            fontSize = (36f + Math.random().toFloat() * 36f),
            textColor = textColor,
            typeface = fontFamilies.random(),
            isBold = Math.random() < 0.5,
            rotation = (-3f + Math.random().toFloat() * 6f),
            animationKind = AnimationKind.entries.toTypedArray().random(),
            showPill = showPill,
            pillColor = pillColor,
            scaleBreathing = Math.random() < 0.5,
            targetScale = (1.0f + Math.random().toFloat() * 0.08f),
            xOffsetFraction = (-0.15f + Math.random().toFloat() * 0.3f),
            yOffsetFraction = (-0.15f + Math.random().toFloat() * 0.3f)
        )
    }

    /**
     * Creates a full-screen overlay FrameLayout with the reminder text, randomized styling,
     * and entrance animation. Calls [onDismiss] when the animation lifecycle completes.
     */
    fun createOverlayView(
        text: String,
        displayDuration: Float,
        onDismiss: () -> Unit
    ): View {
        val style = randomStyle()

        val container = FrameLayout(context).apply {
            setBackgroundColor(Color.TRANSPARENT)
        }

        val textView = TextView(context).apply {
            this.text = text
            setTextSize(TypedValue.COMPLEX_UNIT_SP, style.fontSize)
            setTextColor(style.textColor)
            typeface = if (style.isBold)
                Typeface.create(style.typeface, Typeface.BOLD)
            else
                style.typeface
            this.rotation = style.rotation
            gravity = Gravity.CENTER
            setShadowLayer(16f, 2f, 2f, Color.argb(150, 0, 0, 0))

            if (style.showPill) {
                val bg = GradientDrawable().apply {
                    cornerRadius = 40f
                    setColor(style.pillColor)
                }
                background = bg
                val hPad = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 32f, resources.displayMetrics).toInt()
                val vPad = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16f, resources.displayMetrics).toInt()
                setPadding(hPad, vPad, hPad, vPad)
            }
        }

        val params = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
            Gravity.CENTER
        )

        container.addView(textView, params)

        // Apply position offset after layout, using actual container dimensions
        // (handles dynamic screen sizes, e.g. foldable phones)
        container.post {
            // Constrain text width to 80% of container to allow wrapping
            val maxTextWidth = (container.width * 0.8f).toInt()
            textView.maxWidth = maxTextWidth

            val maxXOffset = container.width * style.xOffsetFraction
            val maxYOffset = container.height * style.yOffsetFraction

            // Clamp translation so the text stays within visible bounds
            val halfW = textView.width / 2f
            val halfH = textView.height / 2f
            val maxClampX = (container.width / 2f - halfW).coerceAtLeast(0f)
            val maxClampY = (container.height / 2f - halfH).coerceAtLeast(0f)
            textView.translationX = maxXOffset.coerceIn(-maxClampX, maxClampX)
            textView.translationY = maxYOffset.coerceIn(-maxClampY, maxClampY)
        }

        // Animate
        animateIn(textView, style, displayDuration, onDismiss)

        return container
    }

    private fun animateIn(
        view: TextView,
        style: OverlayStyle,
        displayDuration: Float,
        onDismiss: () -> Unit
    ) {
        val handler = Handler(Looper.getMainLooper())

        Log.d("OverlayViewFactory", "animateIn: kind=${style.animationKind} duration=${displayDuration}s")

        // IMPORTANT: Leave alpha at 1f initially so that the window is registered
        // with Android's compositor (SurfaceFlinger) as fully visible. This prevents
        // the overlay from being culled when the user is constantly touching the
        // screen (e.g. scrolling), which starves the animator and would otherwise
        // cause the overlay to flash and vanish instantly.
        //
        // We defer setting alpha = 0f and starting the entrance animation to the
        // next frame via post(), after the window is already composited.
        view.post {
            view.alpha = 0f
            val initialTransX = view.translationX
            val initialTransY = view.translationY
            when (style.animationKind) {
                AnimationKind.SLIDE_TOP -> view.translationY = initialTransY - 400f
                AnimationKind.SLIDE_BOTTOM -> view.translationY = initialTransY + 400f
                AnimationKind.SLIDE_LEFT -> view.translationX = initialTransX - 600f
                AnimationKind.SLIDE_RIGHT -> view.translationX = initialTransX + 600f
                AnimationKind.SCALE_UP -> {
                    view.scaleX = 0.3f
                    view.scaleY = 0.3f
                }
                AnimationKind.BLUR_FADE, AnimationKind.FADE -> { /* just opacity */ }
            }

            // Entrance animation using ViewPropertyAnimator (ignores animator_duration_scale)
            val enter = view.animate()
                .alpha(1f)
                .setDuration(600)

            when (style.animationKind) {
                AnimationKind.SLIDE_TOP, AnimationKind.SLIDE_BOTTOM -> enter.translationY(initialTransY)
                AnimationKind.SLIDE_LEFT, AnimationKind.SLIDE_RIGHT -> enter.translationX(initialTransX)
                AnimationKind.SCALE_UP -> enter.scaleX(1f).scaleY(1f)
                else -> {}
            }

            enter.start()

            // Schedule exit using Handler (not affected by animation scale)
            val holdMs = (displayDuration * 1000).toLong()
            val exitDelayMs = 600 + holdMs
            Log.d("OverlayViewFactory", "Exit scheduled in ${exitDelayMs}ms (entrance=600 + hold=${holdMs})")

            handler.postDelayed({
                Log.d("OverlayViewFactory", "EXIT animation starting now")

                val exit = view.animate()
                    .alpha(0f)
                    .setDuration(1500)

                if (style.animationKind == AnimationKind.SCALE_UP) {
                    exit.scaleX(1.5f).scaleY(1.5f)
                }

                exit.withEndAction {
                    Log.d("OverlayViewFactory", "EXIT complete — calling onDismiss")
                    onDismiss()
                }

                exit.start()
            }, exitDelayMs)

            // Scale breathing using ViewPropertyAnimator loop
            if (style.scaleBreathing) {
                fun breatheLoop() {
                    view.animate()
                        .scaleX(style.targetScale)
                        .scaleY(style.targetScale)
                        .setDuration(1500)
                        .withEndAction {
                            view.animate()
                                .scaleX(1f)
                                .scaleY(1f)
                                .setDuration(1500)
                                .withEndAction { breatheLoop() }
                                .start()
                        }
                        .start()
                }
                handler.postDelayed({ breatheLoop() }, 600)
            }
        } // end view.post — everything above runs after the compositor registers the window
    }
}
