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
            xOffsetFraction = (-0.25f + Math.random().toFloat() * 0.5f),
            yOffsetFraction = (-0.25f + Math.random().toFloat() * 0.5f)
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

        // Apply position offset after layout
        container.post {
            val maxXOffset = container.width * style.xOffsetFraction
            val maxYOffset = container.height * style.yOffsetFraction
            textView.translationX = maxXOffset
            textView.translationY = maxYOffset
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

        // Set initial state
        view.alpha = 0f
        when (style.animationKind) {
            AnimationKind.SLIDE_TOP -> view.translationY -= 400f
            AnimationKind.SLIDE_BOTTOM -> view.translationY += 400f
            AnimationKind.SLIDE_LEFT -> view.translationX -= 600f
            AnimationKind.SLIDE_RIGHT -> view.translationX += 600f
            AnimationKind.SCALE_UP -> {
                view.scaleX = 0.3f
                view.scaleY = 0.3f
            }
            AnimationKind.BLUR_FADE, AnimationKind.FADE -> { /* just opacity */ }
        }

        // Entrance animation
        val enterAnims = mutableListOf<Animator>()
        enterAnims.add(ObjectAnimator.ofFloat(view, "alpha", 0f, 1f))

        when (style.animationKind) {
            AnimationKind.SLIDE_TOP, AnimationKind.SLIDE_BOTTOM -> {
                val target = view.translationY + if (style.animationKind == AnimationKind.SLIDE_TOP) 400f else -400f
                enterAnims.add(ObjectAnimator.ofFloat(view, "translationY", view.translationY, target))
            }
            AnimationKind.SLIDE_LEFT, AnimationKind.SLIDE_RIGHT -> {
                val target = view.translationX + if (style.animationKind == AnimationKind.SLIDE_LEFT) 600f else -600f
                enterAnims.add(ObjectAnimator.ofFloat(view, "translationX", view.translationX, target))
            }
            AnimationKind.SCALE_UP -> {
                enterAnims.add(ObjectAnimator.ofFloat(view, "scaleX", 0.3f, 1f))
                enterAnims.add(ObjectAnimator.ofFloat(view, "scaleY", 0.3f, 1f))
            }
            else -> {}
        }

        val enterSet = AnimatorSet().apply {
            playTogether(enterAnims)
            duration = 600
            interpolator = if (style.animationKind == AnimationKind.SCALE_UP)
                OvershootInterpolator(1.2f)
            else
                DecelerateInterpolator()
        }

        // Scale breathing during hold
        if (style.scaleBreathing) {
            enterSet.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    val breatheX = ObjectAnimator.ofFloat(view, "scaleX", 1f, style.targetScale).apply {
                        repeatCount = ObjectAnimator.INFINITE
                        repeatMode = ObjectAnimator.REVERSE
                        duration = 1500
                        interpolator = AccelerateDecelerateInterpolator()
                    }
                    val breatheY = ObjectAnimator.ofFloat(view, "scaleY", 1f, style.targetScale).apply {
                        repeatCount = ObjectAnimator.INFINITE
                        repeatMode = ObjectAnimator.REVERSE
                        duration = 1500
                        interpolator = AccelerateDecelerateInterpolator()
                    }
                    AnimatorSet().apply {
                        playTogether(breatheX, breatheY)
                        start()
                    }
                }
            })
        }

        enterSet.start()

        // Schedule exit
        val holdMs = (Math.max(0f, displayDuration - 2f) * 1000).toLong()
        handler.postDelayed({
            val exitAnims = mutableListOf<Animator>()
            exitAnims.add(ObjectAnimator.ofFloat(view, "alpha", 1f, 0f))

            if (style.animationKind == AnimationKind.SCALE_UP) {
                exitAnims.add(ObjectAnimator.ofFloat(view, "scaleX", view.scaleX, 1.5f))
                exitAnims.add(ObjectAnimator.ofFloat(view, "scaleY", view.scaleY, 1.5f))
            }

            AnimatorSet().apply {
                playTogether(exitAnims)
                duration = 1500
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        onDismiss()
                    }
                })
                start()
            }
        }, 600 + holdMs)
    }
}
